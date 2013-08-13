package org.jboss.as.console.mbui.behaviour;

import com.google.gwt.user.client.Window;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.Precondition;
import org.useware.kernel.gui.behaviour.Procedure;
import org.useware.kernel.gui.behaviour.StatementContext;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.mapping.MappingType;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.jboss.as.console.mbui.model.StereoTypes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executes an operation on a DMR resource.
 * <p/>
 * The actual entity address is resolved from the {@link org.jboss.as.console.mbui.model.mapping.DMRMapping}
 * attached to the {@link InteractionUnit} that triggered this procedure (justification).
 * <p/>
 * The operation name is derived from the suffix of {@link Resource} being produced.
 *
 * @see CommandFactory
 *
 * @author Heiko Braun
 * @date 1/21/13
 */
public class DMROperationProcedure extends Procedure implements OperationContext {

    public final static QName PREFIX = new QName("org.jboss.as", "resource-operation");
    private final Map<QName, ModelNode> operationDescriptions;

    private final DispatchAsync dispatcher;
    private final Dialog dialog;

    private InteractionUnit<StereoTypes> unit;
    private AddressMapping address;
    private String operationName;

    public DMROperationProcedure(
            final Dialog dialog,
            final QName id,
            final QName justification,
            DispatchAsync dispatcher, Map<QName, ModelNode> operationDescriptions, SecurityContext securityContext) {

        super(id, justification);
        this.dialog = dialog;
        this.dispatcher = dispatcher;
        this.operationDescriptions = Collections.unmodifiableMap(operationDescriptions);

        init();

        CommandFactory factory = new CommandFactory(dispatcher, securityContext);
        setCommand(factory.createCommand(operationName, this));

        // behaviour model meta data
        setInputs(new Resource<ResourceType>(id, ResourceType.Interaction));

    }

    private void init() {
        unit = dialog.findUnit(getJustification());
        operationName = getId().getSuffix();

        if(operationName==null)
            throw new IllegalArgumentException("Illegal operation name mapping: "+ unit.getId()+ " (suffix required)");

        DMRMapping mapping = unit.findMapping(MappingType.DMR);
        address = AddressMapping.fromString(mapping.getResolvedAddress());


        // TODO: Refactor init procedure into default precondition ...
        // it appears in several procedures

        // check preconditions of the address token
        final Set<String> requiredStatements = address.getRequiredStatements();

        // any value expression key becomes a precondition matched against the statement context
        setPrecondition(new Precondition() {
            @Override
            public boolean isMet(StatementContext statementContext) {

                // fail fast if not scope active
                if(!getRuntimeAPI().isActive(unit.getId())) return false;

                boolean missingStatement = false;
                for(String key : requiredStatements)
                {
                    missingStatement = statementContext.resolve(key)==null;
                    if(missingStatement) break; // exit upon first value expression that cannot be resolved
                }
                return !missingStatement;
            }
        });


    }


    @Override
    public Dialog getDialog() {
        return dialog;
    }

    @Override
    public InteractionUnit getUnit() {
        return unit;
    }

    @Override
    public AddressMapping getAddress() {
        return address;
    }

    @Override
    public DispatchAsync getDispatcher() {
        return dispatcher;
    }

    @Override
    public StatementContext getStatementContext() {
        return statementScope.getContext(getUnit().getId());
    }

    @Override
    public InteractionCoordinator getCoordinator() {
        return super.coordinator;
    }

    @Override
    public Map<QName, ModelNode> getOperationDescriptions() {
        return operationDescriptions;
    }

    /**
     * the model representations we use internally carry along the entity keys.
     * these are derived from the resource address, but will be available as synthetic resource attributes.
     *
     * @param payload
     * @param address
     */
    private static void assignKeyFromAddressNode(ModelNode payload, ModelNode address) {
        List<Property> props = address.asPropertyList();
        Property lastToken = props.get(props.size()-1);
        payload.get("entity.key").set(lastToken.getValue().asString());
    }

    @Override
    public String toString() {
        return "DMROperation "+ getJustification();
    }
}
