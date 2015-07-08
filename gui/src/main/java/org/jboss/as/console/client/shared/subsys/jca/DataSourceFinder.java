package org.jboss.as.console.client.shared.subsys.jca;

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
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.ApplicationProperties;
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
import org.jboss.as.console.client.shared.subsys.jca.wizard.NewXADatasourceWizard;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

import java.util.Collections;
import java.util.List;

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
    private final ApplicationProperties bootstrap;
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
    }


    @Inject
    public DataSourceFinder(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
                            DispatchAsync dispatcher, RevealStrategy revealStrategy, DataSourceStore dataSourceStore,
                            DataSourceTemplates dataSourceTemplates, BeanFactory beanFactory,
                            DriverRegistry driverRegistry,  ApplicationProperties bootstrap, CurrentProfileSelection currentProfileSelection) {

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
        loadDatasources();
        loadXADatasources();
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
        if(isXa)
            loadXADatasources();
        else
            loadDatasources();
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
        if(Console.getBootstrapContext().isStandalone())
            RevealContentEvent.fire(this, ServerMgmtApplicationPresenter.TYPE_MainContent, this);
        else
            RevealContentEvent.fire(this, ProfileMgmtPresenter.TYPE_MainContent, this);
    }

    public void closeDialoge() {
        window.hide();
    }

    @Override
    public void onPreview(PreviewEvent event) {
        if(isVisible())
            getView().setPreview(event.getHtml());
    }

    public void launchNewDatasourceWizard() {

        window = new DefaultWindow(Console.MESSAGES.createTitle("Datasource"));
        window.setWidth(480);
        window.setHeight(450);
        window.setWidget(new NewDatasourceWizard(DataSourceFinder.this, drivers, datasources, bootstrap,
                dataSourceTemplates, beanFactory).asWidget());
        window.setGlassEnabled(true);
        window.center();

    }

    public void launchNewXADatasourceWizard() {

        window = new DefaultWindow(Console.MESSAGES.createTitle("XA Datasource"));
        window.setWidth(480);
        window.setHeight(450);
        window.setWidget(new NewXADatasourceWizard(DataSourceFinder.this, drivers, xaDatasources, bootstrap,
                dataSourceTemplates, beanFactory).asWidget());
        window.setGlassEnabled(true);
        window.center();
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
                DataSourceFinder.this.drivers = result;
            }
        });
    }

    public void onCreateDatasource(final DataSource datasource) {
        window.hide();

        // HAL-617 / WFLY-4750
        datasource.setEnabled(true);

        // TODO find a proper way to deal with this
        if("".equals(datasource.getUsername()))
            datasource.setUsername(null);
        if("".equals(datasource.getPassword()))
            datasource.setPassword(null);
        if("".equals(datasource.getSecurityDomain()))
            datasource.setSecurityDomain(null);

        // HAL-397
        datasource.setCcm(true);
        datasource.setJta(true);

        dataSourceStore.createDataSource(datasource, new SimpleCallback<ResponseWrapper<Boolean>>() {

            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                if (result.getUnderlying()) {
                    Console.info(Console.MESSAGES.added("Datasource ")+ datasource.getName());
                    loadDatasources();
                }
                else
                    Console.error(Console.MESSAGES.addingFailed("Datasource " + datasource.getName()), result.getResponse().toString());
            }
        });

    }

    public void onCreateXADatasource(final XADataSource updatedEntity) {
        window.hide();

        // HAL-617 / WFLY-4750
        updatedEntity.setEnabled(true);

        if("".equals(updatedEntity.getUsername()))
            updatedEntity.setUsername(null);
        if("".equals(updatedEntity.getPassword()))
            updatedEntity.setPassword(null);
        if("".equals(updatedEntity.getSecurityDomain()))
            updatedEntity.setSecurityDomain(null);

        updatedEntity.setCcm(true);

        dataSourceStore.createXADataSource(updatedEntity, new SimpleCallback<ResponseWrapper<Boolean>>() {
            @Override
            public void onSuccess(ResponseWrapper<Boolean> response) {
                if (response.getUnderlying())
                    Console.info(Console.MESSAGES.added("XA Datasource " + updatedEntity.getName()));
                else
                    Console.error(Console.MESSAGES.addingFailed("XA Datasource " + updatedEntity.getName()), response.getResponse().toString());

                loadXADatasources();
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

                loadDatasources();
            }
        });
    }

    public void onDeleteXA(final XADataSource entity) {
        dataSourceStore.deleteXADataSource(entity, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {

                if (success) {
                    Console.info(Console.MESSAGES.deleted("XA Datasource "+ entity.getName()));
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
                String text = enabled ? Console.MESSAGES.successDisabled("Datasource " + entity.getName()) :Console.MESSAGES.successEnabled("Datasource " + entity.getName()) ;
                if (result.getUnderlying()) {
                    Console.info(text);
                } else {
                    Console.error(Console.MESSAGES.modificationFailed("Datasource ") + entity.getName(), result.getResponse().toString());
                }

                loadDatasources();
            }
        });
    }

    public void onDisableXA(final XADataSource entity, boolean doEnable) {
        dataSourceStore.enableXADataSource(entity, doEnable, new SimpleCallback<ResponseWrapper<Boolean>>()
        {

            @Override
            public void onSuccess(ResponseWrapper<Boolean> result) {
                boolean enabled = entity.isEnabled();
                String text = enabled ? Console.MESSAGES.successDisabled("XA datasource " + entity.getName()) : Console.MESSAGES.successEnabled("XA datasource " + entity.getName()) ;
                if (result.getUnderlying()) {
                    Console.info(text);
                } else {
                    Console.error(Console.MESSAGES.modificationFailed("Datasource " + entity.getName()), result.getResponse().toString());
                }

                loadXADatasources();
            }
        });
    }

    public void closeDialogue() {
        window.hide();
    }

    public void verifyConnection(final DataSource dataSource, boolean xa, boolean existing) {
        VerifyConnectionOp vop = new VerifyConnectionOp(dataSourceStore, dispatcher, beanFactory,
                currentProfileSelection.getName());
        vop.execute(dataSource, xa, existing, new SimpleCallback<VerifyConnectionOp.VerifyResult>() {
            @Override
            public void onSuccess(final VerifyConnectionOp.VerifyResult result) {
                getView().showVerifyConncectionResult(dataSource.getName(), result);
            }
        });
    }

}
