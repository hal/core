package org.jboss.as.console.client.shared.subsys.jca;

import com.allen_sauer.gwt.log.client.Log;
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
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.AdminObject;
import org.jboss.as.console.client.shared.subsys.jca.model.ConnectionDefinition;
import org.jboss.as.console.client.shared.subsys.jca.model.ResourceAdapter;
import org.jboss.as.console.client.shared.subsys.jca.wizard.NewAdapterWizard;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.widgets.forms.AddressBinding;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelNodeUtil;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 */
public class ResourceAdapterFinder extends Presenter<ResourceAdapterFinder.MyView, ResourceAdapterFinder.MyProxy>
        implements PreviewEvent.Handler {

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final BeanFactory beanFactory;
    private final EntityAdapter<ResourceAdapter> adapter;
    private final BeanMetaData beanMetaData;


    private DefaultWindow window;

    @ProxyCodeSplit
    @NameToken(NameTokens.ResourceAdapterFinder)
    @RequiredResources(resources = {"/{selected.profile}/subsystem=resource-adapters/resource-adapter=*"})
    @SearchIndex(keywords = {"jca", "resource-adapter", "connector", "workmanager", "bootstrap-context"})
    public interface MyProxy extends Proxy<ResourceAdapterFinder>, Place {}


    public interface MyView extends View {
        void setPresenter(ResourceAdapterFinder presenter);
        void updateFrom(List<ResourceAdapter> list);
        void setPreview(SafeHtml html);
    }



    @Inject
    public ResourceAdapterFinder(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
                                 DispatchAsync dispatcher, RevealStrategy revealStrategy, ApplicationMetaData metaData,
                                 BeanFactory beanFactory) {

        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.beanFactory = beanFactory;
        this.beanMetaData = metaData.getBeanMetaData(ResourceAdapter.class);
        this.adapter = new EntityAdapter<ResourceAdapter>(ResourceAdapter.class, metaData);


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

                List<Property> children = result.get(RESULT).asPropertyList();
                List<ResourceAdapter> resourceAdapters = new ArrayList<ResourceAdapter>(children.size());

                for (Property child : children) {
                    ModelNode raModel = child.getValue();

                    ResourceAdapter resourceAdapter = adapter.fromDMR(raModel);
                    // The unique identifier of a resource adapter is its name (not the archive name)
                    resourceAdapter.setName(child.getName());

                    // sub resources are not loaded
                    resourceAdapter.setProperties(Collections.<PropertyRecord>emptyList());
                    resourceAdapter.setConnectionDefinitions(new ArrayList<ConnectionDefinition>());
                    resourceAdapter.setAdminObjects(Collections.<AdminObject>emptyList());

                    resourceAdapters.add(resourceAdapter);
                }

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
        window = new DefaultWindow(Console.MESSAGES.createTitle("Resource Adapter"));
        window.setWidth(480);
        window.setHeight(360);

        window.trapWidget(
                new NewAdapterWizard(this, beanFactory.resourceAdapter().as()).asWidget()
        );

        window.setGlassEnabled(true);
        window.center();
    }

    public void closeDialoge() {
        window.hide();
    }

    public void onCreateAdapter(final ResourceAdapter ra) {
        closeDialoge();

        ModelNode addressModel = beanMetaData.getAddress().asResource(Baseadress.get(), ra.getName());

        ModelNode operation = adapter.fromEntity(ra);
        operation.get(OP).set(ADD);
        operation.get(ADDRESS).set(addressModel.get(ADDRESS).asObject());

        operation.remove("name"); // work around

        System.out.println(operation);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {
                Log.error("Adding resource adapter failed: " + caught.getMessage(), caught);
                Console.error(Console.MESSAGES.addingFailed("Resource Adapter " + ra.getName()), caught.getMessage());
                loadAdapter();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();
                if (ModelNodeUtil.indicatesSuccess(result)) {
                    Console.info(Console.MESSAGES.added("Resource Adapter " + ra.getName()));
                } else {
                    Console.error(Console.MESSAGES.addingFailed("Resource Adapter " + ra.getName()), result.toString());
                }
                loadAdapter();
            }
        });

    }

    public void onDelete(final ResourceAdapter ra) {

        AddressBinding address = beanMetaData.getAddress();
        ModelNode operation = address.asResource(Baseadress.get(), ra.getName());
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

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
