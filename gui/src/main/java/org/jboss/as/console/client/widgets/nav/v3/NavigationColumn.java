package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
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
    private HorizontalPanel header;
    private boolean plain = false;
    private MenuDelegate[] menuItems;
    private MenuDelegate[] topMenuItems;
    private HTML headerTitle;

    public NavigationColumn(String title, final Display display, ProvidesKey keyProvider) {
        this.title = title;
        this.keyProvider = keyProvider;
        selectionModel = new SingleSelectionModel<T>();

        cellList = new CellList<T>(new AbstractCell<T>("contextmenu")
        {

            @Override
            public void render(Context context, T data, SafeHtmlBuilder sb) {
                String cssName = (context.getIndex() %2 > 0) ? "combobox-item combobox-item-odd" : "combobox-item";

                if(data.equals(selectionModel.getSelectedObject()))
                    cssName+=" combobox-item-selected";

                sb.append(display.render(cssName, data));
            }

            @Override
            public void onBrowserEvent(Context context, Element parent, final T value, NativeEvent event, ValueUpdater<T> valueUpdater) {
                String eventType = event.getType();

                if ("contextmenu".equals(eventType)) {

                    event.preventDefault();

                    final PopupPanel popupPanel = new PopupPanel(true);

                    final MenuBar popupMenuBar = new MenuBar(true);

                    for (final MenuDelegate menuitem : menuItems) {
                        MenuItem cmd  = new MenuItem(menuitem.getTitle(), true,  new Command() {

                            @Override
                            public void execute() {
                                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                    @Override
                                    public void execute() {
                                        menuitem.getCommand().executeOn((T) value);
                                    }
                                });

                                popupPanel.hide();
                            }

                        });

                        popupMenuBar.addItem(cmd);
                    }

                    popupMenuBar.setVisible(true);


                    popupPanel.add(popupMenuBar);
                    popupPanel.setPopupPosition(event.getClientX(), event.getClientY());
                    popupPanel.show();

                }
            }
        });

        cellList.setSelectionModel(selectionModel);

    }

    public NavigationColumn<T> setTopMenuItems(MenuDelegate... items) {
           this.topMenuItems = items;
           return this;
       }

    public NavigationColumn<T> setMenuItems(MenuDelegate... items) {
        this.menuItems = items;
        return this;
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

            header = new HorizontalPanel();
            header.addStyleName("fill-layout-width");
            header.addStyleName("server-picker-section-header");

            headerTitle = new HTML(title);
            headerTitle.getElement().setAttribute("style", "height:25px");
            header.add(headerTitle);
            ScrollPanel nav = new ScrollPanel(cellList);


            for (final MenuDelegate menuItem : topMenuItems) {
                HTML item = new HTML(menuItem.getTitle());
                item.getElement().setAttribute("style", "color:#0099D3; cursor:pointer");
                item.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                       menuItem.getCommand().executeOn(null);
                    }
                });
                header.add(item);
            }


            layout.add(header);
            layout.add(nav);

            layout.setWidgetTopHeight(header, 0, Style.Unit.PX, 40, Style.Unit.PX);
            layout.setWidgetTopHeight(nav, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        }
        else            // embedded mode, w/o header
        {
            ScrollPanel nav = new ScrollPanel(cellList);

            layout.add(nav);
            layout.setWidgetTopHeight(nav, 0, Style.Unit.PX, 100, Style.Unit.PCT);

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

        if(!plain) headerTitle.setHTML(title+" ("+records.size()+")");
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
}
