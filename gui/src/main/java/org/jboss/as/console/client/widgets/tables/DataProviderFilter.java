package org.jboss.as.console.client.widgets.tables;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 7/31/12
 */
public class DataProviderFilter<T> {

    private ListDataProvider<T> delegate;
    private ArrayList<T> origValues = new ArrayList<T>();
    private Predicate predicate;
    private TextBox filter;
    private Range origVisibleRange;


    public interface Predicate<T> {

        boolean apply(String prefix, T candiate);
    }

    /**
     * @param delegate
     * @param predicate
     */
    public DataProviderFilter(ListDataProvider<T> delegate, Predicate<T> predicate) {
        this.delegate = delegate;
        this.predicate = predicate;
        this.filter = new TextBox();
    }

    private void clearSelection() {
        for (HasData<T> table : delegate.getDataDisplays()) {
            SelectionModel<? super T> selectionModel = table.getSelectionModel();
            if (selectionModel instanceof SingleSelectionModel) {
                SingleSelectionModel<T> sm = (SingleSelectionModel<T>) selectionModel;
                T selectedObject = sm.getSelectedObject();
                if (selectedObject != null) {
                    ((SingleSelectionModel<T>) selectionModel).setSelected(selectedObject, false);
                }
            }
        }
    }

    /**
     * a new backup if the data provider list will be made. <br/>
     * you need to do it when the data provider gets new records.
     */
    public void reset() {
        origVisibleRange = new Range(0, this.delegate.getList().size());
        snapshot();
        clearFilter();
        filter.setText("");
    }

    /**
     * creates a backup of the orig data provider values
     * that are used to reset the provider state when the filter is cleared.
     */
    private void snapshot() {
        // backup original
        this.origValues.clear();
        this.origValues.addAll(delegate.getList());
    }

    public Widget asWidget() {


        filter.setMaxLength(30);
        filter.setVisibleLength(20);
        filter.getElement().setAttribute("style", "width:120px;");

        filter.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {

                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {

                    @Override
                    public void execute() {
                        String prefix = filter.getText();

                        if (prefix != null && !prefix.equals("")) {
                            // filter by prefix
                            filterByPrefix(prefix);
                        } else {
                            clearFilter();
                        }
                    }

                });
            }
        });
        HorizontalPanel panel = new HorizontalPanel();
        Label label = new Label(Console.CONSTANTS.common_label_filter() + ": ");
        panel.add(label);
        panel.add(filter);

        label.getElement().setAttribute("style", "padding-top:8px;margin-right:8px");
        return panel;
    }

    private void filterByPrefix(String prefix) {

        clearSelection();
        delegate.getDataDisplays().iterator().next().setVisibleRange(origVisibleRange);

        final List<T> next = new ArrayList<T>();
        for (T item : origValues) {
            if (predicate.apply(prefix, item)) { next.add(item); }
        }


        delegate.getList().clear(); // cannot call setList() as that breaks the sort handler
        delegate.getList().addAll(next);

        if (next.size() < origVisibleRange.getLength()) {
            for (HasData table : delegate.getDataDisplays()) {
                table.setVisibleRange(0, next.size());
            }
        }
        delegate.flush();

    }

    public void clearFilter() {
        delegate.getList().clear(); // cannot call setList() as that breaks the sort handler
        delegate.getList().addAll(origValues);
        if (origVisibleRange != null) {
            delegate.getDataDisplays().iterator().next().setVisibleRange(origVisibleRange);
        }
        delegate.flush();
    }
}
