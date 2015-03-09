package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

/**
 * @author Heiko Braun
 * @since 13/02/15
 */
public class FinderSelectionEvent extends GwtEvent<FinderSelectionEvent.Handler> {

    public static final Type TYPE = new Type<Handler>();
    private final String value;
    private final String type;
    private final String title;
    private final FinderColumn.FinderId key;
    private final boolean isSelected;

    private FinderSelectionEvent(FinderColumn.FinderId correlationId, String type, String title, boolean isSelected, String value) {
        this.key = correlationId;
        this.type = type;
        this.title = title;
        this.isSelected = isSelected;
        this.value = value;
    }

    public static void fire(final HasHandlers source, FinderColumn.FinderId id, String type, String title, boolean isSelected, String value) {
        source.fireEvent(new FinderSelectionEvent(id, type, title, isSelected, value));
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

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler listener) {
        listener.onSelectionEvent(this);
    }

    public String getValue() {
        return value;
    }

    public interface Handler extends EventHandler {
        void onSelectionEvent(FinderSelectionEvent event);
    }


    public boolean typeEquals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FinderSelectionEvent that = (FinderSelectionEvent) o;

        if (key != that.key) return false;
        if (!type.equals(that.type)) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FinderSelectionEvent that = (FinderSelectionEvent) o;

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
        return "FinderSelectionEvent{" +
                "key=" + key +
                ", type='" + type + '\'' +
                '}';
    }
}
