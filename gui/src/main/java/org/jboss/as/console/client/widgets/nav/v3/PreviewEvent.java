package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * @author Heiko Braun
 * @since 13/02/15
 */
public class PreviewEvent extends GwtEvent<PreviewEvent.Handler> {

    public static final Type TYPE = new Type<Handler>();
    private final SafeHtml html;

    private PreviewEvent(SafeHtml html) {
        this.html = html;
    }

    public static void fire(final HasHandlers source, SafeHtml html) {
        source.fireEvent(new PreviewEvent(html));
    }

    public SafeHtml getHtml() {
        return html;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler listener) {
        listener.onPreview(this);
    }

    public interface Handler extends EventHandler {
        void onPreview(PreviewEvent event);
    }
}