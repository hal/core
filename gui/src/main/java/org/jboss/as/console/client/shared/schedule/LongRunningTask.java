package org.jboss.as.console.client.shared.schedule;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.PopupPanel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.dispatch.AsyncCommand;

/**
 * Use Async.flow() instead
 *
 * @author Heiko Braun
 * @date 12/19/11
 */
@Deprecated
public class LongRunningTask {

    private AsyncCommand<Boolean> command;
    private int numAttempts = 0;
    private int limit;
    private boolean keepRunning = true;
    private String message = null;

    public LongRunningTask(AsyncCommand<Boolean> command, int limit) {
        this.command = command;
        this.limit = limit;

    }

    public void schedule(int millis) {

        final PopupPanel window = Feedback.loading(
                Console.CONSTANTS.common_label_plaseWait(),
                Console.CONSTANTS.common_label_requestProcessed(),
                new Feedback.LoadingCallback() {
                    @Override
                    public void onCancel() {
                        keepRunning = false;
                    }
                });

        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {

                numAttempts++;

                if(numAttempts>limit)
                {
                    Console.warning(Console.CONSTANTS.requestTimeout());
                    keepRunning=false;
                }
                else
                {

                    if(keepRunning) {
                        command.execute(new SimpleCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                keepRunning = result;
                            }
                        });
                    }
                }

                if(!keepRunning && window!=null)
                    window.hide();

                return keepRunning;
            }
        }, millis);

    }

    public void setMessage(String message) {
        this.message = message;
    }
}
