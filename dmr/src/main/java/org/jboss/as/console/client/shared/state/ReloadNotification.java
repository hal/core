package org.jboss.as.console.client.shared.state;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 12/10/12
 */
public class ReloadNotification extends GwtEvent<ReloadNotification.Handler> {

    public static final Type TYPE = new Type<Handler>();
    private final Map<String, ServerState> states;

    public ReloadNotification(Map<String, ServerState> serverStates) {
        this.states = serverStates;
    }

    public enum State {RELOAD_REQUIRED, RESTART_REQUIRED}

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler listener) {
        listener.onReloadRequired(states);
    }

    public interface Handler extends EventHandler {
        void onReloadRequired(Map<String, ServerState> states);
    }
}