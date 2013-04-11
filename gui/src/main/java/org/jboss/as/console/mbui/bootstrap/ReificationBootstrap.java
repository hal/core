package org.jboss.as.console.mbui.bootstrap;

import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.model.Dialog;

/**
 * Class for any tasks that must be carried out before the actual reification can start. The reification is managed
 * by the {@link org.useware.kernel.gui.reification.pipeline.ReificationPipeline} and multiple {@link
 * org.useware.kernel.gui.reification.pipeline.ReificationStep}s which are executed synchronously by
 * definition. Asynchronous tasks have to be executed <em>before</em> the actual reification starts using
 * implementations of this interface.
 *
 * @author Harald Pehl
 * @date 02/22/2013
 */
public abstract class ReificationBootstrap
{
    private final String name;

    protected ReificationBootstrap(final String name)
    {
        this.name = name;
    }

    public abstract void prepare(Dialog dialog, Context context);

    public abstract void prepareAsync(Dialog dialog, Context context, Callback callback);

    @Override
    public String toString()
    {
        return "Preparation: " + name;
    }

    public interface Callback
    {
        void onError(Throwable caught);

        void onSuccess();
    }
}
