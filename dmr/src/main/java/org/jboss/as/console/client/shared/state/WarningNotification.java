package org.jboss.as.console.client.shared.state;

import java.util.logging.Level;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class WarningNotification extends GwtEvent<WarningNotification.Handler> {

    public static final Type<WarningNotification.Handler> TYPE = new Type<WarningNotification.Handler>();
    private final String warning;
    private final Level severity;
    public WarningNotification(final String warning, final Level serverity) {
        this.warning = warning;
        this.severity = serverity;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler listener) {
        listener.onWarning(this.warning, this.severity);
    }

    public interface Handler extends EventHandler {
        public void onWarning(String warning, Level level);
    }
}
