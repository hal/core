package org.jboss.as.console.client.shared.state;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Heiko Braun
 * @date 12/10/12
 */
public class ReloadNotification extends GwtEvent<ReloadNotification.ReloadListener> {

    public static final Type TYPE = new Type<ReloadListener>();

    @Override
    public Type<ReloadListener> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ReloadListener listener) {
        listener.onReloadRequired();
    }

    public interface ReloadListener extends EventHandler {
        void onReloadRequired();
    }
}