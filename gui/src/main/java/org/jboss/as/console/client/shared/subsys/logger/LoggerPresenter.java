package org.jboss.as.console.client.shared.subsys.logger;

import com.allen_sauer.gwt.log.client.Log;
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
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.logger.wizard.HandlerContext;
import org.jboss.as.console.client.shared.subsys.logger.wizard.TwoStepWizard;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 */
public class LoggerPresenter extends Presenter<LoggerPresenter.MyView, LoggerPresenter.MyProxy> {

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final CrudOperationDelegate operationDelegate;
    private final StatementContext statementContext;

    private DefaultWindow window;

    CrudOperationDelegate.Callback defaultOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(AddressTemplate address, String name) {
            Console.info("Successfully saved resource "+address.resolve(statementContext, name));
            loadData();
        }

        @Override
        public void onFailure(AddressTemplate address, String name, Throwable t) {
            loadData();
            Console.error("Failed to save resource " + address.resolve(statementContext, name), t.getMessage());
        }
    };

    private SecurityFramework securityFramework;
    private ResourceDescriptionRegistry descriptionRegistry;

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    @RequiredResources(
            resources = {
                    "{selected.profile}/subsystem=logging",
                    "{selected.profile}/subsystem=logging/root-logger=ROOT",
                    "{selected.profile}/subsystem=logging/logger=*",
                    "{selected.profile}/subsystem=logging/console-handler=*",
                    "{selected.profile}/subsystem=logging/custom-handler=*",
                    "{selected.profile}/subsystem=logging/file-handler=*",
                    "{selected.profile}/subsystem=logging/periodic-rotating-file-handler=*",
                    "{selected.profile}/subsystem=logging/periodic-size-rotating-file-handler=*",
                    "{selected.profile}/subsystem=logging/size-rotating-file-handler=*",
                    "{selected.profile}/subsystem=logging/syslog-handler=*",
                    "{selected.profile}/subsystem=logging/async-handler=*",
                    "{selected.profile}/subsystem=logging/pattern-formatter=*",
                    "{selected.profile}/subsystem=logging/custom-formatter=*"
            }
    )
    @ProxyCodeSplit
    @NameToken(NameTokens.Logging)
    public interface MyProxy extends Proxy<LoggerPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(LoggerPresenter presenter);
        void updateRootLogger(ModelNode payload);
        void updateLogger(List<Property> items);
        void updateHandler(List<Property> items);

        void updatePeriodicSizeHandler(List<Property> items);

        void updateConsoleHandler(List<Property> items);
        void updateFileHandler(List<Property> items);
        void updatePeriodicHandler(List<Property> items);
        void updateSizeHandlerHandler(List<Property> items);
        void updateAsyncHandler(List<Property> items);
        void updateCustomHandler(List<Property> items);
        void updateSyslogHandler(List<Property> items);

        void updatePatternFormatter(List<Property> items);

        void updateCustomFormatter(List<Property> items);
    }

    @Inject
    public LoggerPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, RevealStrategy revealStrategy,
            DispatchAsync dispatcher, CoreGUIContext statementContext,
            SecurityFramework securityFramework, ResourceDescriptionRegistry descriptionRegistry) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;

        this.statementContext =  statementContext;

        this.operationDelegate = new CrudOperationDelegate(this.statementContext, dispatcher);

    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }


    @Override
    protected void onReset() {
        super.onReset();
        loadData();
    }

    private void loadData() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "logging");
        operation.get("recursive-depth").set(2);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to load logging resources", response.getFailureDescription());

                } else {
                    ModelNode logging = response.get(RESULT);

                    getView().updateRootLogger(logging.get("root-logger").get("ROOT"));
                    getView().updateLogger(logging.get("logger").asPropertyList());
                    getView().updateConsoleHandler(logging.get("console-handler").asPropertyList());

                    getView().updateConsoleHandler(logging.get("console-handler").asPropertyList());
                    getView().updateFileHandler(logging.get("file-handler").asPropertyList());
                    getView().updatePeriodicHandler(logging.get("periodic-rotating-file-handler").asPropertyList());
                    getView().updatePeriodicSizeHandler(logging.get("periodic-size-rotating-file-handler").asPropertyList());
                    getView().updateSizeHandlerHandler(logging.get("size-rotating-file-handler").asPropertyList());
                    getView().updateAsyncHandler(logging.get("async-handler").asPropertyList());
                    getView().updateCustomHandler(logging.get("custom-handler").asPropertyList());
                    getView().updateSyslogHandler(logging.get("syslog-handler").asPropertyList());

                    getView().updateCustomFormatter(logging.get("custom-formatter").asPropertyList());
                    getView().updatePatternFormatter(logging.get("pattern-formatter").asPropertyList());
                }
            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public void onRemoveResource(final AddressTemplate address, final String name) {

        operationDelegate.onRemoveResource(address, name, defaultOpCallbacks);
    }

    public void onCreateResource(AddressTemplate address, ModelNode entity) {
        ResourceAddress fqAddress = address.resolve(statementContext);

        entity.get(OP).set(ADD);
        entity.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(entity), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error("Failed to create resource " + fqAddress, response.getFailureDescription());
                } else {

                    Console.info("Successfully created " + fqAddress);
                }

                loadData();
            }
        });
    }

    public void onSaveResource(final AddressTemplate address, Map<String, Object> changeset) {

        operationDelegate.onSaveResource(address, null, changeset, defaultOpCallbacks);
    }

    public void onSaveNamedResource(AddressTemplate address, String name, Map changeset) {
        operationDelegate.onSaveResource(address, name, changeset, defaultOpCallbacks);
    }

    public void onSaveFileAttributes(AddressTemplate address, String name, ModelNode payload) {
        operationDelegate.onSaveComplexAttribute(address, name, "file", payload, defaultOpCallbacks);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    public void onLaunchAddResourceDialog(AddressTemplate address) {
        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(type.toUpperCase()));
        window.setWidth(480);
        window.setHeight(360);

        window.setWidget(
                new AddResourceDialog(
                        Console.MODULES.getSecurityFramework().getSecurityContext(getProxy().getNameToken()),
                        descriptionRegistry.lookup(address),
                        new AddResourceDialog.Callback() {
                            @Override
                            public void onAdd(ModelNode payload) {
                                window.hide();
                                operationDelegate.onCreateResource(
                                        address, payload.get("name").asString(), payload, defaultOpCallbacks);
                            }

                            @Override
                            public void onCancel() {
                                window.hide();
                            }
                        }
                )
        );

        window.setGlassEnabled(true);
        window.center();
    }

    public void onLaunchAddResourceDialogFile(AddressTemplate address) {
        String type = address.getResourceType();

        new TwoStepWizard(
                this,
                address,
                Console.MODULES.getSecurityFramework().getSecurityContext(getProxy().getNameToken()),
                descriptionRegistry.lookup(address)

        ).open(Console.MESSAGES.createTitle(type.toUpperCase()));

    }

    public void onCreateHandler(AddressTemplate address, HandlerContext context) {
        ModelNode payload = context.getAttributes();
        payload.get("file").set(context.getFileAttribute());

        operationDelegate.onCreateResource(
                address, payload.get("name").asString(), payload, defaultOpCallbacks
        );

    }

}
