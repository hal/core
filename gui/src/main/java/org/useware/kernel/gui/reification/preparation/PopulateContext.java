package org.useware.kernel.gui.reification.preparation;

import com.google.web.bindery.event.shared.EventBus;
import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.StatementContext;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ContextKey;
import org.useware.kernel.model.Dialog;

/**
 * @author Harald Pehl
 * @date 02/22/2013
 */
public class PopulateContext extends ReificationPreperation
{
    final EventBus eventBus;
    final InteractionCoordinator coordinator;
    final StatementContext statementContext;

    public PopulateContext(final EventBus eventBus, final InteractionCoordinator coordinator,
            final StatementContext statementContext)
    {
        super("populate context");
        this.eventBus = eventBus;
        this.coordinator = coordinator;
        this.statementContext = statementContext;
    }

    @Override
    public void prepare(final Dialog dialog, final Context context)
    {
        context.set(ContextKey.EVENTBUS, eventBus);
        context.set(ContextKey.COORDINATOR, coordinator);
    }

    @Override
    public void prepareAsync(final Dialog dialog, final Context context, final Callback callback)
    {
        throw new UnsupportedOperationException("Only sync preperation is suported");
    }
}
