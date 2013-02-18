package org.rhq.enterprise.server.resource.metadata;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;

@Stateless
public class AlertMetadataManagerBean implements AlertMetadataManagerLocal {

    private static final Log log = LogFactory.getLog(AlertMetadataManagerBean.class);

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionMgr;

    @EJB
    private AlertTemplateManagerLocal alertTemplateMgr;

    @Override
    public void deleteAlertTemplates(Subject subject, ResourceType resourceType) {
        log.debug("Deleting alert templates for " + resourceType);

        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addFilterAlertTemplateResourceTypeId(resourceType.getId());
        criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

        List<AlertDefinition> templates = alertDefinitionMgr.findAlertDefinitionsByCriteria(subject, criteria);

        Integer[] templateIds = new Integer[templates.size()];
        int i = 0;
        for (AlertDefinition template : templates) {
            templateIds[i++] = template.getId();
        }

        // Alert definitions associated with individual resources and with groups
        // are deleted as part of resource deletion. This commit adds support for
        // templates which are alert definitions associated with the resource
        // type
        alertTemplateMgr.removeAlertTemplates(subject, templateIds);
        alertDefinitionMgr.purgeUnusedAlertDefinitions();
    }
}
