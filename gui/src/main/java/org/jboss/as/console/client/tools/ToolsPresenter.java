package org.jboss.as.console.client.tools;

import com.google.gwt.debugpanel.client.DebugPanel;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import com.gwtplatform.mvp.client.proxy.RevealRootPopupContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.AccessLogView;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.rbac.internal.RunAsRoleTool;
import org.jboss.ballroom.client.widgets.forms.ResolveExpressionEvent;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 6/15/12
 */
public class ToolsPresenter extends Presenter<ToolsPresenter.MyView, ToolsPresenter.MyProxy>
{

    private final PlaceManager placeManager;
    private final DispatchAsync dispatcher;
    private final BootstrapContext context;
    private BrowserPresenter browser;

    private String requestedTool;
    private DefaultWindow window;
    private RunAsRoleTool runAsRoleTool;

    @ProxyCodeSplit
    @NameToken(NameTokens.ToolsPresenter)
    @NoGatekeeper
    public interface MyProxy extends Proxy<ToolsPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(ToolsPresenter presenter);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent =
            new GwtEvent.Type<RevealContentHandler<?>>();

    @Inject
    public ToolsPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, BrowserPresenter browser, DispatchAsync dispatcher, BootstrapContext context) {
        super(eventBus, view, proxy);
        this.placeManager = placeManager;
        //this.debug = debug;
        this.browser = browser;
        this.dispatcher = dispatcher;
        this.context = context;
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        requestedTool = request.getParameter("name", null);
    }

    @Override
    protected void revealInParent() {

        if("expressions".equals(requestedTool))
        {
            getEventBus().fireEventFromSource(new ResolveExpressionEvent("${name:default_value}"), this);
        }
        else if("browser".equals(requestedTool))
        {
            RevealRootPopupContentEvent.fire(this, browser);
        }
        else if("debug-panel".equals(requestedTool))
        {
            if(window==null)
            {
                window = new DefaultWindow("Diagnostics");
                window.setWidth(480);
                window.setHeight(360);


                DebugPanel debugPanel = new DebugPanel();
                Widget debugWidget = debugPanel.asWidget();
                window.setWidget(
                        new ScrollPanel(debugWidget)
                );

                window.setModal(false);
                //window.setGlassEnabled(true);
                window.center();
            }
            else
            {
                window.show();
            }
            //RevealRootPopupContentEvent.fire(this, debug);
        }
        else if("mbui-workbench".equals(requestedTool))
        {
            placeManager.revealPlace(new PlaceRequest("mbui-workbench"));
        }
        else if("access-log".equals(requestedTool))
        {
            if(window == null)
            {
                window = new DefaultWindow("Resource Access Log");
                window.setWidth(480);
                window.setHeight(360);


                AccessLogView panel = new AccessLogView();
                Widget w = panel.asWidget();
                window.setWidget(new ScrollPanel(w));

                window.setModal(false);
                //window.setGlassEnabled(true);
                window.center();
            }
            else
            {
                window.show();
            }
            //RevealRootPopupContentEvent.fire(this, debug);
        }
        else if ("run-as-role".equals(requestedTool) && context.isSuperUser()) {
            if (runAsRoleTool == null) {
                runAsRoleTool = new RunAsRoleTool();
            }

            final ModelNode operation = new ModelNode();
            operation.get(OP).set(COMPOSITE);
            operation.get(ADDRESS).setEmptyList();
            final List<ModelNode> steps = new LinkedList<ModelNode>();

            ModelNode s = new ModelNode();
            s.get(OP).set(READ_ATTRIBUTE_OPERATION);
            s.get(NAME).set("standard-role-names");
            s.get(ADDRESS).add("core-service", "management").add("access", "authorization");
            steps.add(s);

            final boolean domain = !Console.MODULES.getBootstrapContext().isStandalone();
            if (domain)
            {
                ModelNode sg = new ModelNode();
                sg.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
                sg.get(ADDRESS).add("core-service", "management");
                sg.get(ADDRESS).add("access", "authorization");
                sg.get(CHILD_TYPE).set("server-group-scoped-role");
                steps.add(sg);

                ModelNode h = new ModelNode();
                h.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
                h.get(ADDRESS).add("core-service", "management");
                h.get(ADDRESS).add("access", "authorization");
                h.get(CHILD_TYPE).set("host-scoped-role");
                steps.add(h);

            }
            operation.get(STEPS).set(steps);

            // In case we're already in "Run As"-mode the next DMR op will fail.
            // So temporarily disable run as for the next call
            final String runAs = Console.getBootstrapContext().getRunAs();
            if (runAs != null) {
                dispatcher.clearProperty("run_as");
            }
            dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(DMRResponse result) {
                    restoreRunAs();

                    Set<String> serverGroupScoped = new HashSet<String>();
                    Set<String> hostScoped = new HashSet<String>();

                    ModelNode compositeResponse = result.get();

                    List<ModelNode> standardRoles= compositeResponse.get(RESULT).get("step-1").get(RESULT).asList();
                    for (ModelNode node : standardRoles) {
                        StandardRole.add(node.asString());
                    }

                    if (domain) {
                        List<ModelNode> serverGroupRoles = compositeResponse.get(RESULT).get("step-2").get(RESULT).asList();
                        for(ModelNode role : serverGroupRoles)
                        {
                            serverGroupScoped.add(role.asString());
                        }

                        List<ModelNode> hostRoles = compositeResponse.get(RESULT).get("step-3").get(RESULT).asList();
                        for(ModelNode role : hostRoles)
                        {
                            hostScoped.add(role.asString());
                        }
                    }
                    runAsRoleTool.setScopedRoles(serverGroupScoped, hostScoped);
                    runAsRoleTool.launch();
                }

                @Override
                public void onFailure(final Throwable caught) {
                    restoreRunAs();
                    super.onFailure(caught);
                }

                private void restoreRunAs() {
                    if (runAs != null) {
                        dispatcher.setProperty("run_as", runAs);
                    }
                }
            });
        }
    }
}
