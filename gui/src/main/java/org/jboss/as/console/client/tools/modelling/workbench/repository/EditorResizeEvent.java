package org.jboss.as.console.client.tools.modelling.workbench.repository;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Heiko Braun
 * @date 12/10/12
 */
public class EditorResizeEvent extends GwtEvent<EditorResizeEvent.ResizeListener> {

    public static final Type TYPE = new Type<ResizeListener>();
    boolean fullscreen = true;

    public EditorResizeEvent(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    @Override
    public Type<ResizeListener> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ResizeListener listener) {
        listener.onResizeRequested(fullscreen);
    }

    public interface ResizeListener extends EventHandler {
        void onResizeRequested(boolean fullscreen);
    }
}
