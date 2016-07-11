package org.jboss.as.console.client.shared.subsys.undertow;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.UNDERTOW_RESPONSE_FILTER;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class HttpPresenter extends Presenter<HttpPresenter.MyView, HttpPresenter.MyProxy>
        implements CommonHttpPresenter {

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final CrudOperationDelegate operationDelegate;
    private final FilteringStatementContext statementContext;

    private String currentServer;
    private DefaultWindow window;
    
    // the selected host in the host setting view.
    private String selectedHost;

    CrudOperationDelegate.Callback defaultOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(AddressTemplate address, String name) {

            Console.info(Console.MESSAGES.successfullyModifiedResource(address.resolve(statementContext, name).toString()));

            if(address.getResourceType().equals("server"))
                loadServer();
            else {
                loadDetails();
                loadGeneralSettings();
            }
        }

        @Override
        public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
            Console.error(Console.MESSAGES.failedToModifyResource(addressTemplate.toString()), t.getMessage());
        }
    };

    private SecurityFramework securityFramework;
    private ResourceDescriptionRegistry descriptionRegistry;


    @RequiredResources(
            resources = {
                    "{selected.profile}/subsystem=undertow",
                    "{selected.profile}/subsystem=undertow/server={undertow.server}",
                    "{selected.profile}/subsystem=undertow/server={undertow.server}/host=*",
                    "{selected.profile}/subsystem=undertow/server={undertow.server}/host=*/filter-ref=*",
                    "{selected.profile}/subsystem=undertow/server={undertow.server}/http-listener=*",
                    "{selected.profile}/subsystem=undertow/server={undertow.server}/https-listener=*",
                    "{selected.profile}/subsystem=undertow/server={undertow.server}/ajp-listener=*"
            }, recursive = false)
    @ProxyCodeSplit
    @NameToken(NameTokens.HttpPresenter)
    public interface MyProxy extends Proxy<HttpPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(HttpPresenter presenter);

        void setServer(List<Property> server);

        void setServerSelection(String name);

        void setHttpListener(List<Property> httpListener);

        void setAjpListener(List<Property> ajpListener);

        void setHttpsListener(List<Property> httpsListener);

        void setHosts(List<Property> hosts);

        void setConfig(ModelNode data);

        void selectModifiedHost(String hostname);
    }

    @Inject
    public HttpPresenter(
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

        this.statementContext =  new FilteringStatementContext(
                statementContext,
                new FilteringStatementContext.Filter() {
                    @Override
                    public String filter(String key) {
                        if ("undertow.server".equals(key))
                            return currentServer;
                        else
                            return null;
                    }

                    @Override
                    public String[] filterTuple(String key) {
                        return null;
                    }
                }
        ) {

        };

        this.operationDelegate = new CrudOperationDelegate(this.statementContext, dispatcher);
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }


    @Override
    public String getNameToken() {
        return getProxy().getNameToken();
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }


    @Override
    protected void onReset() {
        super.onReset();
        loadGeneralSettings();
        loadServer();
    }

    private void loadServer() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "undertow");
        operation.get(CHILD_TYPE).set("server");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to load http server", response.getFailureDescription());
                    getView().setServer(Collections.EMPTY_LIST);
                } else {
                    getView().setServer(response.get(RESULT).asPropertyList());
                    getView().setServerSelection(currentServer);
                }
                // navigating to a server, de-selects the previous selected host.
                selectedHost = null;
            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        currentServer = request.getParameter("name", null);
    }

    public void loadGeneralSettings() {


        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "undertow");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Log.error("Failed to load undertow configuration", response.getFailureDescription());
                }
                else
                {
                    ModelNode data = response.get(RESULT);
                    getView().setConfig(data);
                }

            }
        });
    }


    public void loadDetails() {

        // no server selected
        if(null==currentServer)
            return;

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "undertow");
        operation.get(ADDRESS).add("server", currentServer);
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Log.error("Failed to load http server details", response.getFailureDescription());
                }
                else
                {
                    ModelNode data = response.get(RESULT);
                    getView().setHttpListener(failSafeGetCollection(data.get("http-listener")));
                    getView().setAjpListener(failSafeGetCollection(data.get("ajp-listener")));
                    getView().setHttpsListener(failSafeGetCollection(data.get("https-listener")));
                    getView().setHosts(failSafeGetCollection(data.get("host")));
                    if (selectedHost != null)
                        getView().selectModifiedHost(selectedHost);
                }

            }
        });
    }

    private static List<Property> failSafeGetCollection(ModelNode item) {
        if(item.isDefined())
            return item.asPropertyList();
        else
            return Collections.EMPTY_LIST;
    }

    // -----------------------

    public void onLaunchAddResourceDialog(final AddressTemplate address) {
        onLaunchAddResourceDialog(address, null);
    }
    
    public void onLaunchAddResourceDialog(AddressTemplate address, String hostname) {

        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(type.toUpperCase()));
        window.setWidth(480);
        window.setHeight(360);

        ResourceDescription lookup = descriptionRegistry.lookup(address);
        if (hostname != null) 
            address = address.replaceWildcards(hostname);
        window.setWidget(newAddResourceDialog(lookup, address));
        window.setGlassEnabled(true);
        window.center();
    }

    static java.util.logging.Logger _log = java.util.logging.Logger.getLogger("org.jboss");


    public void onLaunchAddFilterReferenceDialog(AddressTemplate address, String hostname) {

        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(type.toUpperCase()));
        window.setWidth(480);
        window.setHeight(360);

        ResourceDescription resoourceDescription = descriptionRegistry.lookup(address);
        AddressTemplate resolvedAddress = address.replaceWildcards(hostname);

        // the fields are added manually, because the name field should be autocomplete (SuggestionTextBox)
        // and the ModelNodeFormBuilder just excludes the name attribute.
        ModelNode attributes = resoourceDescription.get("attributes");
        ModelNode attrDesc = attributes.get("priority");
        FormItem priorityItem = null;
        if (attrDesc.hasDefined("min") && attrDesc.hasDefined("max")) {
            priorityItem = new NumberBoxItem(
                "priority", Console.CONSTANTS.common_label_priority(),
                attrDesc.get("min").asLong(),
                attrDesc.get("max").asLong()
            );
        }
        priorityItem.setRequired(false);

        TextBoxItem predicateItem = new TextBoxItem("predicate", Console.CONSTANTS.common_label_predicate(), false);

        // this is the textfield with autocomplete, the suggestions comes from the response filters
        FormItem responseFilterName = new SuggestionResource(NAME, Console.CONSTANTS.common_label_name(), true,
                Console.MODULES.getCapabilities().lookup(UNDERTOW_RESPONSE_FILTER))
                .buildFormItem();

        // a modelnodeformbuilder.formassets is used to pass as parameter to AddResourceDialog 
        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setResourceDescription(resoourceDescription)
                .setSecurityContext(Console.MODULES.getSecurityFramework().getSecurityContext(NameTokens.HttpPresenter));
        ModelNodeFormBuilder.FormAssets formAssets = builder.build();
        ModelNodeForm form = formAssets.getForm();
        form.setFields(responseFilterName, predicateItem, priorityItem);
        form.setEnabled(true);
        
        AddResourceDialog addResourceDialog = new AddResourceDialog(formAssets,
                resoourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        window.hide();
                        operationDelegate.onCreateResource(resolvedAddress, payload.get(NAME).asString(), payload, defaultOpCallbacks);
                    }

                    @Override
                    public void onCancel() {
                        window.hide();
                    }
                }
        );
        window.setWidget(addResourceDialog);
        window.setGlassEnabled(true);
        window.center();

    }

    private AddResourceDialog newAddResourceDialog(ResourceDescription resourceDescription, final AddressTemplate address) {
        return new AddResourceDialog(
                Console.MODULES.getSecurityFramework().getSecurityContext(NameTokens.HttpPresenter),
                resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        window.hide();
                        operationDelegate.onCreateResource(address, payload.get(NAME).asString(), payload, defaultOpCallbacks);
                    }

                    @Override
                    public void onCancel() {
                        window.hide();
                    }
                }
        );
    }

    public void onRemoveResource(final AddressTemplate address, final String name) {

        operationDelegate.onRemoveResource(address, name, defaultOpCallbacks);
    }

    public void onSaveResource(final AddressTemplate address, String name, Map<String, Object> changeset) {

        operationDelegate.onSaveResource(address, name, changeset, defaultOpCallbacks);
    }
    
    void setSelectedHost(String host) {
        this.selectedHost = host;
    }

}
