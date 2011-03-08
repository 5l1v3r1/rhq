package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.ANCESTRY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.AVAILABILITY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.DESCRIPTION;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A DataSource basically the same as ResourceDatasource in the fields it defines, but that works with
 * ResourceComposite as opposed to Resource records.  In this way the Records can provide additional info,
 * like the user's resource permissions, for the resources. 
 *  
 * @author Jay Shaughnessy
 */
public class ResourceCompositeDataSource extends RPCDataSource<ResourceComposite> {
    private static ResourceCompositeDataSource INSTANCE;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public static ResourceCompositeDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResourceCompositeDataSource();
        }
        return INSTANCE;
    }

    public ResourceCompositeDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    // Defined the same way as ResourceDatasource 
    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        return ResourceDatasource.addResourceDatasourceFields(fields);
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        ResourceCriteria criteria = getFetchCriteria(request);

        getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_loadFailed(), caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<ResourceComposite> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    protected void dataRetrieved(final PageList<ResourceComposite> result, final DSResponse response,
        final DSRequest request) {
        HashSet<Integer> typesSet = new HashSet<Integer>();
        ArrayList<Resource> resources = new ArrayList<Resource>(result.size());
        for (ResourceComposite resourceComposite : result) {
            Resource resource = resourceComposite.getResource();
            resources.add(resource);
            ResourceType type = resource.getResourceType();
            if (type != null) {
                typesSet.add(type.getId());
            }
        }

        // In addition to the types of the result resources, get the types of their ancestry
        // NOTE: this may be too labor intensive in general, but since this is a singleton I coudln't
        //       make it easily optional.
        typesSet.addAll(AncestryUtil.getResourceTypeIds(resources));

        ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
        typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
            @Override
            public void onTypesLoaded(Map<Integer, ResourceType> types) {

                Record[] records = buildRecords(result);
                for (Record record : records) {
                    // replace type id with type name
                    Integer typeId = record.getAttributeAsInt(TYPE.propertyName());
                    ResourceType type = types.get(typeId);
                    if (type != null) {
                        record.setAttribute(TYPE.propertyName(), type.getName());
                    }

                    // decode ancestry
                    String ancestry = record.getAttributeAsString(ANCESTRY.propertyName());
                    if (null == ancestry) {
                        continue;
                    }
                    int resourceId = record.getAttributeAsInt("id");
                    String[] decodedAncestry = AncestryUtil.decodeAncestry(resourceId, ancestry, types);
                    // Preserve the encoded ancestry for special-case formatting at higher levels. Set the
                    // decoded strings as different attributes. 
                    record.setAttribute(ResourceDatasource.ATTR_ANCESTRY_RESOURCES, decodedAncestry[0]);
                    record.setAttribute(ResourceDatasource.ATTR_ANCESTRY_TYPES, decodedAncestry[1]);
                }
                response.setData(records);
                response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                processResponse(request.getRequestId(), response);
            }
        });
    }

    protected ResourceCriteria getFetchCriteria(final DSRequest request) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.setPageControl(getPageControl(request));

        criteria.addFilterId(getFilter(request, "id", Integer.class));
        criteria.addFilterParentResourceId(getFilter(request, "parentId", Integer.class));
        criteria.addFilterCurrentAvailability(getFilter(request, AVAILABILITY.propertyName(), AvailabilityType.class));
        criteria.addFilterResourceCategories(getArrayFilter(request, CATEGORY.propertyName(), ResourceCategory.class));
        criteria.addFilterIds(getArrayFilter(request, "resourceIds", Integer.class));
        criteria.addFilterImplicitGroupIds(getFilter(request, "groupId", Integer.class));
        criteria.addFilterName(getFilter(request, NAME.propertyName(), String.class));
        criteria.addFilterResourceTypeId(getFilter(request, TYPE.propertyName(), Integer.class));
        criteria.addFilterPluginName(getFilter(request, PLUGIN.propertyName(), String.class));
        criteria.addFilterTagNamespace(getFilter(request, "tagNamespace", String.class));
        criteria.addFilterTagSemantic(getFilter(request, "tagSemantic", String.class));
        criteria.addFilterTagName(getFilter(request, "tagName", String.class));

        return criteria;
    }

    @Override
    public ResourceComposite copyValues(Record from) {
        // not very strong...
        return new ResourceComposite(new Resource(from.getAttributeAsInt("id")), AvailabilityType.DOWN);
    }

    @Override
    public ListGridRecord copyValues(ResourceComposite from) {
        Resource res = from.getResource();
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("resourceComposite", from);
        record.setAttribute("resource", res);
        record.setAttribute("id", res.getId());
        record.setAttribute(NAME.propertyName(), res.getName());
        record.setAttribute(ANCESTRY.propertyName(), res.getAncestry());
        record.setAttribute(DESCRIPTION.propertyName(), res.getDescription());
        record.setAttribute(TYPE.propertyName(), res.getResourceType().getId());
        record.setAttribute(PLUGIN.propertyName(), res.getResourceType().getPlugin());
        record.setAttribute(CATEGORY.propertyName(), res.getResourceType().getCategory().name());
        record.setAttribute("icon", ImageManager.getResourceIcon(res.getResourceType().getCategory(), res
            .getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP));
        record.setAttribute(AVAILABILITY.propertyName(), ImageManager.getAvailabilityIconFromAvailType(res
            .getCurrentAvailability().getAvailabilityType()));

        record.setAttribute("resourcePersmission", from.getResourcePermission());
        return record;
    }

    public ResourceGWTServiceAsync getResourceService() {
        return resourceService;
    }
}
