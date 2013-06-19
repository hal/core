package org.jboss.as.console.mbui;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.widgets.progress.ProgressBar;
import org.jboss.as.console.client.widgets.progress.ProgressWindow;
import org.jboss.as.console.mbui.bootstrap.ReificationBootstrap;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;
import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.KernelContract;
import org.useware.kernel.gui.behaviour.NavigationDelegate;
import org.useware.kernel.gui.behaviour.StatementContext;
import org.useware.kernel.model.scopes.BranchActivation;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ContextKey;
import org.jboss.as.console.mbui.reification.pipeline.BuildUserInterfaceStep;
import org.jboss.as.console.mbui.reification.pipeline.ImplicitBehaviourStep;
import org.useware.kernel.gui.reification.pipeline.IntegrityStep;
import org.useware.kernel.gui.reification.pipeline.ReificationPipeline;
import org.useware.kernel.gui.reification.pipeline.UniqueIdCheckStep;
import org.jboss.as.console.mbui.bootstrap.ReadOperationDescriptions;
import org.jboss.as.console.mbui.bootstrap.ReadResourceDescription;
import org.useware.kernel.gui.reification.strategy.ReificationWidget;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.structure.QName;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 3/22/13
 */
public class Kernel implements NavigationDelegate {

    private DialogRepository repository;
    private final StatementContext globalContext;

    private Map<String, KernelContract> coordinators = new HashMap<String, KernelContract>();
    private Map<String, ReificationWidget> cachedWidgets = new HashMap<String, ReificationWidget>();
    private String activeDialog;
    private final Framework framework;
    private boolean enableCache;

    public Kernel(DialogRepository repository, Framework framework, StatementContext globalContext) {
        this.repository = repository;
        this.globalContext = globalContext;
        this.framework = framework;
    }

    /**
     * Absolute navigation
     * @param source
     * @param dialog
     */
    @Override
    public void onNavigation(QName source, QName dialog) {

        System.out.println("absolute navigation " + source + ">" + dialog);
    }

    public void reify(final String name, final AsyncCallback<Widget> callback) {


        // passivate current instance before switching
        if(getActiveCoordinator()!=null)
            passivate();

        activeDialog = name;

        if (null == cachedWidgets.get(name) || this.enableCache == false)
        {

            // fetch dialog meta data
            final Dialog dialog  =  repository.getDialog(name);

            // create coordinator instance
            final InteractionCoordinator coordinator = new InteractionCoordinator(
                    dialog, globalContext, this
            );
            coordinators.put(name, coordinator);

            // top level interaction unit & context
            final Context context = new Context();

            final ProgressWindow progress = new ProgressWindow("Building Dialog");


            // build reification pipeline
            Function<Context> prepareContext = new Function<Context>() {
                @Override
                public void execute(Control<Context> control) {
                    context.set(ContextKey.EVENTBUS, coordinator.getLocalBus());
                    context.set(ContextKey.COORDINATOR, coordinator);

                    control.proceed();
                }
            };

            Function<Context> readOperationMetaData = new Function<Context>() {
                @Override
                public void execute(final Control<Context> control) {

                    ReadOperationDescriptions operationMetaData = new ReadOperationDescriptions(framework.getDispatcher());
                    operationMetaData.prepareAsync(dialog, context, new ReificationBootstrap.Callback()
                    {
                        @Override
                        public void onError(Throwable caught) {
                            Log.error("ReadOperationDescriptions failed: " + caught.getMessage(), caught);
                            progress.getBar().setProgress(50.0);
                            control.abort();
                        }

                        @Override
                        public void onSuccess() {
                            Log.info("Successfully retrieved operation meta data");
                            progress.getBar().setProgress(50.0);
                            control.proceed();
                        }
                    });
                }
            };

            Function<Context> readResourceMetaData = new Function<Context>() {
                @Override
                public void execute(final Control<Context> control) {
                    ReificationBootstrap readResourceDescription = new ReadResourceDescription(framework.getDispatcher());
                    readResourceDescription.prepareAsync(dialog, context, new ReificationBootstrap.Callback()
                    {
                        @Override
                        public void onSuccess()
                        {
                            Log.info("Successfully retrieved resource meta data");

                            // setup & start the reification pipeline
                            ReificationPipeline pipeline = new ReificationPipeline(
                                    new UniqueIdCheckStep(),
                                    new BuildUserInterfaceStep(),
                                    new ImplicitBehaviourStep(framework.getDispatcher()),
                                    new IntegrityStep());

                            pipeline.execute(dialog, context);

                            progress.getBar().setProgress(100.0);

                            control.proceed();
                        }

                        @Override
                        public void onError(final Throwable caught)
                        {
                            Log.error("ReadResourceDescription failed: " + caught.getMessage(), caught);
                            progress.getBar().setProgress(100.0);
                            control.abort();
                        }
                    });
                }
            };

            Outcome<Context> outcome = new Outcome<Context>() {
                @Override
                public void onFailure(final Context context) {

                    progress.hide();
                    Window.alert("Reification failed");
                }

                @Override
                public void onSuccess(final Context context) {

                    progress.hide();

                    // show result
                    ReificationWidget widget = context.get(ContextKey.WIDGET);
                    assert widget !=null;

                    cachedWidgets.put(name, widget);
                    BranchActivation activation = new BranchActivation();
                    dialog.getInterfaceModel().accept(activation);
                    //System.out.println("<< Default Activation: "+activation.getCandidate()+">>");

                    callback.onSuccess(widget.asWidget());
                }
            };

            // execute pipeline
            progress.center();
            progress.getBar().setProgress(25.0);

            new Async<Context>().waterfall(
                    context, outcome,
                    prepareContext, readOperationMetaData, readResourceMetaData
            );
        }
        else
        {
            callback.onSuccess(cachedWidgets.get(name).asWidget());
        }

    }

    public void activate() {
        assert activeDialog != null : "Active dialog required";
        getActiveCoordinator().activate();

    }

    public void reset() {
        assert activeDialog != null : "Active dialog required";
        getActiveCoordinator().reset();
    }

    private KernelContract getActiveCoordinator() {
        return coordinators.get(activeDialog);
    }

    public void passivate() {
        getActiveCoordinator().passivate();
    }

    public void setCaching(boolean enableCache) {
        this.enableCache = enableCache;
        cachedWidgets.clear();
    }
}
