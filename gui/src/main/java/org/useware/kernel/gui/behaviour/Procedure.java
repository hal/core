package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.behaviour.Behaviour;
import org.useware.kernel.model.behaviour.Consumer;
import org.useware.kernel.model.behaviour.Producer;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.builder.ResourceConsumption;
import org.useware.kernel.model.structure.builder.ResourceProduction;

import java.util.Set;

/**
 * Represents the behaviour of a dialog model. Procedures are selected for execution by matching a {@link Producer}'s output
 * to a {@link Procedure}'s input. Producers are typically interaction units or procedures themselves.
 *
 * @author Heiko Braun
 * @date 11/16/12
 */
public abstract class Procedure implements Behaviour, Consumer, Producer {

    private ModelDrivenCommand command;
    private QName justification;
    private QName id;

    private ResourceProduction production = new ResourceProduction();
    private ResourceConsumption consumption = new ResourceConsumption();

    protected InteractionCoordinator coordinator;
    protected DialogState statementScope;
    protected Precondition precondition;

    private ProcedureRuntimeAPI runtimeAPI;

    public final static Precondition NOT_GUARDED = new Precondition() {
        @Override
        public boolean isMet(StatementContext context) {
            return true;
        }
    };


    public Procedure(QName id) {
        this.id = id;
        this.justification = null;
        this.precondition = NOT_GUARDED;
    }

    public Procedure(QName id, QName justification) {
        this.id = id;
        this.justification = justification;
        this.precondition = NOT_GUARDED;
    }

    public void setCommand(ModelDrivenCommand command) {
        this.command = command;
    }

    public void setPrecondition(Precondition precondition) {
        this.precondition = precondition;
    }

    Precondition getPrecondition() {
        return precondition;
    }

    public QName getJustification() {
        return justification;
    }

    public ModelDrivenCommand getCommand() {
        return command;
    }

    public QName getId() {
        return id;
    }

    void setCoordinator(InteractionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    void setStatementScope(DialogState scope) {
        this.statementScope = scope;
    }

    // --- Consumer ----


    @Override
    public boolean doesConsume() {
        return consumption.doesConsume();
    }

    @Override
    public boolean doesProduce() {
        return production.doesProduce();
    }

    /**
     * Does consume if the resource and the origin (if given) matches.
     *
     * @param resource the actual resource to be consumed
     * @return boolean
     */
    @Override
    public boolean doesConsume(Resource<ResourceType> resource) {
        boolean resourceMatches = consumption.doesConsume(resource);
        boolean originMatches = false;
        if(resourceMatches)
            originMatches = (justification == null) || justification.equals(resource.getSource());
        return resourceMatches && originMatches;
    }

    @Override
    public Set<Resource<ResourceType>> getInputs() {
        return consumption.getInputs();
    }

    @Override
    public void setInputs(Resource<ResourceType>... resources) {
        consumption.setInputs(resources);
    }

    // --- Consumer ----

    @Override
    public boolean doesProduce(Resource<ResourceType> resource) {
        return production.doesProduce(resource);
    }

    @Override
    public void setOutputs(Resource<ResourceType>... resources) {
        production.setOutputs(resources);
    }

    @Override
    public Set<Resource<ResourceType>> getOutputs() {
        return production.getOutputs();
    }

    public void setRuntimeAPI(ProcedureRuntimeAPI runtimeAPI) {
        this.runtimeAPI = runtimeAPI;
    }

    public ProcedureRuntimeAPI getRuntimeAPI() {
        assert runtimeAPI!=null : "Runtime API not provided";
        return runtimeAPI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Procedure)) return false;

        Procedure procedure = (Procedure) o;

        if (!id.equals(procedure.id)) return false;
        if (justification != null ? !justification.equals(procedure.justification) : procedure.justification != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = justification != null ? justification.hashCode() : 0;
        result = 31 * result + id.hashCode();
        return result;
    }

}
