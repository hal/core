package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 16/07/15
 */
public class ColumnFilter<T> {

    private final SingleSelectionModel<T> selection;
    private CellTable<T> delegate;
    private final Predicate predicate;
    private List<T> origValues = Collections.EMPTY_LIST;
    private LayoutPanel panel;
    private TextBox textBox;
    private String currentFilterExpression = null;

    public ColumnFilter(SingleSelectionModel<T> selection, CellTable<T> delegate, Predicate predicate) {
        this.selection = selection;
        this.delegate = delegate;
        this.predicate = predicate;

        delegate.addRowCountChangeHandler(new RowCountChangeEvent.Handler() {
            @Override
            public void onRowCountChange(RowCountChangeEvent event) {
                if (!isFilterActive()) {
                    origValues = delegate.getVisibleItems();
                }
            }
        });
    }

    public Widget asWidget() {
        panel = new LayoutPanel();

        textBox = new TextBox();
        textBox.setMaxLength(30);
        textBox.setVisibleLength(20);
        textBox.addKeyUpHandler(keyUpEvent -> {

            String token = textBox.getText();
            if (token != null
                    && !token.equals("")) {

                if (!token.equals(currentFilterExpression)) {  // prevent keyUp w/o value change
                    currentFilterExpression = token;
                    selection.clear();
                    applyFilter(token);
                }

            } else {
                selection.clear();
                clear();
            }
        });

        HTML icon = new HTML("<i class=\"icon-search\"></i>");
        icon.getElement().setAttribute("style", "padding-top:5px");
        textBox.getElement().setAttribute("style", "padding-top:5px");
        panel.add(icon);
        panel.add(textBox);
        panel.setWidgetLeftWidth(icon, 5, Style.Unit.PX, 20, Style.Unit.PX);
        panel.setWidgetLeftWidth(textBox, 25, Style.Unit.PX, 205, Style.Unit.PX);

        panel.getElement().getStyle().setMargin(5, Style.Unit.PX);
        return panel;
    }

    private void applyFilter(String token) {

        final List<T> filtered  = new ArrayList<T>();
        for(T item : origValues)
        {
            if(predicate.matches(item, token))
                filtered.add(item);
        }

        delegate.setRowCount(filtered.size(), true);
        delegate.setRowData(0, filtered);
    }

    public void clear() {
        textBox.setText("");
        this.currentFilterExpression = null;

        delegate.setRowCount(origValues.size(), true);
        delegate.setRowData(0, origValues);
    }

    public interface Predicate<T> {
        boolean matches(T item, String token);
    }

    private boolean isFilterActive() {
        return this.currentFilterExpression != null;
    }
}
