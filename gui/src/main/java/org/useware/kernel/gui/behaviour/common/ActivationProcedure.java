package org.useware.kernel.gui.behaviour.common;

import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.ModelDrivenCommand;
import org.useware.kernel.gui.behaviour.Procedure;
import org.useware.kernel.gui.behaviour.ProcedureRuntimeAPI;
import org.useware.kernel.gui.behaviour.SystemEvent;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.structure.QName;

/**
 * @author Heiko Braun
 * @date 2/26/13
 */
public class ActivationProcedure extends Procedure {

    Resource<ResourceType> activation = new Resource<ResourceType>(CommonQNames.ACTIVATION_ID, ResourceType.System);

    public ActivationProcedure(final InteractionCoordinator coordinator) {
        super(CommonQNames.ACTIVATION_ID);
        this.coordinator = coordinator;


        setCommand(new ModelDrivenCommand() {
            @Override
            public void execute(Dialog dialog, Object data) {

                // activate target unit
                QName targetUnit = (QName)data;

                // activate scope
                coordinator.getStatementScope().activateScope(targetUnit);
                //assert getRuntimeAPI().canBeActivated(targetUnit) : "Unit is not activatable: "+ targetUnit;

                if(!getRuntimeAPI().canBeActivated(targetUnit))
                    System.out.println("WARN: Unit is not activatable: "+ targetUnit);

                System.out.println("Activate: "+targetUnit);

                SystemEvent activationEvent = new SystemEvent(CommonQNames.ACTIVATION_ID);
                activationEvent.setPayload(targetUnit);

                coordinator.fireEvent(activationEvent);

            }
        });

        // complement model
        setOutputs(activation);

    }



}
