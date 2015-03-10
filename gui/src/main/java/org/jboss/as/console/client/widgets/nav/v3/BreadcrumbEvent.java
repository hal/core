package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

/**
 * @author Heiko Braun
 * @since 13/02/15
 */
public class BreadcrumbEvent extends GwtEvent<BreadcrumbEvent.Handler> {

    public static final Type TYPE = new Type<Handler>();
    private final String value;
    private final boolean isMenuEvent;
    private final String type;
    private final String title;
    private final FinderColumn.FinderId key;
    private final boolean isSelected;

    private BreadcrumbEvent(FinderColumn.FinderId correlationId, String type, String title, boolean isSelected, String value, boolean isMenuEvent) {
        this.key = correlationId;
        this.type = type;
        this.title = title;
        this.isSelected = isSelected;
        this.value = value;
        this.isMenuEvent = isMenuEvent;
    }

    public static void fire(
            final HasHandlers source,
            FinderColumn.FinderId id,
            String type, String title,
            boolean isSelected,
            String value,
            boolean isMenuEvent) {

        source.fireEvent(new BreadcrumbEvent(id, type, title, isSelected, value, isMenuEvent));
    }

    public FinderColumn.FinderId getCorrelationId() {
        return key;
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return title;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public boolean isMenuEvent() {
        return isMenuEvent;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler listener) {
        listener.onBreadcrumbEvent(this);
    }

    public String getValue() {
        return value;
    }

    public interface Handler extends EventHandler {
        void onBreadcrumbEvent(BreadcrumbEvent event);
    }


    public boolean typeEquals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BreadcrumbEvent that = (BreadcrumbEvent) o;

        if (key != that.key) return false;
        if (!type.equals(that.type)) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BreadcrumbEvent that = (BreadcrumbEvent) o;

        if (key != that.key) return false;
        if (!type.equals(that.type)) return false;
        if (!value.equals(that.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + title.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BreadcrumbEvent{" +
                "key=" + key +
                ", type='" + type + '\'' +
                ", selected='" + isSelected()+ '\'' +
                '}';
    }
}
