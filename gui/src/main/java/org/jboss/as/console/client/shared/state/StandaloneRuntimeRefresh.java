package org.jboss.as.console.client.shared.state;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Heiko Braun
 * @date 12/10/12
 */
public class StandaloneRuntimeRefresh extends GwtEvent<StandaloneRuntimeRefresh.Handler> {

    public static final Type TYPE = new Type<Handler>();

    public StandaloneRuntimeRefresh() {

    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler listener) {
        listener.onStaleModel();
    }

    public interface Handler extends EventHandler {
        void onStaleModel();
    }
}