package org.jboss.as.console.client.shared.state;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Heiko Braun
 * @date 12/10/12
 */
public class ReloadNotification extends GwtEvent<ReloadNotification.Handler> {

    public static final Type TYPE = new Type<Handler>();

    private String message;

    public ReloadNotification(String message) {
        this.message = message;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler listener) {
        listener.onReloadRequired(message);
    }

    public interface Handler extends EventHandler {
        void onReloadRequired(String message);
    }
}