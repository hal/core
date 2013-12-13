package org.jboss.as.console.mbui;

import java.util.HashMap;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.mbui.bootstrap.ReadOperationDescriptions;
import org.jboss.as.console.mbui.bootstrap.ReadResourceDescription;
import org.jboss.as.console.mbui.bootstrap.ReificationBootstrap;
import org.jboss.as.console.mbui.reification.pipeline.BuildUserInterfaceStep;
import org.jboss.as.console.mbui.reification.pipeline.ImplicitBehaviourStep;
import org.jboss.as.console.mbui.reification.rbac.RequiredResources;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;
import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.KernelContract;
import org.useware.kernel.gui.behaviour.NavigationDelegate;
import org.useware.kernel.gui.behaviour.StatementContext;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ContextKey;
import org.useware.kernel.gui.reification.pipeline.IntegrityStep;
import org.useware.kernel.gui.reification.pipeline.ReificationPipeline;
import org.useware.kernel.gui.reification.pipeline.UniqueIdCheckStep;
import org.useware.kernel.gui.reification.strategy.ReificationWidget;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.scopes.BranchActivation;
import org.useware.kernel.model.structure.QName;

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
    private boolean enableCache = true;

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

    class DialogWrapper {
        Dialog dialog;
        InteractionCoordinator coordinator;

        Dialog getDialog() {
            return dialog;
        }

        void setDialog(Dialog dialog) {
            this.dialog = dialog;
        }

        InteractionCoordinator getCoordinator() {
            return coordinator;
        }

        void setCoordinator(InteractionCoordinator coordinator) {
            this.coordinator = coordinator;
        }
    }
    public void reify(final String name, final AsyncCallback<Widget> callback) {

        // passivate current instance before switching
        if(getActiveCoordinator()!=null)
            passivate();

        activeDialog = name;


        if (null == cachedWidgets.get(name) || this.enableCache == false)
        {

            Function<DialogWrapper> retrieveDialogDescription = new Function<DialogWrapper>() {
                @Override
                public void execute(final Control<DialogWrapper> control) {

                    // fetch dialog meta data
                    repository.getDialog(name, new SimpleCallback<Dialog>() {

                        @Override
                        public void onFailure(Throwable caught) {
                            super.onFailure(caught);
                            control.abort();
                        }

                        @Override
                        public void onSuccess(Dialog result) {
                            control.getContext().setDialog(result);

                            // create coordinator instance
                            final InteractionCoordinator coordinator = new InteractionCoordinator(
                                    result, globalContext, Kernel.this
                            );

                            control.getContext().setCoordinator(coordinator);
                            control.proceed();
                        }
                    });
                }
            };

            Outcome<DialogWrapper> delegateOutcome = new Outcome<DialogWrapper>() {
                @Override
                public void onFailure(DialogWrapper context) {
                    Console.error("Failed to retrieve dialog description: "+name);
                }

                @Override
                public void onSuccess(DialogWrapper context) {
                    doReification(name, context.getDialog(), context.getCoordinator(), callback);
                }
            };

            // step 1
            DialogWrapper setup = new DialogWrapper();
            new Async<DialogWrapper>().waterfall(setup, delegateOutcome, retrieveDialogDescription);

        }
        else
        {
            callback.onSuccess(cachedWidgets.get(name).asWidget());
        }

    }

    private void doReification(
            final String name,
            final Dialog dialog, final InteractionCoordinator coordinator,
            final AsyncCallback<Widget> widgetCallback)
    {
        // cache coordinator
        coordinators.put(name, coordinator);

        // top level interaction unit & context
        final Context context = new Context();

        // build reification pipeline
        Function<Context> prepareContext = new Function<Context>() {
            @Override
            public void execute(Control<Context> control) {
                context.set(ContextKey.EVENTBUS, coordinator.getLocalBus());
                context.set(ContextKey.COORDINATOR, coordinator);
                context.set(ContextKey.SECURITY_CONTEXT, framework.getSecurityFramework());
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
                        //progress.getBar().setProgress(25.0);
                        control.abort();
                    }

                    @Override
                    public void onSuccess() {
                        Log.info("Successfully retrieved operation meta data");
                        //progress.getBar().setProgress(25.0);
                        control.proceed();
                    }
                });
            }
        };

        Function<Context> createSecurityContext = new Function<Context>() {
            @Override
            public void execute(final Control<Context> control) {

                SecurityFramework securityFramework = framework.getSecurityFramework();

                RequiredResources resourceVisitor = new RequiredResources();
                dialog.getInterfaceModel().accept(resourceVisitor);

                securityFramework.createSecurityContext(activeDialog, resourceVisitor.getRequiredresources(),
                        new AsyncCallback<SecurityContext>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Console.error("Failed to create security context", caught.getMessage());
                                //progress.getBar().setProgress(50.0);
                                control.abort();
                            }

                            @Override
                            public void onSuccess(SecurityContext result) {
                                control.getContext().set(ContextKey.SECURITY_CONTEXT, result);
                                //progress.getBar().setProgress(50.0);
                                control.proceed();
                            }
                        }
                );

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

                        try {
                            pipeline.execute(dialog, context);
                            //progress.getBar().setProgress(100.0);
                            control.proceed();
                        } catch (Throwable e) {
                            Console.error("Reification failed: "+ e.getMessage());
                            control.abort();
                        }

                    }

                    @Override
                    public void onError(final Throwable caught)
                    {
                        Log.error("ReadResourceDescription failed: " + caught.getMessage(), caught);
                        //progress.getBar().setProgress(100.0);
                        control.abort();
                    }
                });
            }
        };

        Outcome<Context> outcome = new Outcome<Context>() {
            @Override
            public void onFailure(final Context context) {

                //progress.hide();
                Window.alert("Reification failed");
            }

            @Override
            public void onSuccess(final Context context) {

                //progress.hide();

                // show result
                ReificationWidget widget = context.get(ContextKey.WIDGET);
                assert widget !=null;

                cachedWidgets.put(name, widget);
                BranchActivation activation = new BranchActivation();
                dialog.getInterfaceModel().accept(activation);
                //System.out.println("<< Default Activation: "+activation.getCandidate()+">>");

                widgetCallback.onSuccess(widget.asWidget());
            }
        };

        // execute pipeline
        //progress.center();
        //progress.getBar().setProgress(25.0);

        new Async<Context>(Footer.PROGRESS_ELEMENT).waterfall(context, outcome,
                prepareContext, readOperationMetaData, createSecurityContext, readResourceMetaData
        );
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
        coordinators.clear();
    }

}
