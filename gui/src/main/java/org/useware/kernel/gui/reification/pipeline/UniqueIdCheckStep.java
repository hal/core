package org.useware.kernel.gui.reification.pipeline;

import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ReificationException;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Harald Pehl
 * @date 03/14/2013
 */
public class UniqueIdCheckStep extends ReificationStep
{
    public UniqueIdCheckStep()
    {
        super("unique id check");
    }

    @Override
    public void execute(final Dialog dialog, final Context context) throws ReificationException
    {
        IdVisitor idVisitor = new IdVisitor();
        dialog.getInterfaceModel().accept(idVisitor);

        boolean comma = false;
        StringBuilder message = new StringBuilder();
        Map<QName, Integer> idCount = idVisitor.getIdCount();
        for (Map.Entry<QName, Integer> entry : idCount.entrySet())
        {
            if (entry.getValue() > 1)
            {
                if (comma) { message.append(", "); }
                message.append(entry.getKey()).append(": ").append(entry.getValue());
                comma = false;
            }
        }

        if (message.length() != 0)
        {
            throw new ReificationException(
                    "The model contains interaction units with non-unique ids. The following interactions units are used more than once: " + message);
        }
    }


    static class IdVisitor implements InteractionUnitVisitor
    {
        final Map<QName, Integer> idCount;

        IdVisitor() {idCount = new HashMap<QName, Integer>();}

        @Override
        public void startVisit(final Container container)
        {
            // nop
        }

        @Override
        public void visit(final InteractionUnit interactionUnit)
        {
            Integer amount = idCount.get(interactionUnit.getId());
            if (amount == null)
            {
                amount = 0;
            }
            amount++;
            idCount.put(interactionUnit.getId(), amount);
        }

        @Override
        public void endVisit(final Container container)
        {
            // nop
        }

        Map<QName, Integer> getIdCount()
        {
            return idCount;
        }
    }
}
