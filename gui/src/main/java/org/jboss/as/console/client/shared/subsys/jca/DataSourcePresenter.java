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

import static org.jboss.as.console.client.shared.subsys.jca.VerifyConnectionOp.VerifyResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableView;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.CurrentProfileSelection;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.model.ResponseWrapper;
import org.jboss.as.console.client.shared.properties.NewPropertyWizard;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.jca.functions.LoadDataSourcesFunction;
import org.jboss.as.console.client.shared.subsys.jca.functions.LoadXADataSourcesFunction;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceStore;
import org.jboss.as.console.client.shared.subsys.jca.model.DriverRegistry;
import org.jboss.as.console.client.shared.subsys.jca.model.DriverStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.shared.subsys.jca.wizard.NewDatasourceWizard;
import org.jboss.as.console.client.shared.subsys.jca.wizard.NewXADatasourceWizard;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

/**
 * @author Heiko Braun
 */
public class DataSourcePresenter extends Presenter<DataSourcePresenter.MyView, DataSourcePresenter.MyProxy>
        implements PropertyManagement {

    private final DispatchAsync dispatcher;
    private final BeanFactory beanFactory;
    private final CurrentProfileSelection currentProfileSelection;
    private boolean hasBeenRevealed = false;
    private DefaultWindow window;

    private DataSourceStore dataSourceStore;
    private DriverStrategy driverRegistry;
    private RevealStrategy revealStrategy;
    private ApplicationProperties bootstrap;
    private DefaultWindow propertyWindow;
    private List<DataSource> datasources;
    private List<JDBCDriver> drivers = Collections.EMPTY_LIST;
    private List<XADataSource> xaDatasources;

    @ProxyCodeSplit
    @NameToken(NameTokens.DataSourcePresenter)
    @AccessControl(resources = {
            "/{selected.profile}/subsystem=datasources/data-source=*",
            "/{selected.profile}/subsystem=datasources/xa-data-source=*"
    })
    public interface MyProxy extends Proxy<DataSourcePresenter>, Place {}

    public interface MyView extends SuspendableView {

        void setPresenter(DataSourcePresenter presenter);
        void updateDataSources(List<DataSource> datasources);
        void updateXADataSources(List<XADataSource> result);
        void enableDSDetails(boolean b);
        void enableXADetails(boolean b);
        void setPoolConfig(String name, PoolConfig poolConfig);
        void setXAPoolConfig(String dsName, PoolConfig underlying);
        void setXAProperties(String dataSourceName, List<PropertyRecord> result);
        void setConnectionProperties(String reference, List<PropertyRecord> properties);
        void showVerifyConncectionResult(final String name, VerifyResult result);
    }

    @Inject
    public DataSourcePresenter(EventBus eventBus, MyView view, MyProxy proxy, DataSourceStore dataSourceStore,
            DriverRegistry driverRegistry, RevealStrategy revealStrategy, ApplicationProperties bootstrap,
            DispatchAsync dispatcher, BeanFactory beanFactory, CurrentProfileSelection currentProfileSelection) {

        super(eventBus, view, proxy);
        this.dispatcher = dispatcher;
        this.beanFactory = beanFactory;
        this.currentProfileSelection = currentProfileSelection;

        this.dataSourceStore = new DataSourceStoreInterceptor(dataSourceStore);
        this.driverRegistry = driverRegistry.create();
        this.revealStrategy = revealStrategy;
        this.bootstrap = bootstrap;
        this.datasources = new ArrayList<DataSource>();
        this.xaDatasources = new ArrayList<XADataSource>();
        this.drivers = new ArrayList<JDBCDriver>();
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();

        Outcome<FunctionContext> resetOutcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                Console.error(Console.CONSTANTS.subsys_jca_datasource_error_load(), context.getErrorMessage());
                if (!hasBeenRevealed) {
                    hasBeenRevealed = true;
                }
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                // reading this kind of information is expensive, so cache the results until the next call to reset()
                datasources = context.get(LoadDataSourcesFunction.KEY);
                xaDatasources = context.get(LoadXADataSourcesFunction.KEY);

                getView().updateDataSources(datasources);
                getView().updateXADataSources(xaDatasources);

                if (!hasBeenRevealed) {
                    hasBeenRevealed = true;
                }

                // postpone driver auto detection. can be executed in the background

                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        loadDrivers();
                    }
                });
            }
        };

        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT)
                .waterfall(new FunctionContext(), resetOutcome, new LoadDataSourcesFunction(dataSourceStore),
                        new LoadXADataSourcesFunction(dataSourceStore));
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
                Console.warning("Failed to auto detect JDBC driver: " + caught.getMessage());
            }

            @Override
            public void onSuccess(final List<JDBCDriver> result) {
                DataSourcePresenter.this.drivers = result;
            }
        });
    }

    private void loadDataSources() {
        dataSourceStore.loadDataSources(new SimpleCallback<List<DataSource>>() {
            @Override
            public void onSuccess(List<DataSource> result) {
                datasources = result;
                getView().updateDataSources(datasources);
            }
        });
    }

    private void loadXADataSources() {
        dataSourceStore.loadXADataSources(new SimpleCallback<List<XADataSource>>() {

            @Override
            public void onSuccess(List<XADataSource> result) {
                xaDatasources = result;
                getView().updateXADataSources(xaDatasources);
            }
        });
    }

    public void launchNewDatasourceWizard() {

        window = new DefaultWindow(Console.MESSAGES.createTitle("Datasource"));
        window.setWidth(480);
        window.setHeight(450);
        window.setWidget(new NewDatasourceWizard(DataSourcePresenter.this, drivers, datasources, bootstrap)
                .asWidget());
        window.setGlassEnabled(true);
        window.center();

    }

    public void launchNewXADatasourceWizard() {

        window = new DefaultWindow(Console.MESSAGES.createTitle("XA Datasource"));
        window.setWidth(480);
        window.setHeight(450);
        window.setWidget(
                new NewXADatasourceWizard(DataSourcePresenter.this, drivers, xaDatasources, bootstrap)
                        .asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onCreateDatasource(final DataSource datasource) {
        window.hide();

        datasource.setEnabled(false);

        // TODO find a proper way to deal with this
        if("".equals(datasource.getUsername()))
            datasource.setUsername(null);
        if("".equals(datasource.getPassword()))
            datasource.setPassword(null);
        if("".equals(datasource.getSecurityDomain()))
            datasource.setSecurityDomain(null);

        dataSourceStore.createDataSource(datasource, new SimpleCallback<ResponseWrapper<Boolean>>() {

            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                if (result.getUnderlying()) {
                    Console.info(Console.MESSAGES.added("Datasource ")+ datasource.getName());
                    loadDataSources();
                }
                else
                    Console.error(Console.MESSAGES.addingFailed("Datasource " + datasource.getName()), result.getResponse().toString());
            }
        });

    }


    public void onDelete(final DataSource entity) {

        dataSourceStore.deleteDataSource(entity, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {

                if (success) {
                    Console.info(Console.MESSAGES.deleted("Datasource ") + entity.getName());
                } else {
                    Console.error(Console.MESSAGES.deletionFailed("Datasource ") + entity.getName());
                }

                loadDataSources();
            }
        });
    }

    public void onDisable(final DataSource entity, boolean doEnable) {
        dataSourceStore.enableDataSource(entity, doEnable, new SimpleCallback<ResponseWrapper<Boolean>>() {

            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                if (result.getUnderlying()) {
                    Console.info(Console.MESSAGES.modified("Datasource ") + entity.getName());
                } else {
                    Console.error(Console.MESSAGES.modificationFailed("Datasource ") + entity.getName(), result.getResponse().toString());
                }

                loadDataSources();
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

                    loadDataSources();
                }

            });
        }
    }

    public void onSaveXADetails(final String name, Map<String, Object> changedValues) {

        getView().enableXADetails(false);
        if(changedValues.size()>0)
        {
            dataSourceStore.updateXADataSource(name, changedValues, new SimpleCallback<ResponseWrapper<Boolean>> (){

                @Override
                public void onSuccess(ResponseWrapper<Boolean> response) {
                    if(response.getUnderlying())
                        Console.info(Console.MESSAGES.saved("XA Datasource "+name));
                    else
                        Console.error(Console.MESSAGES.saveFailed("XA Datasource " + name), response.getResponse().toString());

                    loadXADataSources();
                }
            });
        }
    }

    public void onCreateXADatasource(final XADataSource updatedEntity) {
        window.hide();

        updatedEntity.setEnabled(false);

        if("".equals(updatedEntity.getUsername()))
            updatedEntity.setUsername(null);
        if("".equals(updatedEntity.getPassword()))
            updatedEntity.setPassword(null);
        if("".equals(updatedEntity.getSecurityDomain()))
            updatedEntity.setSecurityDomain(null);

        dataSourceStore.createXADataSource(updatedEntity, new SimpleCallback<ResponseWrapper<Boolean>>() {
            @Override
            public void onSuccess(ResponseWrapper<Boolean> response) {
                if (response.getUnderlying())
                    Console.info(Console.MESSAGES.added("XA Datasource " + updatedEntity.getName()));
                else
                    Console.error(Console.MESSAGES.addingFailed("XA Datasource " + updatedEntity.getName()), response.getResponse().toString());

                loadXADataSources();
            }
        });
    }


    public void onDisableXA(final XADataSource entity, boolean doEnable) {
        dataSourceStore.enableXADataSource(entity, doEnable, new SimpleCallback<ResponseWrapper<Boolean>>()
        {

            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {

                if (result.getUnderlying()) {
                    Console.info(Console.MESSAGES.modified("Datasource " + entity.getName()));
                } else {
                    Console.error(Console.MESSAGES.modificationFailed("Datasource " + entity.getName()), result.getResponse().toString());
                }

                loadXADataSources();
            }
        });
    }

    public void onDeleteXA(final XADataSource entity) {
        dataSourceStore.deleteXADataSource(entity, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {

                if (success) {
                    Console.info(Console.MESSAGES.deleted("Datasource "+ entity.getName()));
                } else {
                    Console.error(Console.MESSAGES.deletionFailed("Datasource " + entity.getName()));
                }

                loadXADataSources();
            }
        });
    }

    public void loadPoolConfig(final boolean isXA, final String dsName) {

        dataSourceStore.loadPoolConfig(isXA, dsName,
                new SimpleCallback<ResponseWrapper<PoolConfig>>() {
                    @Override
                    public void onSuccess(ResponseWrapper<PoolConfig> result) {
                        if(isXA)
                            getView().setXAPoolConfig(dsName, result.getUnderlying());
                        else
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

    public void loadXAProperties(final String dataSourceName) {
        dataSourceStore.loadXAProperties(dataSourceName, new SimpleCallback<List<PropertyRecord>>()
        {
            @Override
            public void onSuccess(List<PropertyRecord> result) {
                getView().setXAProperties(dataSourceName, result);
            }
        });
    }


    public void onCreateXAProperty(final String reference, final PropertyRecord prop) {

        closePropertyDialoge();

        dataSourceStore.createXAConnectionProperty(reference, prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if (success)
                    Console.info(Console.MESSAGES.added("XA property " + prop.getKey()));
                else
                    Console.error(Console.MESSAGES.addingFailed("XA property " + prop.getKey()));

                loadXAProperties(reference);
            }
        });
    }

    public void onDeleteXAProperty(final String reference, final PropertyRecord prop) {
        dataSourceStore.deleteXAConnectionProperty(reference, prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if (success)
                    Console.info(Console.MESSAGES.deleted("XA property " + prop.getKey()));
                else
                    Console.error(Console.MESSAGES.deletionFailed("XA property " + prop.getKey()));

                loadXAProperties(reference);
            }
        });
    }

    public void launchNewXAPropertyDialoge(String reference) {
        propertyWindow = new DefaultWindow(Console.MESSAGES.createTitle("XA property"));
        propertyWindow.setWidth(480);
        propertyWindow.setHeight(360);
        propertyWindow.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {

            }
        });

        propertyWindow.trapWidget(
                new NewPropertyWizard(new PropertyManagement() {
                    @Override
                    public void onCreateProperty(String reference, PropertyRecord prop) {
                        onCreateXAProperty(reference, prop);
                    }

                    @Override
                    public void onDeleteProperty(String reference, PropertyRecord prop) {

                    }

                    @Override
                    public void onChangeProperty(String reference, PropertyRecord prop) {

                    }

                    @Override
                    public void launchNewPropertyDialoge(String reference) {

                    }

                    @Override
                    public void closePropertyDialoge() {
                        closeXAPropertyDialoge();
                    }
                }, reference).asWidget()
        );

        propertyWindow.setGlassEnabled(true);
        propertyWindow.center();
    }

    public void closeXAPropertyDialoge() {
        propertyWindow.hide();
    }

    public void verifyConnection(final DataSource dataSource, boolean xa, boolean existing) {
        VerifyConnectionOp vop = new VerifyConnectionOp(dataSourceStore, dispatcher, beanFactory,
                currentProfileSelection.getName());
        vop.execute(dataSource, xa, existing, new SimpleCallback<VerifyResult>() {
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

    public void onDoFlush(boolean isXA, String editedName) {
        dataSourceStore.doFlush(isXA, editedName, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if(success)
                    Console.info(Console.MESSAGES.successful("Flush Pool"));
                else
                    Console.error(Console.MESSAGES.failed("Flush Pool"));
            }
        });
    }


}
