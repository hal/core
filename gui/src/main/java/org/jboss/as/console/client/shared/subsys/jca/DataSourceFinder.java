package org.jboss.as.console.client.shared.subsys.jca;

import com.google.common.collect.ImmutableMap;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.CurrentProfileSelection;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.model.ResponseWrapper;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.jca.functions.LoadDataSourcesFunction;
import org.jboss.as.console.client.shared.subsys.jca.functions.LoadXADataSourcesFunction;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceStore;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplates;
import org.jboss.as.console.client.shared.subsys.jca.model.DriverRegistry;
import org.jboss.as.console.client.shared.subsys.jca.model.DriverStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.shared.subsys.jca.wizard.NewDatasourceWizard;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.v3.widgets.wizard.Wizard;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

import java.util.Collections;
import java.util.List;

import static org.jboss.ballroom.client.widgets.forms.FormItem.VALUE_SEMANTICS.UNDEFINED;

/**
 * @author Heiko Braun
 */
public class DataSourceFinder extends Presenter<DataSourceFinder.MyView, DataSourceFinder.MyProxy>
        implements PreviewEvent.Handler, RefreshDSFinderEvent.RefreshHandler {


    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final DataSourceStore dataSourceStore;
    private final DataSourceTemplates dataSourceTemplates;
    private final BeanFactory beanFactory;
    private final DriverStrategy driverRegistry;
    private final BootstrapContext bootstrap;
    private final CurrentProfileSelection currentProfileSelection;

    // cached data
    List<XADataSource> xaDatasources = Collections.EMPTY_LIST;
    List<DataSource> datasources = Collections.EMPTY_LIST;
    private List<JDBCDriver> drivers = Collections.EMPTY_LIST;

    private DefaultWindow window;


    @ProxyCodeSplit
    @NameToken(NameTokens.DataSourceFinder)
    @RequiredResources(resources = {
            "/{selected.profile}/subsystem=datasources/data-source=*",
            "/{selected.profile}/subsystem=datasources/xa-data-source=*"})
    @SearchIndex(keywords = {"jpa", "data-source", "pool", "connection-properties", "jdbc", "xa-data-source"})
    public interface MyProxy extends Proxy<DataSourceFinder>, Place {}


    public interface MyView extends View {

        void setPresenter(DataSourceFinder presenter);

        void setPreview(SafeHtml html);

        void updateFrom(List<DataSource> list);

        void updateDataSources(List<DataSource> datasources);

        void updateXADataSources(List<XADataSource> xaDatasources);

        void showVerifyConncectionResult(String name, VerifyConnectionOp.VerifyResult result);

        void resetFirstColumn();
    }


    @Inject
    public DataSourceFinder(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            DispatchAsync dispatcher, RevealStrategy revealStrategy, DataSourceStore dataSourceStore,
            DataSourceTemplates dataSourceTemplates, BeanFactory beanFactory,
            DriverRegistry driverRegistry, BootstrapContext bootstrap,
            CurrentProfileSelection currentProfileSelection) {

        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.dataSourceStore = dataSourceStore;
        this.dataSourceTemplates = dataSourceTemplates;
        this.beanFactory = beanFactory;
        this.driverRegistry = driverRegistry.create();
        this.bootstrap = bootstrap;
        this.currentProfileSelection = currentProfileSelection;
    }

    @Override
    protected void onReset() {
        PlaceRequest currentPlaceRequest = placeManager.getCurrentPlaceRequest();
        if (currentPlaceRequest.matchesNameToken(getProxy().getNameToken())
                && !currentPlaceRequest.getParameterNames().contains("backButton")) { // keep selection of type column, if user us returning from DS view
            getView().resetFirstColumn();
            loadDatasources();
            loadXADatasources();
        }
    }

    @Override
    protected void onBind() {
        super.onBind();
        getEventBus().addHandler(PreviewEvent.TYPE, this);
        getEventBus().addHandler(RefreshDSFinderEvent.TYPE, this);
        getView().setPresenter(this);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    @Override
    public void onRefresh(String dsName, boolean isXa) {
        if (isXa) { loadXADatasources(); } else { loadDatasources(); }
    }

    public void loadDatasources() {
        Outcome<FunctionContext> resetOutcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                Console.error(Console.CONSTANTS.subsys_jca_datasource_error_load(), context.getErrorMessage());

            }

            @Override
            public void onSuccess(final FunctionContext context) {
                datasources = context.get(LoadDataSourcesFunction.KEY);
                getView().updateDataSources(datasources);

            }
        };

        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT)
                .waterfall(new FunctionContext(), resetOutcome, new LoadDataSourcesFunction(dataSourceStore),
                        new LoadXADataSourcesFunction(dataSourceStore));

        // postpone driver auto detection. can be executed in the background
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                loadDrivers();
            }
        });

    }

    public void loadXADatasources() {
        Outcome<FunctionContext> resetOutcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                Console.error(Console.CONSTANTS.subsys_jca_datasource_error_load(), context.getErrorMessage());

            }

            @Override
            public void onSuccess(final FunctionContext context) {

                xaDatasources = context.get(LoadXADataSourcesFunction.KEY);
                getView().updateXADataSources(xaDatasources);

            }
        };

        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT)
                .waterfall(new FunctionContext(), resetOutcome, new LoadDataSourcesFunction(dataSourceStore),
                        new LoadXADataSourcesFunction(dataSourceStore));
    }

    @Override
    protected void revealInParent() {
        if (Console.getBootstrapContext().isStandalone()) {
            RevealContentEvent.fire(this, ServerMgmtApplicationPresenter.TYPE_MainContent, this);
        } else { RevealContentEvent.fire(this, ProfileMgmtPresenter.TYPE_MainContent, this); }
    }

    public void closeDialoge() {
        window.hide();
    }

    @Override
    public void onPreview(PreviewEvent event) {
        if (isVisible()) { getView().setPreview(event.getHtml()); }
    }

    public void launchNewDatasourceWizard() {
        new NewDatasourceWizard<>(this, dataSourceStore, bootstrap, beanFactory, dataSourceTemplates, datasources,
                drivers, false)
                .open(Console.MESSAGES.createTitle("Datasource"), Wizard.DEFAULT_WIDTH, Wizard.DEFAULT_HEIGHT + 100);
    }

    public void launchNewXADatasourceWizard() {
        new NewDatasourceWizard<>(this, dataSourceStore, bootstrap, beanFactory, dataSourceTemplates, datasources,
                drivers, true)
                .open(Console.MESSAGES.createTitle("XA Datasource"), Wizard.DEFAULT_WIDTH, Wizard.DEFAULT_HEIGHT + 100);
    }

    private void loadDrivers() {
        // Will start a nested async waterfall
        driverRegistry.refreshDrivers(new AsyncCallback<List<JDBCDriver>>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.warning(
                        Console.MESSAGES.failedToDetectJdbcDriver(caught.getMessage()));
            }

            @Override
            public void onSuccess(final List<JDBCDriver> result) {
                DataSourceFinder.this.drivers = result;
            }
        });
    }

    public void onCreateDatasource(final DataSource datasource, final boolean dataSourceCreatedByTest) {
        SimpleCallback<ResponseWrapper<Boolean>> callback = new SimpleCallback<ResponseWrapper<Boolean>>() {
            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                if (result.getUnderlying()) {
                    Console.info(Console.MESSAGES.added("Datasource ") + datasource.getName());
                    loadDatasources();
                } else {
                    Console.error(Console.MESSAGES.addingFailed("Datasource " + datasource.getName()),
                            result.getResponse().toString());
                }
            }
        };

        if (!dataSourceCreatedByTest) {
            datasource.setEnabled(true); // HAL-617 / WFLY-4750
            datasource.setCcm(true); // HAL-397
            datasource.setJta(true); // HAL-397

            // TODO find a proper way to deal with this
            if ("".equals(datasource.getUsername())) { datasource.setUsername(null); }
            if ("".equals(datasource.getPassword())) { datasource.setPassword(null); }
            if ("".equals(datasource.getSecurityDomain())) { datasource.setSecurityDomain(null); }

            dataSourceStore.createDataSource(datasource, callback);

        } else {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                    .put("enabled", true)
                    .put("ccm", true)
                    .put("jta", true);

            if ("".equals(datasource.getUsername())) { builder.put("username", UNDEFINED); }
            if ("".equals(datasource.getPassword())) { builder.put("password", UNDEFINED); }
            if ("".equals(datasource.getSecurityDomain())) { builder.put("securityDomain", UNDEFINED); }

            dataSourceStore.updateDataSource(datasource.getName(), builder.build(), callback);
        }
    }

    public void onCreateXADatasource(final XADataSource updatedEntity, final boolean dataSourceCreatedByTest) {
        SimpleCallback<ResponseWrapper<Boolean>> callback = new SimpleCallback<ResponseWrapper<Boolean>>() {
            @Override
            public void onSuccess(ResponseWrapper<Boolean> response) {
                if (response.getUnderlying()) {
                    Console.info(Console.MESSAGES.added("XA Datasource " + updatedEntity.getName()));
                } else {
                    Console.error(Console.MESSAGES.addingFailed("XA Datasource " + updatedEntity.getName()),
                            response.getResponse().toString());
                }

                loadXADatasources();
            }
        };

        if (!dataSourceCreatedByTest) {
            updatedEntity.setEnabled(true); // HAL-617 / WFLY-4750
            updatedEntity.setCcm(true);

            if ("".equals(updatedEntity.getUsername())) { updatedEntity.setUsername(null); }
            if ("".equals(updatedEntity.getPassword())) { updatedEntity.setPassword(null); }
            if ("".equals(updatedEntity.getSecurityDomain())) { updatedEntity.setSecurityDomain(null); }

            dataSourceStore.createXADataSource(updatedEntity, callback);

        } else {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                    .put("enabled", true)
                    .put("ccm", true);

            if ("".equals(updatedEntity.getUsername())) { builder.put("userName", UNDEFINED); }
            if ("".equals(updatedEntity.getPassword())) { builder.put("password", UNDEFINED); }
            if ("".equals(updatedEntity.getSecurityDomain())) { builder.put("securityDomain", UNDEFINED); }

            dataSourceStore.updateXADataSource(updatedEntity.getName(), builder.build(), callback);
        }
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

                loadDatasources();
            }
        });
    }

    public void onDeleteXA(final XADataSource entity) {
        dataSourceStore.deleteXADataSource(entity, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {

                if (success) {
                    Console.info(Console.MESSAGES.deleted("XA Datasource " + entity.getName()));
                } else {
                    Console.error(Console.MESSAGES.deletionFailed("XA Datasource " + entity.getName()));
                }

                loadXADatasources();
            }
        });
    }


    public void onDisable(final DataSource entity, boolean doEnable) {
        dataSourceStore.enableDataSource(entity, doEnable, new SimpleCallback<ResponseWrapper<Boolean>>() {

            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                boolean enabled = entity.isEnabled();
                String text = enabled ? Console.MESSAGES
                        .successDisabled("Datasource " + entity.getName()) : Console.MESSAGES
                        .successEnabled("Datasource " + entity.getName());
                if (result.getUnderlying()) {
                    Console.info(text);
                } else {
                    Console.error(Console.MESSAGES.modificationFailed("Datasource ") + entity.getName(),
                            result.getResponse().toString());
                }

                loadDatasources();
            }
        });
    }

    public void onDisableXA(final XADataSource entity, boolean doEnable) {
        dataSourceStore.enableXADataSource(entity, doEnable, new SimpleCallback<ResponseWrapper<Boolean>>() {

            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                boolean enabled = entity.isEnabled();
                String text = enabled ? Console.MESSAGES
                        .successDisabled("XA datasource " + entity.getName()) : Console.MESSAGES
                        .successEnabled("XA datasource " + entity.getName());
                if (result.getUnderlying()) {
                    Console.info(text);
                } else {
                    Console.error(Console.MESSAGES.modificationFailed("Datasource " + entity.getName()),
                            result.getResponse().toString());
                }

                loadXADatasources();
            }
        });
    }

    public void closeDialogue() {
        window.hide();
    }

    public void verifyConnection(final DataSource dataSource, boolean xa, boolean existing,
            Scheduler.ScheduledCommand onCreated) {
        VerifyConnectionOp vop = new VerifyConnectionOp(dataSourceStore, dispatcher, beanFactory,
                currentProfileSelection.getName());
        vop.execute(dataSource, xa, existing, new SimpleCallback<VerifyConnectionOp.VerifyResult>() {
            @Override
            public void onSuccess(final VerifyConnectionOp.VerifyResult result) {
                getView().showVerifyConncectionResult(dataSource.getName(), result);
                if (result.wasCreated() && onCreated != null) {
                    Scheduler.get().scheduleDeferred(onCreated);
                }
            }
        });
    }

}
