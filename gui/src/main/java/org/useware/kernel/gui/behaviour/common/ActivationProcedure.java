package org.useware.kernel.gui.behaviour.common;

import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.ModelDrivenCommand;
import org.useware.kernel.gui.behaviour.Procedure;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;

/**
 * Verifies activation constraints and activate a unit and it's corresponding scope.
 *
 * @see InteractionCoordinator#activateUnit(org.useware.kernel.model.structure.QName)
 * @see org.useware.kernel.gui.behaviour.DialogState#activateBranch(org.useware.kernel.model.structure.InteractionUnit)
 * @see InteractionCoordinator#onNavigationEvent(org.useware.kernel.gui.behaviour.NavigationEvent)
 * @see org.useware.kernel.gui.behaviour.InteractionCoordinator#activate()
 *
 * @author Heiko Braun
 * @date 2/26/13
 */
public class ActivationProcedure extends Procedure {

    private final static Resource<ResourceType> activation = new Resource<ResourceType>(CommonQNames.ACTIVATION_ID, ResourceType.System);

    public ActivationProcedure(final InteractionCoordinator coordinator) {
        super(CommonQNames.ACTIVATION_ID);
        this.coordinator = coordinator;


        setCommand(new ModelDrivenCommand() {
            @Override
            public void execute(Dialog dialog, Object data) {

                // activate target unit
                QName targetId = (QName)data;

                // 1.) verify activation constraints
                assert getRuntimeAPI().canBeActivated(targetId) : "Unit is not activatable: "+ targetId;


                String suffix = targetId.getSuffix();
                boolean isRelative = suffix !=null && (suffix.equals("prev")||suffix.equals("next"));

                InteractionUnit unit = dialog.findUnit(targetId);
                if(isRelative)
                    coordinator.getDialogState().activateBranch(unit, suffix);
                else
                    coordinator.getDialogState().activateBranch(unit);

                // 2.) activate the scope of the unit
                //coordinator.getDialogState().activateScope(targetId);

                // 3.) activate the unit itself
                // typically the parent unit is the listener for these events
                // and adopts this intention to a specific widget API (open a window, select a tab, etc)
                // (see InteractionCoordinator#activateUnit())

            }
        });

        // complement model
        setOutputs(activation);

    }



}
