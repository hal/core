/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.events.StaleModelEvent;
import org.jboss.as.console.client.domain.groups.CopyGroupWizard;
import org.jboss.as.console.client.domain.groups.NewServerGroupWizard;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.ProfileStore;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.as.console.client.domain.topology.LifecycleCallback;
import org.jboss.as.console.client.domain.topology.ServerGroup;
import org.jboss.as.console.client.domain.topology.ServerGroupOp;
import org.jboss.as.console.client.domain.topology.ServerGroupOpV3;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.properties.CreatePropertyCmd;
import org.jboss.as.console.client.shared.properties.DeletePropertyCmd;
import org.jboss.as.console.client.shared.properties.NewPropertyWizard;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.shared.util.DMRUtil;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshHosts;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.PropagatesChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 */
public class HostMgmtPresenter extends PerspectivePresenter<HostMgmtPresenter.MyView, HostMgmtPresenter.MyProxy>
        implements PropertyManagement, PreviewEvent.Handler {


    private DefaultWindow window;
    private DefaultWindow propertyWindow;
    private DispatchAsync dispatcher;
    private BeanFactory factory;
    private ApplicationMetaData propertyMetaData;

    @ProxyCodeSplit
    @NameToken(NameTokens.HostMgmtPresenter)
    @AccessControl(resources = {
            "/server-group=*",
            "opt://server-group={selected.entity}/system-property=*"},
            recursive = false)
    @SearchIndex(keywords = {"host", "jvm", "group", "server-group", "profile", "socket-binding"})
    public interface MyProxy extends Proxy<HostMgmtPresenter>, Place {
    }


    public interface MyView extends View {
        void setPresenter(HostMgmtPresenter presenter);
        void updateHosts(String selectedHost, Set<String> hostNames);
        void updateProfiles(List<ProfileRecord> result);
        void updateSocketBindings(List<String> result);
        void setServerGroups(List<ServerGroupRecord> result);

        void preview(SafeHtml html);
    }


    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();

    private final Dispatcher circuit;
    private final ServerStore serverStore;
    private final ServerGroupStore serverGroupStore;
    private final ProfileStore profileStore;
    private BootstrapContext bootstrap;
    private final HostStore hostStore;
    private HandlerRegistration hostHandler;
    private List<ProfileRecord> existingProfiles;
    private List<String> existingSockets;

    @Inject
    public HostMgmtPresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
                             BootstrapContext bootstrap, Header header, HostStore hostStore, Dispatcher circuit,
                             UnauthorisedPresenter unauthorisedPresenter,  ServerStore serverStore, ServerGroupStore serverGroupStore,
                             ProfileStore profileStore, DispatchAsync dispatcher, BeanFactory factory,
                             ApplicationMetaData propertyMetaData) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.HostMgmtPresenter, unauthorisedPresenter,
                TYPE_MainContent);

        this.bootstrap = bootstrap;
        this.hostStore = hostStore;
        this.circuit = circuit;
        this.serverStore = serverStore;
        this.serverGroupStore = serverGroupStore;
        this.profileStore = profileStore;
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.propertyMetaData = propertyMetaData;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(PreviewEvent.TYPE, this);

        hostHandler = hostStore.addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Action action) {

                if (!isVisible()) return;

                getView().updateHosts(hostStore.getSelectedHost(), hostStore.getHostNames());

            }
        });

        // switching between host/group views
        serverStore.addChangeHandler(FilterType.class, new PropagatesChange.Handler() {
            @Override
            public void onChange(Action action) {
                if (!isVisible()) return;

                if(serverStore.getFilter().equals(FilterType.HOST))
                    getView().updateHosts(hostStore.getSelectedHost(), hostStore.getHostNames());
                else
                    loadServerGroupData();
            }
        });

    }

    @Override
    protected void onUnbind() {
        super.onUnbind();
        if (hostHandler != null) {
            hostHandler.removeHandler();
        }
    }

    @Override
    protected void onReset() {
        clearInitialPlace();
        HostMgmtPresenter.super.onReset();
    }

    // TODO: should be moved to circuit
    private void loadServerGroupData() {
        profileStore.loadProfiles(new SimpleCallback<List<ProfileRecord>>() {
            @Override
            public void onSuccess(List<ProfileRecord> result) {
                existingProfiles = result;
                getView().updateProfiles(result);
            }
        });

        serverGroupStore.loadSocketBindingGroupNames(new SimpleCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> result) {
                existingSockets = result;

                getView().updateSocketBindings(result);
            }
        });

        loadServerGroups();
    }

    private void loadServerGroups() {

        serverGroupStore.loadServerGroups(new SimpleCallback<List<ServerGroupRecord>>() {
            @Override
            public void onSuccess(List<ServerGroupRecord> result) {

                getView().setServerGroups(result);
            }
        });
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, PlaceManager placeManager, boolean revealDefault) {
        circuit.dispatch(new RefreshHosts());
        loadServerGroupData(); // move to circuit store
    }

    private void clearInitialPlace() {
        if (bootstrap.getInitialPlace() != null) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    /*Console.getEventBus().fireEvent(
                            new LHSHighlightEvent(bootstrap.getInitialPlace())
                    );*/
                    bootstrap.setInitialPlace(null);
                }
            });
        }
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    // -- server groups management  --

    private void staleModel() {
        fireEvent(new StaleModelEvent(StaleModelEvent.SERVER_GROUPS));
    }

    public void onDeleteGroup(final ServerGroupRecord group) {

        serverGroupStore.delete(group, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean wasSuccessful) {
                if (wasSuccessful) {
                    Console.info(Console.MESSAGES.deleted(group.getName()));
                } else {
                    Console.error(Console.MESSAGES.deletionFailed(group.getName()));
                }

                staleModel();

                loadServerGroups();
            }
        });
    }

    public void createNewGroup(final ServerGroupRecord newGroup) {

        closeDialoge();

        serverGroupStore.create(newGroup, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {

                if (success) {

                    Console.info(Console.MESSAGES.added(newGroup.getName()));
                    loadServerGroups();

                } else {
                    Console.error(Console.MESSAGES.addingFailed(newGroup.getName()));
                }

                staleModel();

            }
        });
    }

    public void launchNewGroupDialog() {

        window = new DefaultWindow(Console.MESSAGES.createTitle("Server Group"));
        window.setWidth(480);
        window.setHeight(360);

        window.trapWidget(
                new NewServerGroupWizard(this, existingProfiles, existingSockets).asWidget()
        );

        window.setGlassEnabled(true);
        window.center();
    }

    public void closeDialoge()
    {
        if(window!=null) window.hide();
    }

    public void closePropertyDialoge() {
        propertyWindow.hide();
    }

    public void launchNewPropertyDialoge(String group) {

        propertyWindow = new DefaultWindow(Console.MESSAGES.createTitle("System Property"));
        propertyWindow.setWidth(480);
        propertyWindow.setHeight(360);

        propertyWindow.trapWidget(
                new NewPropertyWizard(this, group, true).asWidget()
        );

        propertyWindow.setGlassEnabled(true);
        propertyWindow.center();
    }

    public void onCreateProperty(final String groupName, final PropertyRecord prop)
    {
        if(propertyWindow!=null && propertyWindow.isShowing())
        {
            propertyWindow.hide();
        }

        ModelNode address = new ModelNode();
        address.add("server-group", groupName);
        address.add("system-property", prop.getKey());

        CreatePropertyCmd cmd = new CreatePropertyCmd(dispatcher, factory, address);
        cmd.execute(prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadServerGroups();
            }
        });
    }

    public void onDeleteProperty(final String groupName, final PropertyRecord prop)
    {
        ModelNode address = new ModelNode();
        address.add("server-group", groupName);
        address.add("system-property", prop.getKey());

        DeletePropertyCmd cmd = new DeletePropertyCmd(dispatcher,factory,address);
        cmd.execute(prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadServerGroups();
            }
        });
    }

    @Override
    public void onChangeProperty(String groupName, PropertyRecord prop) {
        // do nothing
    }

    public void launchCopyWizard(final ServerGroupRecord orig) {
        window = new DefaultWindow("New Server Group");
        window.setWidth(400);
        window.setHeight(320);

        window.trapWidget(
                new CopyGroupWizard(HostMgmtPresenter.this, orig).asWidget()
        );

        window.setGlassEnabled(true);
        window.center();
    }

    public void onSaveCopy(final ServerGroupRecord orig, final ServerGroupRecord newGroup) {
        window.hide();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).add("server-group", orig.getName());
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation, false), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to read server-group: " + orig.getName(), caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error("Failed to read server-group: " + orig.getName(), response.getFailureDescription());
                } else {
                    ModelNode model = response.get("result").asObject();
                    model.remove("name");

                    // re-create node

                    ModelNode compositeOp = new ModelNode();
                    compositeOp.get(OP).set(COMPOSITE);
                    compositeOp.get(ADDRESS).setEmptyList();

                    List<ModelNode> steps = new ArrayList<ModelNode>();

                    final ModelNode rootResourceOp = new ModelNode();
                    rootResourceOp.get(OP).set(ADD);
                    rootResourceOp.get(ADDRESS).add("server-group", newGroup.getName());

                    steps.add(rootResourceOp);

                    DMRUtil.copyResourceValues(model, rootResourceOp, steps);

                    compositeOp.get(STEPS).set(steps);

                    dispatcher.execute(new DMRAction(compositeOp), new SimpleCallback<DMRResponse>() {
                        @Override
                        public void onSuccess(DMRResponse dmrResponse) {
                            ModelNode response = dmrResponse.get();

                            if (response.isFailure()) {
                                Console.error("Failed to copy server-group", response.getFailureDescription());
                            } else {
                                Console.info("Successfully copied server-group '" + newGroup.getName() + "'");
                            }

                            loadServerGroups();
                        }
                    });

                }

            }

        });
    }

    @Override
    public void onPreview(PreviewEvent event) {
        getView().preview(event.getHtml());
    }

    public void onGroupLifecycle(final String group, final LifecycleOperation op) {

        ServerGroupOpV3 serverGroupOp = new ServerGroupOpV3(op, new LifecycleCallback() {
            @Override
            public void onSuccess() {
                circuit.dispatch(new RefreshServer());
            }

            @Override
            public void onTimeout() {
                Console.warning("Request timeout");
                circuit.dispatch(new RefreshServer());
            }

            @Override
            public void onAbort() {
                Console.warning("Request aborted.");
                circuit.dispatch(new RefreshServer());
            }

            @Override
            public void onError(Throwable caught) {
                Console.error("Server " + op.name() + " failed", caught.getMessage());
                circuit.dispatch(new RefreshServer());
            }
        }, dispatcher, serverGroupStore, group,  serverStore.getServerForGroup(group));

        serverGroupOp.run();

    }

}
