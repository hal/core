package org.useware.kernel.gui.behaviour.common;

import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.ModelDrivenCommand;
import org.useware.kernel.gui.behaviour.Procedure;
import org.useware.kernel.gui.behaviour.StatementEvent;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.structure.QName;

/**
 * @author Heiko Braun
 * @date 2/26/13
 */
public class SelectStatementProcedure extends Procedure {

    private final static Resource<ResourceType> SELECT = new Resource<ResourceType>(CommonQNames.SELECT_ID, ResourceType.Statement);

    public SelectStatementProcedure(final InteractionCoordinator coordinator) {
        super(CommonQNames.SELECT_ID);
        this.coordinator = coordinator;


        setCommand(new ModelDrivenCommand() {
            @Override
            public void execute(Dialog dialog, Object data) {

                StatementEvent event = (StatementEvent)data;

                QName sourceId = (QName)event.getSource();
                String key = event.getKey();
                String value = event.getValue();


                if(value!=null)
                    coordinator.setStatement(sourceId, key, value);
                else
                    coordinator.clearStatement(sourceId, key);

                // when statement change, the system will be clear
                coordinator.reset();
            }
        });

        setInputs(SELECT);

    }



}
