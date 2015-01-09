package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 09/01/15
 */
public class NavigationColumn<T> {


    private final SingleSelectionModel<T> selectionModel;
    private final CellList<T> cellList;
    private final String title;


    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    public NavigationColumn(String title, final Display display) {
        this.title = title;
        selectionModel = new SingleSelectionModel<T>();

        cellList = new CellList<T>(new AbstractCell<T>()
        {

            @Override
            public void render(Context context, T data, SafeHtmlBuilder sb) {
                String cssName = (context.getIndex() %2 > 0) ? "combobox-item combobox-item-odd" : "combobox-item";

                if(data.equals(selectionModel.getSelectedObject()))
                    cssName+=" combobox-item-selected";

                sb.append(display.render(cssName, data));
            }

        });

        cellList.setSelectionModel(selectionModel);


    }

    public void addSelectionChangeHandler(SelectionChangeEvent.Handler handler) {
        selectionModel.addSelectionChangeHandler(handler);
    }

    public boolean hasSelectedItem() {
        return selectionModel.getSelectedObject()!=null;
    }

    public T getSelectedItem() {
        return selectionModel.getSelectedObject();
    }

    public Widget asWidget() {

        LayoutPanel layout = new LayoutPanel();
        layout.addStyleName("navigation-column");

        HTML header = new HTML(title);
        header.addStyleName("server-picker-section-header");

        ScrollPanel nav = new ScrollPanel(cellList);

        layout.add(header);
        layout.add(nav);

        layout.setWidgetTopHeight(header, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(nav, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        return layout;
    }

    public void updateFrom(List<T> records) {
        updateFrom(records, false);
    }

    public void updateFrom(final List<T> records, final boolean selectDefault) {
        selectionModel.clear();
        cellList.setRowCount(records.size(), true);
        cellList.setRowData(0, records);

        if(selectDefault && records.size()>0)
        {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    selectionModel.setSelected(records.get(0), true);
                }
            });
        }
    }

    public interface Display<T> {
        public SafeHtml render(String baseCss, T data);
    }
}
