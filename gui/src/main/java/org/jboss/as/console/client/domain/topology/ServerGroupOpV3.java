package org.jboss.as.console.client.domain.topology;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupDAO;
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

public class ServerGroupOpV3 extends TopologyOp {

    private final Map<String, Object> params;
    private final DispatchAsync dispatcher;
    private final ServerGroupDAO serverGroupDAO;
    private final String group;
    private final ModelNode node;

    public ServerGroupOpV3(final LifecycleOperation op, Map<String, Object> params, final LifecycleCallback callback, final DispatchAsync dispatcher,
                           final ServerGroupDAO serverGroupDAO, final String group,
                           final List<Server> server) {

        super(op, callback);
        this.params = params;

        this.dispatcher = dispatcher;
        this.serverGroupDAO = serverGroupDAO;
        this.group = group;

        this.node = new ModelNode();
        this.node.get(ADDRESS).setEmptyList();
        this.node.get(OP).set(COMPOSITE);
        List<ModelNode> steps = new LinkedList<ModelNode>();

        for (Server serverRef : server) {
            ModelNode serverStateOp = new ModelNode();
            serverStateOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
            serverStateOp.get(NAME).set("status");
            serverStateOp.get(ADDRESS).add("host", serverRef.getHostName());
            serverStateOp.get(ADDRESS).add("server-config", serverRef.getName());
            steps.add(serverStateOp);
        }

        this.node.get(STEPS).set(steps);
    }

    public void run() {
        BooleanCallback bc = new BooleanCallback();
        switch (op) {
            case START:
                serverGroupDAO.startServerGroup(group, bc);
                break;
            case STOP:
                serverGroupDAO.stopServerGroup(group, bc);
                break;
            case RESTART:
                serverGroupDAO.restartServerGroup(group, bc);
                break;
            case SUSPEND:
                serverGroupDAO.suspendServerGroup(group, params, bc);
                break;
            case RESUME:
                serverGroupDAO.resumeServerGroup(group, bc);
                break;
            case KILL:
            case RELOAD:
                // not supported for server groups
                break;
        }
        new Async(Footer.PROGRESS_ELEMENT).whilst(new KeepGoing(), new Finish(), new QueryStatus(), 1000);
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
                                switch (op) {
                                    case START:
                                    case RESTART:
                                        lifecycleReached = "started".equalsIgnoreCase(status);
                                        break;
                                    case STOP:
                                        lifecycleReached = "stopped".equalsIgnoreCase(status);
                                        break;
                                    case SUSPEND:
                                    case RESUME:
                                        lifecycleReached = true;
                                        break;
                                    case KILL:
                                    case RELOAD:
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
