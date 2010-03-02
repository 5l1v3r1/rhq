package org.rhq.enterprise.gui.coregui.client;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourcesView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceView;
import org.rhq.enterprise.gui.coregui.client.menu.MenuBarView;
import org.rhq.enterprise.gui.coregui.client.util.ErrorHandler;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.smartgwt.client.core.KeyIdentifier;
import com.smartgwt.client.util.KeyCallback;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author Greg Hinkle
 */
public class CoreGUI implements EntryPoint {

    public static final String CONTENT_CANVAS_ID = "BaseContent";

    private static Subject sessionSubject;
    //private static Subject fullSubject;

    private static ErrorHandler errorHandler = new ErrorHandler();

    private static BreadcrumbTrailPane breadCrumb;

    private static Canvas content;

    private View rootView;

    public void onModuleLoad() {

        if (!GWT.isScript()) {
            KeyIdentifier debugKey = new KeyIdentifier();
            debugKey.setCtrlKey(true);
            debugKey.setKeyName("D");
            Page.registerKey(debugKey, new KeyCallback() {
                public void execute(String keyName) {
                    SC.showConsole();
                }
            });
        }

        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable e) {
                SC.say("Globally uncaught exception... " + e.getMessage());
                e.printStackTrace();
            }
        });

        RequestBuilder b = new RequestBuilder(RequestBuilder.GET, "/j_security_check.do?j_username=rhqadmin&j_password=rhqadmin");
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(Request request, Response response) {
                    System.out.println("Portal-War logged in");
                }

                public void onError(Request request, Throwable exception) {
                    System.out.println("Portal-War login failed");
                }
            });
            b.send();
        } catch (RequestException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        SubjectGWTServiceAsync subjectService = SubjectGWTServiceAsync.Util.getInstance();

        subjectService.login("rhqadmin", "rhqadmin", new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                System.out.println("Failed to login - cause: " + caught);
                Label loginFailed = new Label("Failed to login - cause: " + caught);
                loginFailed.draw();
            }

            public void onSuccess(Subject result) {
                System.out.println("Logged in: " + result.getSessionId());
                setSessionSubject(result);

                buildCoreUI();

                /* We can cache all metadata right here
                ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                        (Integer[]) null, EnumSet.allOf(ResourceTypeRepository.MetadataType.class), new ResourceTypeRepository.TypesLoadedCallback() {
                    public void onTypesLoaded(HashMap<Integer, ResourceType> types) {
                        System.out.println("Preloaded [" + types.size() + "] resource types");
                        buildCoreUI();
                    }
                });
                */
            }
        });
    }


    private void buildCoreUI() {

        RootCanvas rootCanvas = new RootCanvas();
        this.rootView = new View(new ViewId("", null), rootCanvas);


//        HTMLPane menuPane = new HTMLPane();
//        menuPane.setWidth100();
//        menuPane.setHeight(26);
//        menuPane.setContentsType(ContentsType.PAGE);
//        menuPane.setContentsURL("/rhq/common/menu/menu.xhtml");
//        menuPane.setZIndex(400000);
//
//        layout.addMember(menuPane);


        MenuBarView menuBarView = new MenuBarView();
        menuBarView.setWidth("100%");
//        WidgetCanvas menuCanvas = new WidgetCanvas(menuBarView);
//        menuCanvas.setTop(0);
//        menuCanvas.setWidth100();
//        menuCanvas.draw();

        rootCanvas.addMember(menuBarView);


        breadCrumb = new BreadcrumbTrailPane();

        rootCanvas.addMember(breadCrumb);


        DOM.setInnerHTML(RootPanel.get("Loading-Panel").getElement(), "");


        Canvas canvas = new Canvas(CONTENT_CANVAS_ID);
        canvas.setWidth100();
        canvas.setHeight100();

        rootCanvas.addMember(canvas);


//        canvas.addChild(new AdministrationView()) ; //DemoCanvas());


        rootCanvas.draw();        

        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> historyChangeEvent) {
                String path = historyChangeEvent.getValue();
                System.out.println("History request: " + path);

                List<String> viewIdNames = path.equals("") ? Collections.<String>emptyList() : Arrays.asList(path.split("\\/"));

                View parentView = CoreGUI.this.rootView;
                ViewRenderer viewRenderer = (ViewRenderer)parentView.getCanvas();
                List<Breadcrumb> breadcrumbs = new ArrayList<Breadcrumb>(viewIdNames.size());
                try {
                    for (int i = 0, viewIdNamesSize = viewIdNames.size(); i < viewIdNamesSize; i++) {
                        String viewIdName = viewIdNames.get(i);
                        Canvas parentCanvas = parentView.getCanvas();
                        if (parentCanvas != null && parentCanvas instanceof ViewRenderer) {
                            viewRenderer = (ViewRenderer) parentCanvas;
                        }
                        ViewId viewId = new ViewId(viewIdName, parentView.getId());
                        boolean lastNode = (i == (viewIdNamesSize - 1));
                        parentView = viewRenderer.renderView(viewId, parentView, lastNode);
                        breadcrumbs.add(parentView.getBreadcrumb());
                    }
                } catch (UnknownViewException e) {
                    // Abort the for-loop, since once we hit an unknown name, we don't care about any remaining names
                    // in the list. The breadcrumbs list will contain breadcrumbs for only the names that were
                    // recognized.
                    System.err.println(e.getMessage());
                }

                System.out.println("Breadcrumbs: " + breadcrumbs);
                breadCrumb.setBreadcrumbs(breadcrumbs);

                /*breadCrumb.initialize(path);
                ArrayList<Place> trail = breadCrumb.getTrail();

                Place base = trail.get(0);

                if (content != null && content instanceof Presenter) {
                    Presenter p = ((Presenter) content);
                    if (!p.fireDisplay(base, trail.subList(1, trail.size()))) {
                        Canvas c = createContent(base.getId());
                        setContent(c);

                        if (trail.size() >= 2 && c instanceof Presenter) {
                            ((Presenter) c).fireDisplay(trail.get(0), trail.subList(1, trail.size()));
                        }
                    }
                } else {
                    Canvas c = createContent(base.getId());
                    setContent(c);
                    if (trail.size() >= 2 && c instanceof Presenter) {
                        ((Presenter) c).fireDisplay(trail.get(0), trail.subList(1, trail.size()));
                    }
                }*/

                breadCrumb.refresh();
            }
        });

        History.fireCurrentHistoryState();
    }

    public Canvas createContent(String breadcrumbName) {
        Canvas canvas;
        if (breadcrumbName.equals("Administration")) {
            canvas = new AdministrationView();
        } else if (breadcrumbName.equals("Demo")) {
            canvas = new DemoCanvas();
        } else if (breadcrumbName.equals("Resources")) {
            canvas = new ResourcesView();
        } else if (breadcrumbName.equals("Resource")) {
            canvas = new ResourceView();
        } else if (breadcrumbName.equals("Dashboard")) {
            canvas = new DashboardView();
        } else {
            canvas = null;
        }
        return canvas;
    }


    // -------------------- Static application utilities ----------------------

    public static ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public static Subject getSessionSubject() {
        return sessionSubject;
    }

    public static void setSessionSubject(Subject subject) {
        GWTServiceLookup.registerSession(String.valueOf(subject.getSessionId()));

        // TODO this breaks because of reattach rules, bizarely even in queries. gonna switch out to non-subject include apis
        // Create a minimized session object for validation on requests
//        Subject s = new Subject(subject.getName(),subject.getFactive(), subject.getFsystem());
//        s.setSessionId(subject.getSessionId());
        CoreGUI.sessionSubject = subject;
    }

    public static void setContent(Canvas newContent) {
        Canvas contentCanvas = Canvas.getById(CONTENT_CANVAS_ID);
        if (contentCanvas.getChildren().length > 0)
            contentCanvas.getChildren()[0].destroy();

        if (newContent != null) {
            content = newContent;
            contentCanvas.addChild(newContent);
        }
        contentCanvas.draw();
    }

    public static void goTo(String path) {
        History.newItem(path, true);
    }

    public static void updateBreadCrumbDisplayName(ViewId viewId, String displayName) {
        int index = -1;
        for (ViewId current = viewId.getParent(); current != null; current = current.getParent()) {
           index++;
        }
        List<Breadcrumb> breadcrumbs = breadCrumb.getBreadcrumbs();
        if (index < breadcrumbs.size()) {
            Breadcrumb breadcrumb = breadcrumbs.get(index);
            if (breadcrumb.getName().equals(viewId.getName())) {
                breadcrumbs.set(index, new Breadcrumb(breadcrumb.getName(), displayName));
                breadCrumb.refresh();
            }
        }
    }

    private class RootCanvas extends VLayout implements ViewRenderer {
        private RootCanvas() {
            setWidth100(); // (1200);
            setHeight100(); // (900);
        }

        public View renderView(ViewId viewId, View parentView, boolean lastNode) throws UnknownViewException {
            String path = viewId.getPath();
            Canvas canvas = createContent(path);
            if (canvas == null) {
                throw new UnknownViewException();
            }
            setContent(canvas);
            return new View(viewId, canvas);
        }
    }
}


