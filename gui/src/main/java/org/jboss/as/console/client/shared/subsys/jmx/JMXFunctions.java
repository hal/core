package org.jboss.as.console.client.shared.subsys.jmx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADD;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.VALUE;
import static org.jboss.dmr.client.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

/**
 * @author Harald Pehl
 */
class JMXFunctions {

    private static final AddressTemplate JMX_SUBSYSTEM_TEMPLATE = AddressTemplate
            .of("{selected.profile}/subsystem=jmx");
    private static final AddressTemplate REMOTING_CONNECTOR_TEMPLATE = JMX_SUBSYSTEM_TEMPLATE
            .append("remoting-connector=jmx");


    /**
     * Checks whether the {@code remoting-connector=jmx} resource exists and pushes {@code 200} to the context stack
     * if it exists, {@code 404} otherwise.
     */
    static class CheckRemotingConnector implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final StatementContext statementContext;

        CheckRemotingConnector(final DispatchAsync dispatcher, final StatementContext statementContext) {
            this.dispatcher = dispatcher;
            this.statementContext = statementContext;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            ResourceAddress address = REMOTING_CONNECTOR_TEMPLATE.resolve(statementContext);
            Operation operation = new Operation.Builder(READ_RESOURCE_OPERATION, address).build();
            dispatcher.execute(new DMRAction(operation), new FunctionCallback(control) {
                @Override
                protected void proceed() {
                    // role mapping exists
                    control.getContext().push(200);
                    control.proceed();
                }

                @Override
                protected void abort() {
                    // no role mapping found
                    control.getContext().push(404);
                    control.proceed();
                }
            });
        }
    }


    /**
     * Adds the {@code remoting-connector=jmx} resource if the predicate returns {@code true}, proceeds otherwise.
     * Expects an integer status code at the top of the context stack which is used to call the predicate.
     */
    static class AddRemotingConnector implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final StatementContext statementContext;
        private final Predicate<Integer> predicate;

        AddRemotingConnector(final DispatchAsync dispatcher, final StatementContext statementContext,
                final Predicate<Integer> predicate) {
            this.dispatcher = dispatcher;
            this.statementContext = statementContext;
            this.predicate = predicate;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            if (control.getContext().emptyStack()) {
                control.proceed();
            } else {
                Integer status = control.getContext().pop();
                if (predicate.apply(status)) {
                    ResourceAddress address = REMOTING_CONNECTOR_TEMPLATE.resolve(statementContext);
                    Operation operation = new Operation.Builder(ADD, address).build();
                    dispatcher.execute(new DMRAction(operation), new FunctionCallback(control));
                } else {
                    control.proceed();
                }
            }
        }
    }


    static class ModifyJmxAttributes implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final StatementContext statementContext;
        private final Map<String, Object> changeset;

        ModifyJmxAttributes(final DispatchAsync dispatcher, final StatementContext statementContext,
                final Map<String, Object> changeset) {
            this.dispatcher = dispatcher;
            this.statementContext = statementContext;
            this.changeset = changeset;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            List<Operation> steps = new ArrayList<>();

            if (changeset.containsKey("sensitive")) {
                ResourceAddress address = JMX_SUBSYSTEM_TEMPLATE.resolve(statementContext);
                Operation operation = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, address)
                        .param(NAME, "non-core-mbean-sensitivity")
                        .param(VALUE, (Boolean) changeset.get("sensitive"))
                        .build();
                steps.add(operation);
            }
            if (changeset.containsKey("showModel")) {
                ResourceAddress address = JMX_SUBSYSTEM_TEMPLATE.resolve(statementContext);
                Operation operation = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, address)
                        .param(NAME, "show-model")
                        .param(VALUE, (Boolean) changeset.get("showModel"))
                        .build();
                steps.add(operation);
            }
            if (changeset.containsKey("mgmtEndpoint")) {
                ResourceAddress address = REMOTING_CONNECTOR_TEMPLATE.resolve(statementContext);
                Operation operation = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, address)
                        .param(NAME, "use-management-endpoint")
                        .param(VALUE, (Boolean) changeset.get("mgmtEndpoint"))
                        .build();
                steps.add(operation);
            }
            dispatcher.execute(new DMRAction(new Composite(steps)), new FunctionCallback(control));
        }
    }
}

