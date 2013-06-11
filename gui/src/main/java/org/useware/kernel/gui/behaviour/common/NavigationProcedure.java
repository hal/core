package org.useware.kernel.gui.behaviour.common;

import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.ModelDrivenCommand;
import org.useware.kernel.gui.behaviour.Procedure;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.structure.QName;

/**
 * @author Heiko Braun
 * @date 2/26/13
 */
public class NavigationProcedure extends Procedure {

    public final static QName ID = QName.valueOf("org.useware:navigate#next");
    private final static Resource<ResourceType> navigation = new Resource<ResourceType>(ID, ResourceType.Interaction);

    public NavigationProcedure(final InteractionCoordinator coordinator) {
        super(ID);
        this.coordinator = coordinator;


        setCommand(new ModelDrivenCommand() {
            @Override
            public void execute(Dialog dialog, Object data) {
                // activate target unit

                QName targetUnit = (QName)data;
                System.out.println("navigate "+targetUnit);


            }
        });

        // complement model
        setInputs(navigation);

    }



}
