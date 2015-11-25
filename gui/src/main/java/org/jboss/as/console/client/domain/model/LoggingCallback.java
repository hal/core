package org.jboss.as.console.client.domain.model;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;

public abstract class LoggingCallback<T> implements AsyncCallback<T> {

    @Override
    public void onFailure(Throwable caught) {
        Log.error(Console.CONSTANTS.common_error_unknownError(), caught);
    }
}
