/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.gwt.flow.client;

import com.google.gwt.core.client.Scheduler;

/**
 * Flow control functions for GWT.
 * Integrates with the default GWT scheduling mechanism.
 *
 * @author Heiko Braun
 */
public class Async<C> {

    private final static Object EMPTY_CONTEXT = new Object();
    private final Progress progress;

    public Async() {
        this(new Progress.Nop());
    }

    public Async(final Progress progress) {
        this.progress = progress;
    }

    /**
     * Run an array of functions in series, each one running once the previous function has completed.
     * If any functions in the series pass an error to its callback,
     * no more functions are run and outcome for the series is immediately called with the value of the error.
     */
    @SuppressWarnings("unchecked")
    public void series(final Outcome outcome, final Function... functions) {
        _series(null, outcome, functions);  // generic signature problem, hence null
    }

    /**
     * Runs an array of functions in series, working on a shared context.
     * However, if any of the functions pass an error to the callback,
     * the next function is not executed and the outcome is immediately called with the error.
     */
    @SafeVarargs
    public final void waterfall(final C context, final Outcome<C> outcome, final Function<C>... functions) {
        _series(context, outcome, functions);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private final void _series(C context, final Outcome<C> outcome, final Function<C>... functions) {
        final C finalContext = context != null ? context : (C) EMPTY_CONTEXT;
        final SequentialControl<C> ctrl = new SequentialControl<C>(finalContext, functions);

        // reset progress
        progress.reset(functions.length);

        // select first function and start
        ctrl.proceed();
        Scheduler.get().scheduleIncremental(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (ctrl.isDrained()) {
                    // schedule deferred so that 'return false' executes first!
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            progress.finish();
                            outcome.onSuccess(finalContext);
                        }
                    });
                    return false;
                } else if (ctrl.isAborted()) {
                    // schedule deferred so that 'return false' executes first!
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            progress.finish();
                            outcome.onFailure(finalContext);
                        }
                    });
                    return false;
                } else {
                    ctrl.nextUnlessPending();
                    return true;
                }
            }
        });
    }

    /**
     * Run an array of functions in parallel, without waiting until the previous function has completed.
     * If any of the functions pass an error to its callback, the outcome is immediately called with the value of the
     * error.
     */
    @SuppressWarnings("unchecked")
    public void parallel(C context, final Outcome<C> outcome, final Function<C>... functions) {
        final C finalContext = context != null ? context : (C) EMPTY_CONTEXT;
        final CountingControl<C> ctrl = new CountingControl<C>(finalContext, functions);
        progress.reset(functions.length);

        Scheduler.get().scheduleIncremental(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (ctrl.isAborted() || ctrl.allFinished()) {
                    // schedule deferred so that 'return false' executes first!
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        @SuppressWarnings("unchecked")
                        public void execute() {
                            if (ctrl.isAborted()) {
                                progress.finish();
                                outcome.onFailure(finalContext);
                            } else {
                                progress.finish();
                                outcome.onSuccess(finalContext);
                            }

                        }
                    });
                    return false;
                } else {
                    // one after the other until all are active
                    ctrl.next();
                    return true;
                }
            }
        });
    }

    /**
     * Repeatedly call function, while condition is met. Calls the callback when stopped, or an error occurs.
     */
    public void whilst(Precondition condition, final Outcome outcome, final Function function) {
        whilst(condition, outcome, function, -1);
    }

    /**
     * Same as {@link #whilst(Precondition, Outcome, Function)} but waits {@code period} millis between calls to
     * {@code function}.
     *
     * @param period any value below 100 is ignored!
     */
    public void whilst(Precondition condition, final Outcome outcome, final Function function, int period) {
        final GuardedControl ctrl = new GuardedControl(condition);
        progress.reset();

        Scheduler.RepeatingCommand repeatingCommand = new Scheduler.RepeatingCommand() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean execute() {
                if (!ctrl.shouldProceed()) {
                    // schedule deferred so that 'return false' executes first!
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            if (ctrl.isAborted()) {
                                progress.finish();
                                outcome.onFailure(EMPTY_CONTEXT);
                            } else {
                                progress.finish();
                                outcome.onSuccess(EMPTY_CONTEXT);
                            }
                        }
                    });
                    return false;
                } else {
                    function.execute(ctrl);
                    progress.tick();
                    return true;
                }
            }
        };

        if (period > 100) {
            Scheduler.get().scheduleFixedPeriod(repeatingCommand, period);
        } else {
            Scheduler.get().scheduleIncremental(repeatingCommand);
        }
    }

    private class SequentialControl<C> implements Control<C> {

        private final C context;
        private final Function<C>[] functions;
        private Function<C> next;
        private int index;
        private boolean drained;
        private boolean aborted;
        private boolean pending;

        @SafeVarargs
        SequentialControl(final C context, final Function<C>... functions) {
            this.context = context;
            this.functions = functions;
        }

        @Override
        public C getContext() {
            return context;
        }

        @Override
        public void proceed() {
            if (index > 0) {
                // start ticking *after* the first function has finished
                progress.tick();
            }
            if (index >= functions.length) {
                next = null;
                drained = true;
            } else {
                next = functions[index];
                index++;
            }
            this.pending = false;
        }

        @Override
        public void abort() {
            new RuntimeException("").printStackTrace();
            this.aborted = true;
            this.pending = false;
        }

        public boolean isAborted() {
            return aborted;
        }

        public boolean isDrained() {
            return drained;
        }

        public void nextUnlessPending() {
            if (!pending) {
                pending = true;
                next.execute(this);
            }
        }
    }

    private class CountingControl<C> implements Control<C> {

        private final C context;
        private final Function<C>[] functions;
        protected boolean aborted;
        private int index;
        private int finished;

        @SafeVarargs
        CountingControl(final C context, Function<C>... functions) {
            this.context = context;
            this.functions = functions;
        }

        @Override
        public C getContext() {
            return context;
        }

        @SuppressWarnings("unchecked")
        public void next() {
            if (index < functions.length) {
                functions[index].execute(this);
                index++;
            }
        }

        @Override
        public void proceed() {
            if (index > 0) {
                // start ticking *after* the first function has finished
                progress.tick();
            }
            increment();
        }

        private void increment() {
            ++finished;
        }

        @Override
        public void abort() {
            increment();
            aborted = true;
        }

        public boolean isAborted() {
            return aborted;
        }

        public boolean allFinished() {
            return finished >= functions.length;
        }
    }

    private class GuardedControl implements Control {

        private final Precondition condition;
        private boolean aborted;

        GuardedControl(Precondition condition) {
            this.condition = condition;
        }

        @Override
        public void proceed() {
            // ignore
        }

        public boolean shouldProceed() {
            return condition.isMet() && !aborted;
        }

        @Override
        public void abort() {
            this.aborted = true;
        }

        public boolean isAborted() {
            return aborted;
        }

        @Override
        public Object getContext() {
            return EMPTY_CONTEXT;
        }
    }
}
