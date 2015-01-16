package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 09/01/15
 */
public class NavigationColumn<T> {


    private final SingleSelectionModel<T> selectionModel;
    private final CellList<T> cellList;
    private final String title;
    private final ProvidesKey keyProvider;
    private HTML header;
    private ToolStrip toolstrip;
    private boolean plain = false;

    public NavigationColumn(String title, final Display display, ProvidesKey keyProvider) {
        this.title = title;
        this.keyProvider = keyProvider;
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

    public NavigationColumn<T> setPlain(boolean plain) {
        this.plain = plain;
        return this;
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

        if(!plain) {     // including the header

            header = new HTML(title);
            header.addStyleName("server-picker-section-header");

            ScrollPanel nav = new ScrollPanel(cellList);

            layout.add(header);

            if (toolstrip != null)
                layout.add(toolstrip.asWidget());

            layout.add(nav);

            if (toolstrip != null) {
                layout.setWidgetTopHeight(header, 0, Style.Unit.PX, 40, Style.Unit.PX);
                layout.setWidgetTopHeight(toolstrip, 40, Style.Unit.PX, 70, Style.Unit.PX);
                layout.setWidgetTopHeight(nav, 70, Style.Unit.PX, 100, Style.Unit.PCT);
            } else {
                layout.setWidgetTopHeight(header, 0, Style.Unit.PX, 40, Style.Unit.PX);
                layout.setWidgetTopHeight(nav, 40, Style.Unit.PX, 100, Style.Unit.PCT);
            }

        }
        else            // embedded mode, w/o header
        {
            ScrollPanel nav = new ScrollPanel(cellList);

            if (toolstrip != null)
                layout.add(toolstrip.asWidget());

            layout.add(nav);

            if (toolstrip != null) {
                layout.setWidgetTopHeight(toolstrip, 0, Style.Unit.PX, 30, Style.Unit.PX);
                layout.setWidgetTopHeight(nav, 30, Style.Unit.PX, 100, Style.Unit.PCT);
            } else {
                layout.setWidgetTopHeight(nav, 0, Style.Unit.PX, 100, Style.Unit.PCT);
            }

        }

        return layout;
    }

    public void updateFrom(List<T> records) {
        updateFrom(records, false);
    }

    public void updateFrom(final List<T> records, final boolean selectDefault) {
        selectionModel.clear();
        cellList.setRowCount(records.size(), true);
        cellList.setRowData(0, records);

        if(!plain) header.setHTML(title+" ("+records.size()+")");
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

    public void selectByKey(Object key) {
        selectionModel.clear();
        int i=0;
        for(T item : cellList.getVisibleItems()) {
            if(keyProvider.getKey(item).equals(key)) {
                selectionModel.setSelected(item, true);
                cellList.getRowElement(i).scrollIntoView();
                break;
            }
            i++;
        }
    }

    public void setTools(ToolStrip toolStrip) {
        this.toolstrip = toolStrip;
    }
}
