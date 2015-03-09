package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.RowHoverEvent;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.Console;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 09/01/15
 */
public class FinderColumn<T> {


    private static final String CLICK = "click";
    private final SingleSelectionModel<T> selectionModel;
    private final CellTable<T> cellTable;
    private final FinderId correlationId;
    private final String title;
    private final Display display;
    private final ProvidesKey keyProvider;
    private HorizontalPanel header;
    private boolean plain = false;
    private MenuDelegate[] menuItems = new MenuDelegate[]{};
    private MenuDelegate[] topMenuItems = new MenuDelegate[]{};
    private HTML headerTitle;
    private ValueProvider<T> valueProvider;

    public enum FinderId { CONFIGURATION, RUNTIME}

    /**
     * Thje default finder preview
     */
    private final PreviewFactory DEFAULT_PREVIEW = new PreviewFactory() {
        @Override
        public SafeHtml createPreview(Object data) {
            SafeHtmlBuilder builder = new SafeHtmlBuilder();
            String icon = display.isFolder(data) ? "icon-folder-close-alt" : "icon-file-text-alt";
            builder.appendHtmlConstant("<center><i class='"+icon+"' style='font-size:48px;top:100px;position:relative'></i></center>");
            return builder.toSafeHtml();
        }
    };

    private PreviewFactory<T> previewFactory = DEFAULT_PREVIEW;

    public FinderColumn(final FinderId correlationId, final String title, final Display display, final ProvidesKey keyProvider) {
        this.correlationId = correlationId;
        this.title = title;
        this.display = display;
        this.keyProvider = keyProvider;
        selectionModel = new SingleSelectionModel<T>(keyProvider);

        cellTable = new CellTable<T>(200, DefaultCellTable.DEFAULT_CELL_TABLE_RESOURCES , keyProvider);
        cellTable.setStyleName("navigation-cell-table");
        cellTable.getElement().setAttribute("style", "border:none!important");
        cellTable.setLoadingIndicator(new HTML());

        Column<T, SafeHtml> titleColumn = new Column<T, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(T data) {
                return display.render("navigation-column-item", data);
            }
        };

        final Column<T, String> menuColumn = new Column<T, String>(new ButtonCell() {
            public void render(Cell.Context context, SafeHtml data, SafeHtmlBuilder sb) {

                if(menuItems.length>0) {
                    sb.appendHtmlConstant("<div class='nav-menu'>");
                    sb.appendHtmlConstant("<div class='btn-group'>");
                    sb.appendHtmlConstant("<button action='default' class='btn' type='button' tabindex=\"-1\">");
                    if (data != null) {
                        sb.append(data);
                    }
                    sb.appendHtmlConstant("</button>");

                    if(menuItems.length>1) {
                        sb.appendHtmlConstant("<button action='menu' class='btn dropdown-toggle' type='button' tabindex=\"-1\">");
                        sb.appendHtmlConstant("<span><i class='icon-caret-down'></i></span>");
                        sb.appendHtmlConstant("</button>");
                        sb.appendHtmlConstant("</div>");
                        sb.appendHtmlConstant("</div>");
                    }
                }

            }

        }) {


            @Override
            public String getValue(T object) {
                return menuItems.length>0 ? menuItems[0].getTitle() : "";
            }

        };

        Column<T, SafeHtml> iconColumn = new Column<T, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(T data) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                builder.appendHtmlConstant("<i class='icon-caret-right row-icon' style='vertical-align:middle'></i>");
                return builder.toSafeHtml();
            }
        };

        cellTable.addColumn(titleColumn);
        cellTable.addColumn(menuColumn);
        cellTable.addColumn(iconColumn);

        cellTable.setColumnWidth(menuColumn, 50, Style.Unit.PX);
        cellTable.setColumnWidth(iconColumn, 16, Style.Unit.PX);

        cellTable.setSelectionModel(selectionModel);

        // visibility of the context menu column
        cellTable.addRowHoverHandler(new RowHoverEvent.Handler() {
            @Override
            public void onRowHover(RowHoverEvent event) {
                TableRowElement hoveringRow = event.getHoveringRow();

                // skip empty menus
                if(menuItems.length==0) return;

                if(event.isUnHover()) {
                    hoveringRow.removeClassName("nav-hover");
                }
                else
                {
                    hoveringRow.addClassName("nav-hover");
                }

            }
        });

        cellTable.addCellPreviewHandler(new CellPreviewEvent.Handler<T>() {
            @Override
            public void onCellPreview(final CellPreviewEvent<T> event) {
                boolean isClick = CLICK.equals(event.getNativeEvent().getType());
                if(isClick && 1==event.getColumn())
                {
                    event.getNativeEvent().preventDefault();
                    final Element element = Element.as(event.getNativeEvent().getEventTarget());
                    String action = element.getAttribute("action");
                    if("default".equals(action))
                    {
                        menuItems[0].getCommand().executeOn(event.getValue());
                    }
                    else if("menu".equals(action))
                    {
                        openContextMenu(event.getNativeEvent(), event.getValue());
                    }


                }
            }
        });

        cellTable.setRowStyles(new RowStyles<T>() {
            @Override
            public String getStyleNames(T row, int rowIndex) {
                boolean isFolder = display.isFolder(row);
                String css = display.rowCss(row);
                return isFolder ? css + " folder-view" : css + " file-view";
            }
        });


        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {

                        // preview
                        PlaceManager placeManager = Console.MODULES.getPlaceManager();
                        final T selectedObject = selectionModel.getSelectedObject();
                        if(selectedObject!=null) {
                            PreviewEvent.fire(placeManager, getPreview(selectedObject));

                            // delegate to value provider if given, otherwise the keyprovider will do fine
                            String value = valueProvider!=null ? valueProvider.get(selectedObject) :
                                    String.valueOf(keyProvider.getKey(selectedObject));

                            FinderSelectionEvent.fire(placeManager, correlationId, title, selectedObject!=null,
                                    value);
                        }
                        else
                        {
                            FinderSelectionEvent.fire(placeManager, correlationId, title, selectedObject!=null, "");
                        }


                    }
                });
            }
        });
    }

    public FinderId getCorrelationId() {
        return correlationId;
    }

    private void openContextMenu(final NativeEvent event, final T object) {

        Element el = Element.as(event.getEventTarget());
        Element anchor = el.getParentElement();

        final PopupPanel popupPanel = new PopupPanel(true);
        final MenuBar popupMenuBar = new MenuBar(true);
        popupMenuBar.setStyleName("dropdown-menu");

        int i=0;
        for (final MenuDelegate menuitem : menuItems) {

            if(i>0) {     // skip the "default" action
                MenuItem cmd = new MenuItem(menuitem.getTitle(), true, new Command() {

                    @Override
                    public void execute() {
                        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {
                                menuitem.getCommand().executeOn((T) object);
                            }
                        });

                        popupPanel.hide();
                    }

                });

                popupMenuBar.addItem(cmd);
            }
            i++;
        }

        popupMenuBar.setVisible(true);


        popupPanel.setWidget(popupMenuBar);
        int left = anchor.getAbsoluteLeft();
        int top = anchor.getAbsoluteTop() + 22;

        popupPanel.setPopupPosition(left, top);
        popupPanel.setAutoHideEnabled(true);
        popupPanel.show();
    }

    public FinderColumn<T> setTopMenuItems(MenuDelegate... items) {
        this.topMenuItems = items;
        return this;
    }

    public FinderColumn<T> setMenuItems(MenuDelegate... items) {
        this.menuItems = items;
        return this;
    }

    public FinderColumn<T> setPlain(boolean plain) {
        this.plain = plain;
        return this;
    }

    public FinderColumn<T> setPreviewFactory(PreviewFactory<T> previewFactory) {
        this.previewFactory = previewFactory;
        return this;
    }

    public FinderColumn<T> setValueProvider(ValueProvider<T> valueProvider) {
        this.valueProvider = valueProvider;
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
        layout.getElement().setId(title);

        if(!plain) {     // including the header

            header = new HorizontalPanel();
            header.addStyleName("fill-layout-width");
            header.addStyleName("server-picker-section-header");

            headerTitle = new HTML(title);
            headerTitle.getElement().setAttribute("style", "height:25px");
            header.add(headerTitle);
            ScrollPanel nav = new ScrollPanel(cellTable);


            for (final MenuDelegate menuItem : topMenuItems) {
                HTML item = new HTML(menuItem.getTitle());
                item.getElement().setAttribute("style", "color:#0099D3; cursor:pointer;padding-right:5px");
                item.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        menuItem.getCommand().executeOn(null);
                    }
                });
                header.add(item);
                item.getElement().getParentElement().setAttribute("align", "right");
            }


            layout.add(header);
            layout.add(nav);

            layout.setWidgetTopHeight(header, 0, Style.Unit.PX, 40, Style.Unit.PX);
            layout.setWidgetTopHeight(nav, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        }
        else            // embedded mode, w/o header
        {
            ScrollPanel nav = new ScrollPanel(cellTable);

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
        cellTable.setRowCount(records.size(), true);
        cellTable.setRowData(0, records);

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
        boolean isFolder(T data);
        SafeHtml render(String baseCss, T data);
        String rowCss(T data);
    }

    /*public void selectByKey(Object key) {
        selectionModel.clear();
        int i=0;
        for(T item : cellTable.getVisibleItems()) {
            if(keyProvider.getKey(item).equals(key)) {
                selectionModel.setSelected(item, true);
                cellTable.getRowElement(i).scrollIntoView();
                break;
            }
            i++;
        }
    }*/

    public SafeHtml getPreview(T data) {
        return previewFactory.createPreview(data);
    }
}
