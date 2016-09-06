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

package org.jboss.as.console.client.shared.subsys.jca;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableView;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.CurrentProfileSelection;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.model.ResponseWrapper;
import org.jboss.as.console.client.shared.properties.NewPropertyWizard;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceStore;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplates;
import org.jboss.as.console.client.shared.subsys.jca.model.DriverRegistry;
import org.jboss.as.console.client.shared.subsys.jca.model.DriverStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.dispatch.DispatchAsync;

import static org.jboss.as.console.client.shared.subsys.jca.VerifyConnectionOp.VerifyResult;

/**
 * @author Heiko Braun
 */
public class DataSourcePresenter extends Presenter<DataSourcePresenter.MyView, DataSourcePresenter.MyProxy>
        implements PropertyManagement {

    private final DispatchAsync dispatcher;
    private final BeanFactory beanFactory;
    private final CurrentProfileSelection currentProfileSelection;
    private final DataSourceTemplates dataSourceTemplates;
    private boolean hasBeenRevealed = false;
    private DefaultWindow window;

    private DataSourceStore dataSourceStore;
    private DriverStrategy driverRegistry;
    private RevealStrategy revealStrategy;
    private ApplicationProperties bootstrap;
    private DefaultWindow propertyWindow;
    private List<DataSource> datasources;
    private List<JDBCDriver> drivers;
    private String selectedDatasource;

    @ProxyCodeSplit
    @NameToken(NameTokens.DataSourcePresenter)
    @RequiredResources(resources = {
            "/{selected.profile}/subsystem=datasources/data-source=*"}
    )
    @SearchIndex(keywords = {"jpa", "data-source", "pool", "connection-properties", "jdbc"})
    public interface MyProxy extends Proxy<DataSourcePresenter>, Place {}


    public interface MyView extends SuspendableView {

        void setPresenter(DataSourcePresenter presenter);
        void updateDataSource(DataSource ds);
        void enableDSDetails(boolean b);
        void setPoolConfig(String name, PoolConfig poolConfig);
        void setConnectionProperties(String reference, List<PropertyRecord> properties);
        void showVerifyConncectionResult(final String name, VerifyResult result);
    }


    @Inject
    public DataSourcePresenter(EventBus eventBus, MyView view, MyProxy proxy, DataSourceStore dataSourceStore,
                               DriverRegistry driverRegistry, RevealStrategy revealStrategy, ApplicationProperties bootstrap,
                               DispatchAsync dispatcher, BeanFactory beanFactory, CurrentProfileSelection currentProfileSelection,
                               DataSourceTemplates dataSourceTemplates) {

        super(eventBus, view, proxy);
        this.dispatcher = dispatcher;
        this.beanFactory = beanFactory;
        this.currentProfileSelection = currentProfileSelection;
        this.dataSourceTemplates = dataSourceTemplates;

        this.dataSourceStore = dataSourceStore;
        this.driverRegistry = driverRegistry.create();
        this.revealStrategy = revealStrategy;
        this.bootstrap = bootstrap;
        this.datasources = new ArrayList<DataSource>();
        this.drivers = new ArrayList<JDBCDriver>();
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        super.prepareFromRequest(request);
        selectedDatasource = request.getParameter("name", null);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadDataSource();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    private void loadDrivers() {
        // Will start a nested async waterfall
        driverRegistry.refreshDrivers(new AsyncCallback<List<JDBCDriver>>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.warning(Console.MESSAGES.failedToDetectJdbcDriver(caught.getMessage()));
            }

            @Override
            public void onSuccess(final List<JDBCDriver> result) {
                DataSourcePresenter.this.drivers = result;
            }
        });
    }

    private void loadDataSource() {
        dataSourceStore.loadDataSource(selectedDatasource, false, new SimpleCallback<DataSource>() {
            @Override
            public void onSuccess(DataSource result) {

                getView().updateDataSource(result);
            }
        });
    }

    public void closeDialogue() {
        window.hide();
    }

    public void onSaveDSDetails(final String name, Map<String, Object> changedValues) {
        getView().enableDSDetails(false);
        if(changedValues.size()>0)
        {
            dataSourceStore.updateDataSource(name, changedValues, new SimpleCallback<ResponseWrapper<Boolean>> (){

                @Override
                public void onSuccess(ResponseWrapper<Boolean> response) {
                    if(response.getUnderlying())
                        Console.info(Console.MESSAGES.saved("Datasource "+name));
                    else
                        Console.error(Console.MESSAGES.saveFailed("Datasource ") + name, response.getResponse().toString());

                    loadDataSource();
                }

            });
        }
    }


    public void loadPoolConfig(final boolean isXA, final String dsName) {

        dataSourceStore.loadPoolConfig(isXA, dsName,
                new SimpleCallback<ResponseWrapper<PoolConfig>>() {
                    @Override
                    public void onSuccess(ResponseWrapper<PoolConfig> result) {
                        getView().setPoolConfig(dsName, result.getUnderlying());
                    }
                });
    }

    public void onSavePoolConfig(final String editedName, Map<String, Object> changeset, final boolean isXA) {
        dataSourceStore.savePoolConfig(isXA, editedName, changeset, new SimpleCallback<ResponseWrapper<Boolean>>(){
            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                if(result.getUnderlying())
                    Console.info(Console.MESSAGES.saved("Pool Settings "+editedName));
                else
                    Console.error(Console.MESSAGES.saveFailed("Pool Settings "+ editedName), result.getResponse().toString());

                loadPoolConfig(isXA, editedName);
            }
        });
    }

    public void onDeletePoolConfig(final String editedName, PoolConfig entity, final boolean isXA) {

        dataSourceStore.deletePoolConfig(isXA, editedName, new SimpleCallback<ResponseWrapper<Boolean>>(){
            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                if(result.getUnderlying())
                    Console.info(Console.MESSAGES.modified("pool setting "+editedName));
                else
                    Console.error(Console.MESSAGES.modificationFailed("pool setting " + editedName), result.getResponse().toString());

                loadPoolConfig(isXA, editedName);
            }
        });
    }

    public void verifyConnection(final DataSource dataSource) {
        VerifyConnectionOp vop = new VerifyConnectionOp(dataSourceStore, dispatcher, beanFactory,
                currentProfileSelection.getName());
        vop.execute(dataSource, false, true, new SimpleCallback<VerifyResult>() {
            @Override
            public void onSuccess(final VerifyResult result) {
                getView().showVerifyConncectionResult(dataSource.getName(), result);
            }
        });
    }

    public void onLoadConnectionProperties(final String datasourceName) {
        dataSourceStore.loadConnectionProperties(datasourceName, new SimpleCallback<List<PropertyRecord>>(){

            @Override
            public void onSuccess(List<PropertyRecord> propertyRecords) {
                getView().setConnectionProperties(datasourceName, propertyRecords);
            }
        });
    }

    @Override
    public void onCreateProperty(final String reference, final PropertyRecord prop) {

        closePropertyDialoge();

        dataSourceStore.createConnectionProperty(reference, prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if(success)
                    Console.info(Console.MESSAGES.added("Connection property "+prop.getKey()));
                else
                    Console.error(Console.MESSAGES.addingFailed("Connection property "+prop.getKey()));

                onLoadConnectionProperties(reference);
            }
        });
    }

    @Override
    public void onDeleteProperty(final String reference, final PropertyRecord prop) {
        dataSourceStore.deleteConnectionProperty(reference, prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if(success)
                    Console.info(Console.MESSAGES.deleted("Connection property "+prop.getKey()));
                else
                    Console.error(Console.MESSAGES.deletionFailed("Connection property "+prop.getKey()));

                onLoadConnectionProperties(reference);
            }
        });
    }

    @Override
    public void onChangeProperty(String reference, PropertyRecord prop) {
        // not possible
    }

    @Override
    public void launchNewPropertyDialoge(String reference) {
        propertyWindow = new DefaultWindow(Console.MESSAGES.createTitle("Connection Property"));
        propertyWindow.setWidth(480);
        propertyWindow.setHeight(360);

        propertyWindow.trapWidget(
                new NewPropertyWizard(this, reference, false).asWidget()
        );

        propertyWindow.setGlassEnabled(true);
        propertyWindow.center();
    }

    @Override
    public void closePropertyDialoge() {
        propertyWindow.hide();
    }

    public void onDoFlush(boolean isXA, String editedName, String flushOp) {
        dataSourceStore.doFlush(isXA, editedName, flushOp, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if(success)
                    Console.info(Console.MESSAGES.successful("Flush Pool"));
                else
                    Console.error(Console.MESSAGES.failed("Flush Pool"));
            }
        });
    }


    public void onDisable(final DataSource entity, boolean doEnable) {
        dataSourceStore.enableDataSource(entity, doEnable, new SimpleCallback<ResponseWrapper<Boolean>>() {

            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                boolean enabled = entity.isEnabled();
                String text = enabled ? Console.MESSAGES.successDisabled("Datasource " + entity.getName()) :Console.MESSAGES.successEnabled("Datasource " + entity.getName()) ;
                if (result.getUnderlying()) {
                    Console.info(text);
                } else {
                    Console.error(Console.MESSAGES.modificationFailed("Datasource ") + entity.getName(), result.getResponse().toString());
                }

                loadDataSource();

                getEventBus().fireEvent(new RefreshDSFinderEvent(false, selectedDatasource));
            }
        });
    }
}
