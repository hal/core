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
    private final String key;
    private final FinderColumn.FinderId finder;
    private final boolean isSelected;

    private BreadcrumbEvent(FinderColumn.FinderId finder, String key, boolean isSelected, String value, boolean isMenuEvent) {
        this.finder = finder;
        this.key = key;
        this.isSelected = isSelected;
        this.value = value;
        this.isMenuEvent = isMenuEvent;
    }

    public static void fire(
            final HasHandlers source,
            FinderColumn.FinderId id,
            String title,
            boolean isSelected,
            String value,
            boolean isMenuEvent) {

        source.fireEvent(new BreadcrumbEvent(id, title, isSelected, value, isMenuEvent));
    }

    public FinderColumn.FinderId getFinderId() {
        return finder;
    }

    public String getKey() {
        return key;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BreadcrumbEvent that = (BreadcrumbEvent) o;

        if (!value.equals(that.value)) return false;
        if (!key.equals(that.key)) return false;
        return finder == that.finder;

    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + finder.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BreadcrumbEvent{" +
                "finderId=" + finder +
                " ,key=" + key +
                " ,value=" + value +
                '}';
    }
}
