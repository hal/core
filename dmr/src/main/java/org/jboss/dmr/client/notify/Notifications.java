package org.jboss.dmr.client.notify;

import com.google.gwt.core.shared.GWT;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import org.jboss.as.console.client.shared.state.ReloadNotification;

/**
 * @author Heiko Braun
 * @date 8/22/13
 */
public class Notifications {

    public static EventBus EVENT_BUS = GWT.create(SimpleEventBus.class);

    public static void addReloadHandler(ReloadNotification.Handler handler)
    {
        EVENT_BUS.addHandler(ReloadNotification.TYPE, handler);
    }

    public static void fireReloadNotification(ReloadNotification notification)
    {
        EVENT_BUS.fireEvent(notification);
    }
}
