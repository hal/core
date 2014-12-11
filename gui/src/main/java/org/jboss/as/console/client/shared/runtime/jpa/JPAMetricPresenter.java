package org.jboss.as.console.client.shared.runtime.jpa;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.jpa.model.JPADeployment;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 1/19/12
 */
public class JPAMetricPresenter extends CircuitPresenter<JPAMetricPresenter.MyView, JPAMetricPresenter.MyProxy> {

    @ProxyCodeSplit
    @NameToken(NameTokens.JPAMetricPresenter)
    @SearchIndex(keywords = {"jpa", "hibernate"})
    @RequiredResources(resources = {"/{selected.host}/{selected.server}/deployment=*/subsystem=jpa"})
    public interface MyProxy extends ProxyPlace<JPAMetricPresenter> {}


    public interface MyView extends View, HasPresenter<JPAMetricPresenter> {
        void setJpaUnits(List<JPADeployment> jpaUnits);
        void setSelectedUnit(String[] strings);
        void updateMetric(UnitMetric unitMetric);
        void clearValues();
    }


    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private BeanFactory factory;
    private PlaceManager placeManager;
    private String[] selectedUnit;
    private EntityAdapter<JPADeployment> adapter;

    @Inject
    public JPAMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher, Dispatcher circuit,
            ApplicationMetaData metaData, RevealStrategy revealStrategy,
            BeanFactory factory, PlaceManager placeManager) {
        super(eventBus, view, proxy, circuit);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.placeManager = placeManager;
        this.factory = factory;

        adapter = new EntityAdapter<JPADeployment>(JPADeployment.class, metaData);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(Console.MODULES.getServerStore());
    }

    @Override
    protected void onAction(Action action) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                refresh(true);
            }
        });
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        String dpl = request.getParameter("dpl", null);
        if (dpl != null) {
            this.selectedUnit = new String[]{
                    dpl,
                    request.getParameter("unit", null)
            };
        } else {
            this.selectedUnit = null;
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        if(isVisible()) refresh(true);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    public void refresh(final boolean paging) {

        getView().clearValues();
        getView().setJpaUnits(Collections.EMPTY_LIST);
        getView().setSelectedUnit(null);

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).setEmptyList();
        operation.get(OP).set(COMPOSITE);


        List<ModelNode> steps = new ArrayList<ModelNode>();

        ModelNode deploymentsOp = new ModelNode();
        deploymentsOp.get(OP).set(READ_RESOURCE_OPERATION);
        deploymentsOp.get(ADDRESS).set(RuntimeBaseAddress.get());
        deploymentsOp.get(ADDRESS).add("deployment", "*");
        deploymentsOp.get(ADDRESS).add("subsystem", "jpa");
        deploymentsOp.get(INCLUDE_RUNTIME).set(true);
        deploymentsOp.get(RECURSIVE).set(true);

        ModelNode subdeploymentOp = new ModelNode();
        subdeploymentOp.get(OP).set(READ_RESOURCE_OPERATION);
        subdeploymentOp.get(ADDRESS).set(RuntimeBaseAddress.get());
        subdeploymentOp.get(ADDRESS).add("deployment", "*");
        subdeploymentOp.get(ADDRESS).add("subdeployment", "*");
        subdeploymentOp.get(ADDRESS).add("subsystem", "jpa");
        subdeploymentOp.get(INCLUDE_RUNTIME).set(true);
        subdeploymentOp.get(RECURSIVE).set(true);

        steps.add(deploymentsOp);
        steps.add(subdeploymentOp);

        operation.get(STEPS).set(steps);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode compositeResponse = result.get();

                if(compositeResponse.isFailure())
                {
                    Log.error(Console.MESSAGES.failed("JPA Deployments"), compositeResponse.getFailureDescription());
                }
                else
                {
                    List<JPADeployment> jpaUnits = new ArrayList<JPADeployment>();

                    ModelNode compositeResult = compositeResponse.get(RESULT);

                    ModelNode mainResponse = compositeResult.get("step-1");
                    ModelNode subdeploymentResponse = compositeResult.get("step-2");

                    parseJpaResources(mainResponse, jpaUnits);
                    parseJpaResources(subdeploymentResponse, jpaUnits);

                    getView().setJpaUnits(jpaUnits);
                }


                // update selection (paging)
                if(paging)
                    getView().setSelectedUnit(selectedUnit);


            }
        });
    }

    private void parseJpaResources(ModelNode response, List<JPADeployment> jpaUnits) {

        List<ModelNode> deployments = response.get(RESULT).asList();

        for(ModelNode deployment : deployments)
        {
            ModelNode deploymentValue = deployment.get(RESULT).asObject();

            if(deploymentValue.hasDefined("hibernate-persistence-unit"))
            {

                List<Property> units = deploymentValue.get("hibernate-persistence-unit").asPropertyList();

                for(Property unit : units)
                {

                    JPADeployment jpaDeployment = factory.jpaDeployment().as();
                    ModelNode unitValue = unit.getValue();
                    String tokenString = unit.getName();
                    String[] tokens = tokenString.split("#");
                    jpaDeployment.setDeploymentName(tokens[0]);
                    jpaDeployment.setPersistenceUnit(tokens[1]);

                    // https://issues.jboss.org/browse/AS7-5157
                    boolean enabled = unitValue.hasDefined("enabled") ? unitValue.get("enabled").asBoolean() : false;
                    jpaDeployment.setMetricEnabled(enabled);

                    jpaUnits.add(jpaDeployment);
                }

            }
        }
    }

    public void loadMetrics(String[] tokens) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());


        // parent deployment names
        if(tokens[0].indexOf("/")!=-1)
        {
            String[] parent = tokens[0].split("/");
            operation.get(ADDRESS).add("deployment", parent[0]);
            operation.get(ADDRESS).add("subdeployment", parent[1]);
        }
        else
        {
            operation.get(ADDRESS).add("deployment", tokens[0]);
        }

        operation.get(ADDRESS).add("subsystem", "jpa");
        operation.get(ADDRESS).add("hibernate-persistence-unit", tokens[0]+"#"+tokens[1]);

        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Console.error(
                            Console.MESSAGES.failed("JPA Metrics"),
                            response.getFailureDescription()
                    );
                }
                else
                {

                    ModelNode payload  = response.get(RESULT).asObject();

                    boolean isEnabled = payload.get("enabled").asBoolean();

                    if(!isEnabled)
                    {

                        getView().updateMetric(
                                new UnitMetric(false)
                        );
                    }
                    else
                    {

                        Metric txMetric = new Metric(
                                payload.get("completed-transaction-count").asLong(),
                                payload.get("successful-transaction-count").asLong()
                        );

                        //  ----

                        Metric queryExecMetric = new Metric(
                                payload.get("query-execution-count").asLong(),
                                payload.get("query-execution-max-time").asLong()
                        );

                        queryExecMetric.add(
                                payload.get("query-execution-max-time-query-string").asString()
                        );


                        //  ----

                        Metric queryCacheMetric = new Metric(
                                payload.get("query-cache-put-count").asLong(),
                                payload.get("query-cache-hit-count").asLong(),
                                payload.get("query-cache-miss-count").asLong()
                        );

                        //  ----

                        Metric secondLevelCacheMetric = new Metric(
                                payload.get("second-level-cache-put-count").asLong(),
                                payload.get("second-level-cache-hit-count").asLong(),
                                payload.get("second-level-cache-miss-count").asLong()
                        );


                        Metric connectionMetric = new Metric(
                                payload.get("session-open-count").asLong(),
                                payload.get("session-close-count").asLong(),
                                payload.get("connect-count").asLong()
                        );

                        getView().updateMetric(
                                new UnitMetric(
                                        txMetric,
                                        queryCacheMetric,
                                        queryExecMetric,
                                        secondLevelCacheMetric,
                                        connectionMetric
                                )
                        );

                    }

                }

            }
        });
    }

    public void onSaveJPADeployment(JPADeployment editedEntity, Map<String, Object> changeset) {

        ModelNode address = new ModelNode();
        address.get(ADDRESS).set(RuntimeBaseAddress.get());

        // parent deployment names
        if(editedEntity.getDeploymentName().indexOf("/")!=-1)
        {
            String[] parent = editedEntity.getDeploymentName().split("/");
            address.get(ADDRESS).add("deployment", parent[0]);
            address.get(ADDRESS).add("subdeployment", parent[1]);
        }
        else
        {
            address.get(ADDRESS).add("deployment", editedEntity.getDeploymentName());
        }


        address.get(ADDRESS).add("subsystem", "jpa");
        address.get(ADDRESS).add("hibernate-persistence-unit", editedEntity.getDeploymentName()+"#"+editedEntity.getPersistenceUnit());

        ModelNode operation = adapter.fromChangeset(changeset, address);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Console.error(
                            Console.MESSAGES.modificationFailed("JPA Deployment"),
                            response.getFailureDescription());
                }
                else
                {
                    Console.info(Console.MESSAGES.modified("JPA Deployment"));

                    refresh(false);
                }
            }
        });
    }
}
