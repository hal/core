package org.jboss.as.console.client.domain.topology;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

public class HostControllerOpV3 extends TopologyOp {

    private final DispatchAsync dispatcher;
    private final HostInformationStore hostInformationStore;
    private final String host;
    private final ModelNode node;

    public HostControllerOpV3(final LifecycleOperation op, final LifecycleCallback callback, final DispatchAsync dispatcher,
                              final HostInformationStore hostInformationStore, final String host, final List<Server> server) {

        super(op, callback);

        this.dispatcher = dispatcher;
        this.hostInformationStore = hostInformationStore;
        this.host = host;

        this.node = new ModelNode();
        this.node.get(ADDRESS).setEmptyList();
        this.node.get(OP).set(COMPOSITE);
        List<ModelNode> steps = new LinkedList<ModelNode>();

        for (Server serverRef : server) {
            ModelNode serverStateOp = new ModelNode();
            serverStateOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
            serverStateOp.get(ADDRESS).add("host", host);
            serverStateOp.get(ADDRESS).add("server-config", serverRef.getName());
            serverStateOp.get(NAME).set("status");
            steps.add(serverStateOp);
        }

        this.node.get(STEPS).set(steps);
    }

    public void run() {
        BooleanCallback bc = new BooleanCallback();
        switch (op) {
            case RESTART:
                hostInformationStore.restartHost(host, bc);
                break;
            case RELOAD:
                hostInformationStore.reloadHost(host, bc);
                break;
        }
        new Async(Footer.PROGRESS_ELEMENT).whilst(new KeepGoing(), new Finish(), new QueryStatus(), 2000);
    }

    class QueryStatus implements Function<Object> {

        @Override
        public void execute(final Control<Object> control) {
            dispatcher.execute(new DMRAction(node, false), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(Throwable caught) {
                    // ignore
                }

                @Override
                public void onSuccess(DMRResponse dmrResponse) {
                    ModelNode response = dmrResponse.get();
                    if (!response.isFailure()) {
                        List<Property> steps = response.get(RESULT).asPropertyList();
                        for (Property step : steps) {
                            ModelNode stepResult = step.getValue();
                            if (stepResult.get(RESULT).isDefined()) {
                                String status = stepResult.get(RESULT).asString();
                                boolean disabled = "disabled".equalsIgnoreCase(status);
                                boolean stopped = "stopped".equalsIgnoreCase(status);
                                boolean started = "started".equalsIgnoreCase(status);
                                switch (op) {
                                    case START:
                                    case RESTART:
                                        lifecycleReached = started;
                                        lifecycleReached |= disabled;
                                        lifecycleReached |= stopped;
                                        break;
                                    case STOP:
                                        lifecycleReached = stopped;
                                        lifecycleReached |= disabled;
                                        break;
                                    case SUSPEND:
                                    case RESUME:
                                        lifecycleReached = true;
                                        break;
                                    case RELOAD:
                                        lifecycleReached = started;
                                        lifecycleReached |= disabled;
                                        lifecycleReached |= stopped;
                                        break;
                                    case KILL:
                                        // not supported for server groups
                                        break;
                                }
                                if (!lifecycleReached) {
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
