package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelNodeUtil;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 */
public class ResourceAdapterFinder extends Presenter<ResourceAdapterFinder.MyView, ResourceAdapterFinder.MyProxy>
        implements PreviewEvent.Handler {

    static final String ARCHIVE_FIELD = "archive";
    static final String MODULE_FIELD = "module";

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityFramework securityFramework;
    private final CoreGUIContext statementContext;

    private DefaultWindow window;

    private final static AddressTemplate BASE_ADDRESS = AddressTemplate.of("{selected.profile}/subsystem=resource-adapters/resource-adapter=*");

    @ProxyCodeSplit
    @NameToken(NameTokens.ResourceAdapterFinder)
    @RequiredResources(resources = {"{selected.profile}/subsystem=resource-adapters/resource-adapter=*"})
    @SearchIndex(keywords = {"jca", "resource-adapter", "connector", "workmanager", "bootstrap-context"})
    public interface MyProxy extends Proxy<ResourceAdapterFinder>, Place {}


    public interface MyView extends View {
        void setPresenter(ResourceAdapterFinder presenter);
        void updateFrom(List<Property> list);
        void setPreview(SafeHtml html);
    }

    @Inject
    public ResourceAdapterFinder(
            EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            DispatchAsync dispatcher, RevealStrategy revealStrategy,
            ResourceDescriptionRegistry descriptionRegistry, SecurityFramework securityFramework,
            CoreGUIContext statementContext) {

        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.descriptionRegistry = descriptionRegistry;

        this.securityFramework = securityFramework;
        this.statementContext = statementContext;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getEventBus().addHandler(PreviewEvent.TYPE, this);
        getView().setPresenter(this);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {

    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    @Override
    protected void onReset() {
        super.onReset();

        if(placeManager.getCurrentPlaceRequest().matchesNameToken(getProxy().getNameToken()))
            loadAdapter();
    }


    private void loadAdapter() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "resource-adapters");
        operation.get(CHILD_TYPE).set("resource-adapter");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse response) {
                ModelNode result = response.get();
                List<Property> resourceAdapters = result.get(RESULT).asPropertyList();
                getView().updateFrom(resourceAdapters);

            }
        });
    }

    @Override
    protected void revealInParent() {
        if(Console.getBootstrapContext().isStandalone())
            RevealContentEvent.fire(this, ServerMgmtApplicationPresenter.TYPE_MainContent, this);
        else
            RevealContentEvent.fire(this, ProfileMgmtPresenter.TYPE_MainContent, this);
    }

    @Override
    public void onPreview(PreviewEvent event) {
        if(isVisible())
            getView().setPreview(event.getHtml());
    }

    public void launchNewAdapterWizard() {
        final SecurityContext securityContext =
                securityFramework.getSecurityContext(getProxy().getNameToken());

        final ResourceDescription resourceDescription = descriptionRegistry.lookup(BASE_ADDRESS);
        final DefaultWindow dialog = new DefaultWindow("New Resource Adapter");
        ModelNodeFormBuilder.FormAssets resourceAdapterAssets = new ModelNodeFormBuilder()
            .setCreateMode(true)
            .setRequiredOnly(true)
            .setConfigOnly()
            .include(ARCHIVE_FIELD,MODULE_FIELD, "transaction-support")
            .setResourceDescription(resourceDescription)
            .setSecurityContext(securityContext)
            .build();

        resourceAdapterAssets.getForm().setEnabled(true);

        AddResourceDialog addDialog = new AddResourceDialog(resourceAdapterAssets, resourceDescription, new AddResourceDialog.Callback() {
            @Override
            public void onAdd(ModelNode payload) {
                dialog.hide();

                final ResourceAddress fqAddress =
                        BASE_ADDRESS.resolve(statementContext, payload.get("name").asString());

                payload.get(OP).set(ADD);
                payload.get(ADDRESS).set(fqAddress);

                dispatcher.execute(new DMRAction(payload), new SimpleCallback<DMRResponse>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        super.onFailure(caught);
                        loadAdapter();
                    }

                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {
                        Console.info("Successfully added " + payload.get("name").asString());
                        loadAdapter();
                    }
                });
            }

            @Override
            public void onCancel() {
                dialog.hide();
            }
        });
        resourceAdapterAssets.getForm().addFormValidator((formItems, outcome) -> {
            FormItem<String> archiveItem = formItem(formItems, ARCHIVE_FIELD);
            FormItem<String> moduleItem = formItem(formItems, MODULE_FIELD);
            if (archiveItem.isUndefined() && moduleItem.isUndefined()) {
                moduleItem.setErrMessage("Please set either module or archive.");
                moduleItem.setErroneous(true);
                outcome.addError(MODULE_FIELD);
            }
        });

        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    public void closeDialoge() {
        window.hide();
    }

    @SuppressWarnings("unchecked")
    private <T> FormItem<T> formItem(List<FormItem> formItems, String name) {
        for (FormItem formItem : formItems) {
            if (name.equals(formItem.getName())) {
                return formItem;
            }
        }
        return null;
    }


    public void onDelete(final Property ra) {

        ResourceAddress fqAddress = BASE_ADDRESS.resolve(statementContext, ra.getName());

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                loadAdapter();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();
                if (ModelNodeUtil.indicatesSuccess(result))
                    Console.info(Console.MESSAGES.deleted("Resource Adapter " + ra.getName()));
                else
                    Console.error(Console.MESSAGES.deletionFailed("Resource Adapter " + ra.getName()),
                            result.toString());

                loadAdapter();
            }
        });

    }

}
