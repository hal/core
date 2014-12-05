package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.behaviour.CrudOperationDelegate;
import org.jboss.as.console.mbui.behaviour.DefaultPresenterContract;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 11/28/11
 */
public class EEPresenter extends Presenter<EEPresenter.MyView, EEPresenter.MyProxy>
        implements DefaultPresenterContract {

    @ProxyCodeSplit
    @NameToken(NameTokens.EEPresenter)
    @RequiredResources(resources = {"{selected.profile}/subsystem=ee"})
    @SearchIndex(keywords = {"thread-factory", "ee", "context-service", "scheduler", "executor", "managed-bean"})
    public interface MyProxy extends Proxy<EEPresenter>, Place {}


    public interface MyView extends View {
        void setPresenter(EEPresenter presenter);
        void updateFrom(ModelNode data);
        void setModules(List<ModelNode> modules);
        void setContextServices(List<Property> services);
        void setThreadFactories(List<Property> data);
        void setExecutor(List<Property> data);
        void setScheduledExecutor(List<Property> data);
        void setBindings(ModelNode data);
        boolean isLegacyView();
    }


    private final PlaceManager placeManager;
    private final CrudOperationDelegate operationDelegate;

    private RevealStrategy revealStrategy;
    private ApplicationMetaData metaData;
    private DispatchAsync dispatcher;
    private final CoreGUIContext statementContext;

    private DefaultWindow window;
    private List<ModelNode> globalModules = new ArrayList<ModelNode>();


    @Inject
    public EEPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, DispatchAsync dispatcher,
            RevealStrategy revealStrategy, CoreGUIContext statementContext) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(this.statementContext, dispatcher);
    }

    CrudOperationDelegate.Callback defaultOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(ResourceAddress address, String name) {

            loadSubsystem();
        }

        @Override
        public void onFailure(ResourceAddress address, String name, Throwable t) {
            // noop
        }
    };
    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }


    @Override
    protected void onReset() {
        super.onReset();
        loadSubsystem();
    }

    public void launchNewModuleDialogue() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Module"));
        window.setWidth(480);
        window.setHeight(360);

        window.trapWidget(
                new NewModuleWizard(this).asWidget()
        );

        window.setGlassEnabled(true);
        window.center();
    }

    public void onAddModule(ModelNode module) {
        closeDialoge();

        // TODO: keeping the state in presenter?
        globalModules.add(module);

        persistsModules(globalModules);
    }

    private void persistsModules(List<ModelNode> modules) {
        ModelNode operation= new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "ee");
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("global-modules");

        operation.get(VALUE).set(modules);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response  = result.get();

                if(response.isFailure())
                {
                    Console.error(Console.MESSAGES.modificationFailed("Modules"), response.getFailureDescription());
                }
                else
                {
                    Console.info(Console.MESSAGES.modified("Modules"));
                }

                loadSubsystem();
            }
        });
    }


    private void loadSubsystem() {

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "ee");
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response  = result.get();

                if(response.isFailure())
                {
                    Console.error(Console.MESSAGES.failed("Loading EE Subsystem"), response.getFailureDescription());
                }
                else
                {
                    ModelNode data = response.get(RESULT).asObject();

                    globalModules = failSafeGetCollection(data.get("global-modules"));
                    getView().setModules(globalModules);
                    getView().updateFrom(data);

                    if(!getView().isLegacyView()) {
                        getView().setContextServices(failSafeGetProperties(data.get("context-service")));
                        getView().setExecutor(failSafeGetProperties(data.get("managed-executor-service")));
                        getView().setScheduledExecutor(failSafeGetProperties(data.get("managed-scheduled-executor-service")));
                        getView().setThreadFactories(failSafeGetProperties(data.get("managed-thread-factory")));
                        getView().setBindings(data.get("service").get("default-bindings").asObject());
                    }
                }
            }
        });
    }

    private static List<ModelNode> failSafeGetCollection(ModelNode item) {

        // the DMR API returns unmodifiable collections
        List<ModelNode> modules = new ArrayList<>();
        if(item.isDefined())
            modules.addAll(item.asList());
        return modules;
    }

    private static List<Property> failSafeGetProperties(ModelNode item) {

            if(item.isDefined())
                return item.asPropertyList();
            else
                return Collections.EMPTY_LIST;
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public void closeDialoge() {
        window.hide();
    }

    public void onRemoveModule(ModelNode module) {

        List<ModelNode> modules = new ArrayList<ModelNode>();

        for(ModelNode m : globalModules)
        {
            if(!m.get(NAME).equals(module.get(NAME)))
            {
                modules.add(m);
            }
        }

        persistsModules(modules);
    }

    @Override
    public void onLaunchAddResourceDialog(final String addressString) {
        ResourceAddress address = new ResourceAddress(addressString, statementContext);
        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(type.toUpperCase()));
        window.setWidth(480);
        window.setHeight(360);

        window.setWidget(
                new AddResourceDialog(
                        addressString,
                        statementContext,
                        Console.MODULES.getSecurityFramework().getSecurityContext(getProxy().getNameToken()),
                        new AddResourceDialog.Callback() {
                            @Override
                            public void onAddResource(ResourceAddress address, ModelNode payload) {
                                window.hide();
                                operationDelegate.onCreateResource(addressString, payload, defaultOpCallbacks);
                            }

                            @Override
                            public void closeDialogue() {
                                window.hide();
                            }
                        }
                )
        );

        window.setGlassEnabled(true);
        window.center();
    }

    @Override
    public void onRemoveResource(String addressString, String name) {
        operationDelegate.onRemoveResource(addressString, name, defaultOpCallbacks);
    }

    @Override
    public void onSaveResource(String addressString, String name, Map<String, Object> changeset) {
        operationDelegate.onSaveResource(addressString, name, changeset, defaultOpCallbacks);
    }
}
