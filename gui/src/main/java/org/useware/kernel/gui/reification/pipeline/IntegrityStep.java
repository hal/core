package org.useware.kernel.gui.reification.pipeline;

import com.allen_sauer.gwt.log.client.Log;
import org.useware.kernel.gui.behaviour.Integrity;
import org.useware.kernel.gui.behaviour.IntegrityErrors;
import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ContextKey;
import org.useware.kernel.gui.reification.ReificationException;
import org.useware.kernel.model.Dialog;

/**
 * @author Harald Pehl
 * @date 02/22/2013
 */
public class IntegrityStep extends ReificationStep
{
    public IntegrityStep()
    {
        super("integrity check");
    }

    @Override
    public void execute(final Dialog dialog, final Context context) throws ReificationException
    {
        InteractionCoordinator coordinator = context.get(ContextKey.COORDINATOR);
        try
        {
            // Step 3: Verify integrity
            Integrity.check(
                    dialog.getInterfaceModel(),
                    coordinator.listProcedures()
            );
        }
        catch (IntegrityErrors integrityErrors)
        {
            if (integrityErrors.needsToBeRaised())
            {
                Log.error(integrityErrors.getMessage());
                //                throw new RuntimeException("Integrity check failed", integrityErrors);
            }
        }
    }
}
