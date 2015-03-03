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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.MultiView;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.general.model.LoadSocketBindingsCmd;
import org.jboss.as.console.client.shared.jvm.CreateJvmCmd;
import org.jboss.as.console.client.shared.jvm.DeleteJvmCmd;
import org.jboss.as.console.client.shared.jvm.Jvm;
import org.jboss.as.console.client.shared.jvm.JvmManagement;
import org.jboss.as.console.client.shared.jvm.UpdateJvmCmd;
import org.jboss.as.console.client.shared.properties.CreatePropertyCmd;
import org.jboss.as.console.client.shared.properties.DeletePropertyCmd;
import org.jboss.as.console.client.shared.properties.NewPropertyWizard;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerRef;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.AddServer;
import org.jboss.as.console.client.v3.stores.domain.actions.CopyServer;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.as.console.client.v3.stores.domain.actions.RemoveServer;
import org.jboss.as.console.client.v3.stores.domain.actions.UpdateServer;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.nav.v3.CloseApplicationEvent;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 3/3/11
 *
 * IA - refactoring remaining issues:
 *
 * + jvm settings
 * + system properties
 * - server status preview
 * ? start/stop server
 *
 */
public class ServerConfigPresenter extends CircuitPresenter<ServerConfigPresenter.MyView, ServerConfigPresenter.MyProxy>
        implements JvmManagement, PropertyManagement {


    @ProxyCodeSplit
    @NameToken(NameTokens.ServerPresenter)
    @OperationMode(DOMAIN)
    @AccessControl(resources = {
            "/{selected.host}/server-config=*",
            "opt://{selected.host}/server-config=*/system-property=*"},
            recursive = false)
    @SearchIndex(keywords = {"server", "server-config", "jvm", "socket-binding"})
    public interface MyProxy extends Proxy<ServerConfigPresenter>, Place {}


    public interface MyView extends MultiView {
        void setPresenter(ServerConfigPresenter presenter);
        void updateSocketBindings(List<String> result);
        void setJvm(String reference, Jvm jvm);
        void setProperties(String reference, List<PropertyRecord> properties);
        void setGroups(List<ServerGroupRecord> result);
        void updateFrom(Server server);
        void setHosts(Set<String> hostNames, String selectedHost);

        void setSelectedServer(Server selectServer);
    }


    private final ServerStore serverStore;
    private final Dispatcher circuit;
    private HostInformationStore hostInfoStore;
    private ServerGroupStore serverGroupStore;

    private DefaultWindow propertyWindow;
    private DispatchAsync dispatcher;
    private ApplicationMetaData propertyMetaData;
    private BeanFactory factory;
    private PlaceManager placeManager;


    private final HostStore hostStore;


    @Inject
    public ServerConfigPresenter(EventBus eventBus, MyView view, MyProxy proxy,
                                 HostInformationStore hostInfoStore, ServerGroupStore serverGroupStore,
                                 DispatchAsync dispatcher, ApplicationMetaData propertyMetaData,
                                 BeanFactory factory, PlaceManager placeManager, HostStore hostStore,
                                 ServerStore serverStore, Dispatcher circuit) {

        super(eventBus, view, proxy, circuit);

        this.hostInfoStore = hostInfoStore;
        this.serverGroupStore = serverGroupStore;
        this.dispatcher = dispatcher;
        this.propertyMetaData = propertyMetaData;
        this.factory = factory;
        this.placeManager = placeManager;
        this.serverStore = serverStore;
        this.hostStore = hostStore;

        this.circuit = circuit;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(ServerWizardEvent.TYPE, this);

        addChangeHandler(hostStore);
        addChangeHandler(serverStore);
    }

    @Override
    protected void onAction(Action action) {

        if(!isVisible()) return; // don't process anything when not visible

        // TODO (hbraun):  I think this whole section is not needed anymore

      /*  if(action instanceof SelectServer)
        {
            SelectServer serverSelection = (SelectServer) action;
            List<Server> serverModel = serverStore.getServerForHost(serverSelection.getHost());
            for (Server server : serverModel) {
                if(server.getHostName().equals(serverSelection.getHost())
                        && server.getName().equals(serverSelection.getServer()))
                {
                    getView().updateFrom(server);
                    break;
                }
            }
        }

        // Refresh the server list when:
        // - changes to host/group filter refresh the server list
        // - group and host selection events
        // - server's are added or removed
        else if(
                (action instanceof FilterType)
                        || (action instanceof GroupSelection)
                        || (action instanceof HostSelection)
                        || (action instanceof RemoveServer)
                        || (action instanceof AddServer)
                ) {

            if(FilterType.HOST.equals(serverStore.getFilter()))
            {
                String selectedHost = hostStore.getSelectedHost();

                List<Server> serverModel = Collections.EMPTY_LIST;
                if (selectedHost != null) {
                    serverModel = serverStore.getServerForHost(
                            hostStore.getSelectedHost()
                    );

                }

                getView().updateServerList(serverModel);
            }
            else if(FilterType.GROUP.equals(serverStore.getFilter()))
            {
                List<Server> serverModel = serverStore.getServerForGroup(serverStore.getSelectedGroup());
                getView().updateServerList(serverModel);
            }
        }*/
    }

    @Override
    protected void onReset() {
        super.onReset();

        loadSocketBindings(new Command() {
            @Override
            public void execute() {

                if(serverStore.getSelectServer()!=null) {
                    getView().updateFrom(serverStore.findServer(serverStore.getSelectServer()));
                    getView().setSelectedServer(serverStore.findServer(serverStore.getSelectServer()));
                }

                getView().setHosts(hostStore.getHostNames(), hostStore.getSelectedHost());

                getView().toggle(
                        placeManager.getCurrentPlaceRequest().getParameter("action", "none")
                );
            }
        });
    }

    public void onServerConfigSelectionChanged(final Server server) {
        if (server != null) {
            loadJVMConfiguration(server);
            loadProperties(server);

            SecurityContextChangedEvent.fire(this, "/{selected.host}/server-config=*", server.getName());
        }
    }

    private void loadSocketBindings(final Command cmd) {
        serverGroupStore.loadSocketBindingGroupNames(new SimpleCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> result) {
                getView().updateSocketBindings(result);
                loadServerGroups(cmd);
            }
        });
    }

    private void loadServerGroups(final Command cmd) {
        serverGroupStore.loadServerGroups(new SimpleCallback<List<ServerGroupRecord>>() {
            @Override
            public void onSuccess(List<ServerGroupRecord> serverGroups) {
                getView().setGroups(serverGroups);
                getView().setHosts(hostStore.getHostNames(), hostStore.getSelectedHost());
                cmd.execute();
            }
        });
    }


    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    public void closeApplicationView() {
        CloseApplicationEvent.fire(this);
    }

    public void onCreateServerConfig(final Server newServer) {

        circuit.dispatch(new AddServer(newServer));
        closeApplicationView();
    }

    public void onSaveChanges(final Server entity, Map<String, Object> changedValues) {
        circuit.dispatch(new UpdateServer(entity, changedValues));
    }


    public void tryDelete(final ServerRef server) {

        closeApplicationView();

        // check if instance exist
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).add("host", server.getHostName());
        operation.get(ADDRESS).add("server-config", server.getServerName());
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP).set(READ_RESOURCE_OPERATION);

        //System.out.println(operation);
        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable throwable) {
                Console.error("Failed to delete server", throwable.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                String outcome = response.get(OUTCOME).asString();

                Boolean serverIsRunning = Boolean.FALSE;

                // 2.0.x
                if (outcome.equals(SUCCESS)) {
                    serverIsRunning = response.get(RESULT).get("status").asString().equalsIgnoreCase("started");
                }

                if (!serverIsRunning)
                {
                    performDeleteOperation(server);
                }
                else
                {
                    Console.error(
                            Console.MESSAGES.deletionFailed("Server Configuration"),
                            Console.MESSAGES.server_config_stillRunning(server.getServerName())
                    );
                }
            }
        });


    }

    private void performDeleteOperation(final ServerRef server) {

        circuit.dispatch(new RemoveServer(server));
    }

    public String getSelectedHost() {
        return hostStore.getSelectedHost();
    }


    @Override
    public void onCreateJvm(String reference, Jvm jvm) {
        ModelNode address = new ModelNode();
        address.add("host", serverStore.getSelectServer().getHostName());
        address.add("server-config", reference);
        address.add(JVM, jvm.getName());
        final String selectedConfigName = reference;

        CreateJvmCmd cmd = new CreateJvmCmd(dispatcher, factory, address);
        cmd.execute(jvm, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                circuit.dispatch(new RefreshServer());
            }
        });
    }

    @Override
    public void onDeleteJvm(String reference, Jvm jvm) {

        ModelNode address = new ModelNode();
        address.add("host", serverStore.getSelectServer().getHostName());
        address.add("server-config", reference);
        address.add(JVM, jvm.getName());
        final String selectedConfigName = reference;

        DeleteJvmCmd cmd = new DeleteJvmCmd(dispatcher, factory, address);
        cmd.execute(new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                circuit.dispatch(new RefreshServer());
            }
        });

    }

    @Override
    public void onUpdateJvm(String reference, String jvmName, Map<String, Object> changedValues) {

        if (changedValues.size() > 0) {
            ModelNode address = new ModelNode();
            address.add("host", serverStore.getSelectServer().getHostName());
            address.add("server-config", reference);
            address.add(JVM, jvmName);
            final String selectedConfigName = reference;

            UpdateJvmCmd cmd = new UpdateJvmCmd(dispatcher, factory, propertyMetaData, address);
            cmd.execute(changedValues, new SimpleCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    circuit.dispatch(new RefreshServer());
                }
            });
        }
    }

    @Override
    public void onCreateProperty(String reference, final PropertyRecord prop) {
        if (propertyWindow != null && propertyWindow.isShowing()) {
            propertyWindow.hide();
        }

        ModelNode address = new ModelNode();
        address.add("host", serverStore.getSelectServer().getHostName());
        address.add("server-config", reference);
        address.add("system-property", prop.getKey());
        final String selectedConfigName = reference;

        CreatePropertyCmd cmd = new CreatePropertyCmd(dispatcher, factory, address);
        cmd.execute(prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                circuit.dispatch(new RefreshServer());
            }
        });
    }

    @Override
    public void onDeleteProperty(String reference, final PropertyRecord prop) {

        ModelNode address = new ModelNode();
        address.add("host", serverStore.getSelectServer().getHostName());
        address.add("server-config", reference);
        address.add("system-property", prop.getKey());
        final String selectedConfigName = reference;

        DeletePropertyCmd cmd = new DeletePropertyCmd(dispatcher, factory, address);
        cmd.execute(prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                circuit.dispatch(new RefreshServer());
            }
        });
    }

    @Override
    public void onChangeProperty(String reference, PropertyRecord prop) {
        // do nothing
    }

    @Override
    public void launchNewPropertyDialoge(String reference) {
        propertyWindow = new DefaultWindow(Console.MESSAGES.createTitle("System Property"));
        propertyWindow.setWidth(480);
        propertyWindow.setHeight(360);

        propertyWindow.trapWidget(
                new NewPropertyWizard(this, reference, true).asWidget()
        );

        propertyWindow.setGlassEnabled(true);
        propertyWindow.center();
    }

    @Override
    public void closePropertyDialoge() {
        propertyWindow.hide();
    }

    public void loadJVMConfiguration(final Server server) {
        hostInfoStore.loadJVMConfiguration(server.getHostName(), server, new SimpleCallback<Jvm>() {
            @Override
            public void onSuccess(Jvm jvm) {
                getView().setJvm(server.getName(), jvm);
            }
        });
    }

    public void loadProperties(final Server server) {
        hostInfoStore
                .loadProperties(server.getHostName(), server, new SimpleCallback<List<PropertyRecord>>() {
                    @Override
                    public void onSuccess(List<PropertyRecord> properties) {
                        getView().setProperties(server.getName(), properties);
                    }
                });
    }

    public void onSaveCopy(final String targetHost, final Server original, final Server newServer) {
        closeApplicationView();
        circuit.dispatch(new CopyServer(targetHost, original, newServer));

    }

    public String getFilter() {
        return serverStore.getFilter();
    }

    public String getSelectedGroup() {
        return serverStore.getSelectedGroup();
    }

    public ServerRef getSelectedServer() {
            return serverStore.getSelectServer();
        }

}