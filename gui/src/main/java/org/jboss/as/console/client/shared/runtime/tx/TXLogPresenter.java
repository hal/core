package org.jboss.as.console.client.shared.runtime.tx;

import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.plugins.RuntimeGroup;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.RuntimeExtension;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 11/3/11
 */
public class TXLogPresenter extends CircuitPresenter<TXLogPresenter.MyView, TXLogPresenter.MyProxy> {

    @ProxyCodeSplit
    @NameToken("tx-logs")
    @SearchIndex(keywords = {"recovery", "durability", "transaction-log", "transaction"})
    @RuntimeExtension(name = "Transaction Logs", group = RuntimeGroup.METRICS, key = "transactions")
    @AccessControl(
            resources = {"/{implicit.host}/{selected.server}/subsystem=transactions/log-store=log-store"},
            operations = "/{implicit.host}/{selected.server}/subsystem=transactions/log-store=log-store#probe")
    public interface MyProxy extends Proxy<TXLogPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(TXLogPresenter presenter);

        void clear();

        void updateFrom(List<TXRecord> records);

        void updateParticpantsFrom(List<TXParticipant> records);
    }

    private static final String ACTION = "action";
    private static final String RELOAD = "reload";
    private static final String DELETE = "delete";
    private static final String REFRESH = "refresh";
    private static final String RECOVER = "recover";
    private static final String PROBE = "probe";

    private DispatchAsync dispatcher;
    private EntityAdapter<TXRecord> entityAdapter;
    private RevealStrategy revealStrategy;
    private final EntityAdapter<TXParticipant> participantAdapter;

    @Inject
    public TXLogPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher, Dispatcher circuit,
            ApplicationMetaData metaData, RevealStrategy revealStrategy) {
        super(eventBus, view, proxy, circuit);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;

        this.entityAdapter = new EntityAdapter<TXRecord>(TXRecord.class, metaData);
        this.participantAdapter = new EntityAdapter<TXParticipant>(TXParticipant.class, metaData);
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
                getView().clear();
                refresh(ACTION);
            }
        });
    }

    @Override
    protected void onReset() {
        super.onReset();
        refresh(RELOAD);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    public void refresh(String type) {

        // clear at first
        getView().clear();

        ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem","transactions");
        address.add("log-store","log-store");

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("transactions");
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();

                if(result.isFailure())
                {
                    Console.error("Failed to read transactions logs", result.getFailureDescription());
                }
                else
                {
                    List<Property> items = result.get(RESULT).asPropertyList();
                    List<TXRecord> records = new ArrayList<TXRecord>(items.size());
                    for(Property item : items)
                    {
                        TXRecord txRecord = entityAdapter.fromDMR(item.getValue());
                        txRecord.setId(item.getName());

                        records.add(txRecord);
                    }

                    // update view
                    getView().updateFrom(records);

                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            Console.info(Console.MESSAGES.successful(type + " operation"));
                        }
                    });
                }


            }
        });
    }

    public void onLoadParticipants(final TXRecord selection) {
        ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem","transactions");
        address.add("log-store","log-store");
        address.add("transactions", selection.getId());

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("participants");
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();

                if(result.isFailure())
                {
                    Console.error("Failed to read transactions participants", result.getFailureDescription());
                }
                else
                {
                    List<Property> items = result.get(RESULT).asPropertyList();
                    List<TXParticipant> records = new ArrayList<TXParticipant>(items.size());
                    for(Property item : items)
                    {
                        TXParticipant participant = participantAdapter.fromDMR(item.getValue());
                        participant.setId(item.getName());
                        participant.setLog(selection.getId()); // FK

                        records.add(participant);
                    }

                    // update view
                    getView().updateParticpantsFrom(records);
                }


            }
        });
    }

    public void onDeleteRecord(final TXRecord selection) {
        ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem","transactions");
        address.add("log-store","log-store");
        address.add("transactions", selection.getId());

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(DELETE);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();

                if(result.isFailure())
                {
                    Console.error(Console.MESSAGES.deletionFailed(selection.getId()), result.getFailureDescription());
                }
                else
                {
                    refresh(DELETE);
                }

            }
        });
    }


    public void onRefreshParticipant(TXParticipant selection) {
        ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem","transactions");
        address.add("log-store","log-store");
        address.add("transactions", selection.getLog());
        address.add("participants", selection.getId());

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REFRESH);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();

                if(result.isFailure())
                {
                    Console.error("Refresh operation failed", result.getFailureDescription());
                }
                else
                {
                    refresh(REFRESH);
                }
            }
        });
    }

    public void onRecoverParticipant(TXParticipant selection) {
        ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem","transactions");
        address.add("log-store","log-store");
        address.add("transactions", selection.getLog());
        address.add("participants", selection.getId());

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(RECOVER);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();

                if(result.isFailure())
                {
                    Console.error("Recover operation failed", result.getFailureDescription());
                }
                else
                {
                    refresh(RECOVER);
                }
            }
        });
    }

    public void onProbe() {
        ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem","transactions");
        address.add("log-store","log-store");

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(PROBE);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();

                if(result.isFailure())
                {
                    Console.error("Probe operation failed", result.getFailureDescription());
                }
                else
                {
                    refresh(PROBE);
                }
            }
        });
    }

}
