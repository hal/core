package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.RowHoverEvent;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.client.proxy.LockInteractionEvent;
import com.gwtplatform.mvp.client.proxy.LockInteractionHandler;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityContextAware;
import org.jboss.ballroom.client.rbac.SecurityService;
import org.jboss.ballroom.client.spi.Framework;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 09/01/15
 */
public class FinderColumn<T> implements SecurityContextAware {


    private static final String RBAC_SUPPRESSED = "rbac-suppressed";

    // temporary state that prevents preview events
    private boolean previewLock = false;

    private ColumnFilter filter;
    private Object previousSelectedKey = null;

    public enum FinderId { HOME, DEPLOYMENT, CONFIGURATION, RUNTIME, ACCESS_CONTROL, CSP, PATCHING }

    static Framework FRAMEWORK = GWT.create(Framework.class);
    static SecurityService SECURITY_SERVICE = FRAMEWORK.getSecurityService();

    private static final String CLICK = "click";

    private final SingleSelectionModel<T> selectionModel;
    private final CellTable<T> cellTable;
    private final ListDataProvider<T> dataProvider;
    private final FinderId finderId;
    private final String title;
    private final Display display;
    private TooltipDisplay tooltipDisplay = null;
    private FinderTooltip tooltip;
    private final ProvidesKey keyProvider;
    private final String id;
    private final String token;
    private LayoutPanel header;
    private boolean plain = false;
    private MenuDelegate[] menuItems = new MenuDelegate[]{};
    private MenuDelegate[] topMenuItems = new MenuDelegate[]{};

    // accessible items are the ones remaining after the security context has been applied
    private List<MenuDelegate> accessibleMenuItems = new LinkedList<>();

    // accessible items are the ones remaining after the security context has been applied
    private List<MenuDelegate> accessibleTopMenuItems = new LinkedList<>();

    private HTML headerTitle;
    private ValueProvider<T> valueProvider;

    private LayoutPanel layout;
    private LayoutPanel headerMenu;

    private boolean showSize = false;

    /**
     * The default finder preview
     */
    private final PreviewFactory DEFAULT_PREVIEW = new PreviewFactory() {
        @Override
        public void createPreview(Object data, AsyncCallback callback) {
            SafeHtmlBuilder builder = new SafeHtmlBuilder();
            String icon = display.isFolder(data) ? "icon-folder-close-alt" : "icon-file-text-alt";
            builder.appendHtmlConstant("<center><i class='"+icon+"' style='font-size:48px;top:100px;position:relative'></i></center>");

            callback.onSuccess(builder.toSafeHtml());
        }
    };

    private PreviewFactory<T> previewFactory = DEFAULT_PREVIEW;

    public FinderColumn(final FinderId correlationId, final String title, final Display display, final ProvidesKey keyProvider, String token) {
        this(correlationId, title, display, keyProvider, token, 500);
    }

    public FinderColumn(final FinderId correlationId, final String title, final Display display, final ProvidesKey keyProvider, String token, int pageSize) {
        this.finderId = correlationId;
        this.title = title;
        this.display = display;
        this.keyProvider = keyProvider;

        // RBAC related
        this.token = token;
        this.id = Document.get().createUniqueId();

        selectionModel = new SingleSelectionModel<T>(keyProvider);

        cellTable = new CellTable<T>(pageSize, DefaultCellTable.DEFAULT_CELL_TABLE_RESOURCES , keyProvider);
        cellTable.setStyleName("navigation-cell-table");
        cellTable.getElement().setAttribute("style", "border:none!important");
        cellTable.setLoadingIndicator(new HTML());
        cellTable.setEmptyTableWidget(new HTML("<div class='empty-finder-column'>No Items!</div>"));

        this.dataProvider = new ListDataProvider<>(keyProvider);
        dataProvider.addDataDisplay(cellTable);

        Column<T, SafeHtml> titleColumn = new Column<T, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(T data) {

                SafeHtml title = display.render("navigation-column-item", data);
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendHtmlConstant("<div class='row-level' style='position:relative'>");
                sb.append(title);


                int offset1 = display.isFolder(data) ? 10 : 5;
                int offset2 = display.isFolder(data) ? 5 : 0;

                if(accessibleMenuItems.size()>0) {
                    sb.appendHtmlConstant("<span class='nav-menu' style='position:absolute; top:5px; right:"+offset1+"px'>");
                    sb.appendHtmlConstant("<div class='btn-group'>");

                    sb.appendHtmlConstant("<button action='default' class='btn' type='button' tabindex=\"-1\">");
                    if (data != null) {
                        sb.appendEscaped(accessibleMenuItems.get(0).render(data));
                    }
                    sb.appendHtmlConstant("</button>");

                    if(accessibleMenuItems.size()>1) {
                        sb.appendHtmlConstant("<button action='menu' class='btn dropdown-toggle' type='button' tabindex=\"-1\">");
                        sb.appendHtmlConstant("<span><i class='icon-caret-down'></i></span>");
                        sb.appendHtmlConstant("</button>");
                    }

                    sb.appendHtmlConstant("</div>");
                    sb.appendHtmlConstant("</span>");
                }

                if(display.isFolder(data)) {
                    sb.appendHtmlConstant("<span style='position:absolute; top:5px; right:"+offset2+"px'>");
                    sb.appendHtmlConstant("<i class='icon-angle-right row-icon' style='vertical-align:middle'></i>");
                    sb.appendHtmlConstant("</span>");
                }

                return sb.toSafeHtml();
            }
        };


        cellTable.addColumn(titleColumn);

        // width constraints, overflow, etc
        cellTable.getElement().getStyle().setTableLayout(Style.TableLayout.FIXED);
        cellTable.setColumnWidth(titleColumn, 100, Style.Unit.PCT);

        cellTable.setSelectionModel(selectionModel);

        // visibility of the context menu column
        // See HAL-738: We keep this in case we change our minds, but for now the menu is implicitly enabled

        /*cellTable.addRowHoverHandler(new RowHoverEvent.Handler() {
            @Override
            public void onRowHover(RowHoverEvent event) {
                TableRowElement hoveringRow = event.getHoveringRow();

                // skip empty menus
                if(accessibleMenuItems.size()==0) return;

                if(event.isUnHover()) {
                    hoveringRow.removeClassName("nav-hover");
                }
                else
                {
                    hoveringRow.addClassName("nav-hover");
                }

            }
        });*/

        // In the beginning we used to have multiple columns, which lead to this implementation
        // But now with a single column this part might be moved the SelectionHandler
        // TODO: Revisit this idea...
        cellTable.addCellPreviewHandler(new CellPreviewEvent.Handler<T>() {
            @Override
            public void onCellPreview(final CellPreviewEvent<T> event) {
                boolean isClick = CLICK.equals(event.getNativeEvent().getType());
                if (isClick && 0 == event.getColumn()) {

                    // preview
                    triggerPreviewEvent();

                    // update breadcrumb navigation
                    triggerBreadcrumbEvent(true);

                    event.getNativeEvent().preventDefault();
                    final Element element = Element.as(event.getNativeEvent().getEventTarget());
                    ButtonRef ref = resolveActionAttribute(element);
                    if ("default".equals(ref.action)) {
                        accessibleMenuItems.get(0).getCommand().executeOn(event.getValue());
                    } else if ("menu".equals(ref.action)) {
                        openContextMenu(ref.element, event.getValue());
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

                triggerBreadcrumbEvent(false);

                // skip empty menus
                if (accessibleMenuItems.size() == 0) return;

                // toggle row level tools
                toggleRowLevelTools(() -> selectionModel.getSelectedObject() == null);
            }
        });

        // listen on navigation events to prevent Preview race conditions
        Console.MODULES.getEventBus().addHandler(LockInteractionEvent.getType(), e -> {
            FinderColumn.this.previewLock = e.shouldLock();
        });
    }

    private ButtonRef resolveActionAttribute(Element element) {
        if(!element.hasAttribute("action"))
        {
            if(element.hasParentElement())
                return resolveActionAttribute(element.getParentElement());
            else
                new ButtonRef("unresolved", element);
        }
        return new ButtonRef(element.getAttribute("action"), element);
    }

    class ButtonRef  {

        public ButtonRef(String action, Element element) {
            this.action = action;
            this.element = element;
        }

        String action;
        Element element;
    }

    @FunctionalInterface
    public interface Disclose {
      boolean isDisclosed();
    }

    private void toggleRowLevelTools(Disclose fn) {

        int row = cellTable.getKeyboardSelectedRow();
        if (row < cellTable.getRowCount()) {
            TableRowElement rowElement = cellTable.getRowElement(row);
            if (rowElement != null) {
                if(!fn.isDisclosed()) {
                    rowElement.addClassName("nav-hover");
                } else {
                    rowElement.removeClassName("nav-hover");
                }
            }
        }
    }


    /**
     * Central point for security context changes
     * This will be called when:
     *
     * a) the widget is attached (default)
     * b) the security context changes (i.e. scoped roles)
     *
     */
    private void applySecurity(final SecurityContext securityContext, boolean update) {

        // System.out.println("<< Process SecurityContext on column "+title+": "+securityContext+">>");

        // calculate accessible menu items
        filterNonPrivilegeOperations(securityContext, accessibleTopMenuItems, topMenuItems);
        filterNonPrivilegeOperations(securityContext,accessibleMenuItems, menuItems);

        // the top menu is build here
        if(!plain)
            buildTopMenu(headerMenu);

        // the row level menu is build when the celltable is filled
        // hence we need to refresh it

        toggleRowLevelTools(() -> true); // hide it
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                dataProvider.refresh();
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        toggleRowLevelTools(() -> selectionModel.getSelectedObject() == null); // show when selected
                    }
                });
            }
        });

    }

    private void filterNonPrivilegeOperations(SecurityContext securityContext, List<MenuDelegate> target, MenuDelegate[] source) {

        target.clear();
        for (MenuDelegate menuItem : source) {

            boolean writePrivilege = menuItem.hasOperationAddress() ?
                    securityContext.getOperationPriviledge(menuItem.getResource(), menuItem.getOp()).isGranted() :
                    securityContext.getWritePriviledge().isGranted();

            // Role.Operation will be filtered depending on the permission
            if(MenuDelegate.Role.Operation == menuItem.getRole()
                    && writePrivilege)
            {
                target.add(menuItem);
            }

            // Role.Navigation will not be filtered at all
            else if(MenuDelegate.Role.Navigation == menuItem.getRole())
            {
                target.add(menuItem);
            }

        }
    }

    public FinderColumn<T> setShowSize(boolean b) {
        this.showSize = b;
        return this;
    }

    private void triggerPreviewEvent() {

        if(!hasSelectedItem()) return;

        final T selectedObject = selectionModel.getSelectedObject();

        // different selected object trigger preview
        // preview and place management sometimes compete, hence the deferred event
        // see also DefaultPlacemanager#doRevealPlace() when the callback returns
        Scheduler.get().scheduleFixedDelay(() -> {

            boolean reschedulePreviewEvents = FinderColumn.this.previewLock;

            if(!reschedulePreviewEvents) {
                // preview events
                PlaceManager placeManager = Console.MODULES.getPlaceManager();
                if (selectedObject != null) {

                    previewFactory.createPreview(selectedObject, new SimpleCallback<SafeHtml>() {
                        @Override
                        public void onSuccess(SafeHtml content) {
                            PreviewEvent.fire(placeManager, content);
                        }
                    });
                }
            }

            if(reschedulePreviewEvents)
                System.out.println("Preview event will be re-scheduled: "+FinderColumn.this.title);

            return reschedulePreviewEvents; // if locked this fn get re-scheduled

        }, 100);

    }

    private void triggerBreadcrumbEvent(boolean isMenuEvent) {

        PlaceManager placeManager = Console.MODULES.getPlaceManager();
        final T selectedObject = selectionModel.getSelectedObject();

        if(selectedObject!=null) {

            // delegate to value provider if given, otherwise the keyprovider will do fine
            String value = valueProvider!=null ? valueProvider.get(selectedObject) :
                    String.valueOf(keyProvider.getKey(selectedObject));

            BreadcrumbEvent.fire(placeManager, finderId, title, true, value, isMenuEvent);
        }
        else
        {
            BreadcrumbEvent.fire(placeManager, finderId, title, false, "", isMenuEvent);
        }

    }

    private void openTopContextMenu(Element anchor, final NativeEvent event) {

        Element el = Element.as(event.getEventTarget());
        //Element anchor = el.getParentElement().getParentElement();

        final PopupPanel popupPanel = new PopupPanel(true);
        final MenuBar popupMenuBar = new MenuBar(true);
        popupMenuBar.setStyleName("dropdown-menu");

        int i=0;
        for (final MenuDelegate menuitem : accessibleTopMenuItems) {

            if(i>0) {     // skip the "default" action
                MenuItem cmd = new MenuItem(menuitem.getTitle(), true, new Command() {

                    @Override
                    public void execute() {
                        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {
                                menuitem.getCommand().executeOn(null);
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
        int top = anchor.getAbsoluteTop() + 30;

        popupPanel.setPopupPosition(left, top);
        popupPanel.setAutoHideEnabled(true);
        popupPanel.show();
    }

    private void openContextMenu(Element el, final T object) {

        Element anchor = el.getParentElement();

        final PopupPanel popupPanel = new PopupPanel(true);
        final MenuBar popupMenuBar = new MenuBar(true);
        popupMenuBar.setStyleName("dropdown-menu");

        int i=0;
        for (final MenuDelegate menuitem : accessibleMenuItems) {

            if(i>0) {     // skip the "default" action
                MenuItem cmd = new MenuItem(menuitem.render(object), true, new Command() {

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
        int left = anchor.getAbsoluteLeft()+5;
        int top = anchor.getAbsoluteTop() + 22;

        popupPanel.setPopupPosition(left, top);
        popupPanel.setAutoHideEnabled(true);
        if(tooltip!=null) tooltip.cancel();
        popupPanel.show();
    }

    /**
     * the top level menu items (part of the header)
     * @param items
     * @return
     */
    public FinderColumn<T> setTopMenuItems(MenuDelegate<T>... items) {
        this.topMenuItems = items;
        return this;
    }

    /**
     * row level menu items. the first item act's as the default action.
     * @param items
     * @return
     */
    public FinderColumn<T> setMenuItems(MenuDelegate<T>... items) {
        this.menuItems = items;
        return this;
    }


    /**
     * renders the column without a header
     * @param plain
     * @return
     */
    public FinderColumn<T> setPlain(boolean plain) {
        this.plain = plain;
        return this;
    }

    /**
     * factory for content previews
     * @param previewFactory
     * @return
     */
    public FinderColumn<T> setPreviewFactory(PreviewFactory<T> previewFactory) {
        this.previewFactory = previewFactory;
        return this;
    }

    public FinderColumn<T> setTooltipDisplay(TooltipDisplay<T> tooltipDisplay) {
        this.tooltipDisplay = tooltipDisplay;
        return this;
    }

    public FinderColumn<T> setFilter(ColumnFilter.Predicate<T> filterValue) {
        this.filter = new ColumnFilter(this.selectionModel, cellTable, filterValue);
        return this;
    }

    /**
     * provides the value part of a key/value breadcrumb tuple.
     * if this is not given (default) then the column title will be used as the value.
     * @param valueProvider
     * @return
     */
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

        // tooltips (lazy init because they are optional)
        if(this.tooltipDisplay!=null) {
            tooltip = new FinderTooltip(new FinderTooltip.Context<T>() {
                @Override
                public SafeHtml render(T item) {
                    return tooltipDisplay.render(item);
                }
            });

            cellTable.addRowHoverHandler(new RowHoverEvent.Handler() {
                @Override
                public void onRowHover(RowHoverEvent event) {
                    TableRowElement hoveringRow = event.getHoveringRow();

                    T item = cellTable.getVisibleItem(hoveringRow.getRowIndex());

                    if (event.isUnHover()) {
                        tooltip.cancel();
                    } else {
                        tooltip.prepare(hoveringRow, item);
                    }

                }
            });
        }

        layout = new LayoutPanel() {
            @Override
            protected void onLoad() {

                 // security context state changes
                SECURITY_SERVICE.registerWidget(id, FinderColumn.this);
            }

            @Override
            protected void onUnload() {
                SECURITY_SERVICE.unregisterWidget(id);
            }
        };



        layout.getElement().setId(id);

        layout.addStyleName("navigation-column");
        layout.getElement().setId(id);   // RBAC
        layout.getElement().setAttribute("data-column", title);

        if(!plain) {     // including the header

            header = new LayoutPanel();
            header.addStyleName("fill-layout-width");
            header.addStyleName("finder-col-header");

            headerTitle = new HTML();
            updateTitle();
            headerTitle.addStyleName("finder-col-title");
            header.add(headerTitle);
            ScrollPanel column = new ScrollPanel(cellTable);
            column.getElement().getStyle().setOverflowX(Style.Overflow.HIDDEN);

            headerMenu = new LayoutPanel();
            headerMenu.setStyleName("fill-layout");
            headerMenu.addStyleName("header-menu");
            header.add(headerMenu); // fill we be filled through #applySecurity()

            header.setWidgetLeftWidth(headerTitle, 0, Style.Unit.PX, 60, Style.Unit.PCT);
            header.setWidgetRightWidth(headerMenu, 0, Style.Unit.PX, 40, Style.Unit.PCT);

            header.setWidgetTopHeight(headerTitle, 1, Style.Unit.PX, 38, Style.Unit.PX);
            header.setWidgetTopHeight(headerMenu, 1, Style.Unit.PX, 38, Style.Unit.PX);

            layout.add(header);
            layout.add(column);

            int offset =41;
            Widget filterWidget = filter!=null ? filter.asWidget() : null;
            int filterHeight = 45;
            if(filter!=null)
            {
                layout.add(filterWidget);
                offset += filterHeight;
            }

            layout.setWidgetTopHeight(header, 0, Style.Unit.PX, 40, Style.Unit.PX);
            if(filter!=null)
                layout.setWidgetTopHeight(filterWidget, 41, Style.Unit.PX, filterHeight, Style.Unit.PX);

            layout.setWidgetTopHeight(column, offset, Style.Unit.PX, 95, Style.Unit.PCT);

        }
        else            // embedded mode, w/o header
        {
            ScrollPanel nav = new ScrollPanel(cellTable);
            nav.getElement().getStyle().setOverflowX(Style.Overflow.HIDDEN);

            layout.add(nav);
            layout.setWidgetTopHeight(nav, 0, Style.Unit.PX, 100, Style.Unit.PCT);

        }

        return layout;
    }

    private void updateTitle() {

        String label = title;
        if(!GWT.isScript())
            label = "<span title='"+token+"'>"+title+"</span>";

        if(showSize)
            headerTitle.setHTML(label+" ("+cellTable.getRowCount()+")");
        else
            headerTitle.setHTML(label);

    };

    private void buildTopMenu(LayoutPanel container) {

        String groupId = HTMLPanel.createUniqueId();

        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        sb.appendHtmlConstant("<div class='nav-headerMenu top-level'>");
        sb.appendHtmlConstant("<div id="+groupId+" class='btn-group' style='float:right;padding-right:10px;padding-top:8px'>");
        sb.appendHtmlConstant("</div>");
        sb.appendHtmlConstant("</div>");

        HTMLPanel headerMenu = new HTMLPanel(sb.toSafeHtml());
        headerMenu.setStyleName("fill-layout");

        container.clear();
        container.add(headerMenu);

        if(accessibleTopMenuItems.size()>0) {

            container.getElement().removeClassName(RBAC_SUPPRESSED);

            MenuDelegate firstItem = accessibleTopMenuItems.get(0);
            HTML item = new HTML(firstItem.getTitle());
            item.setStyleName("btn");
            item.addStyleName("primary");
            item.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    firstItem.getCommand().executeOn(null);
                }
            });
            headerMenu.add(item, groupId);

            // remaining menu items move into dropdown
            if(accessibleTopMenuItems.size()>1)
            {

                HTML dropDown = new HTML("<span><i class='icon-caret-down'></i></span>");
                dropDown.setStyleName("btn");
                dropDown.addStyleName("dropdown-toggle");
                dropDown.addStyleName("primary");
                dropDown.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {

                        openTopContextMenu(headerMenu.getElementById(groupId), event.getNativeEvent());
                    }
                });
                headerMenu.add(dropDown, groupId);

            }

        }
        else {
            // no items
            container.getElement().addClassName(RBAC_SUPPRESSED);
        }

    }

    public void updateFrom(List<T> records) {
        updateFrom(records, false);
    }

    public void updateFrom(final List<T> records, final boolean selectDefault) {

        if(filter!=null) filter.clear();
        selectionModel.clear();
        /*cellTable.setRowCount(records.size(), true);
        cellTable.setRowData(0, records);*/

        dataProvider.setList(records);

        if(!plain) {
            updateTitle();
        }

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

    /**
     * renderer for the column content
     * @param <T>
     */
    public interface Display<T> {
        boolean isFolder(T data);
        SafeHtml render(String baseCss, T data);
        String rowCss(T data);
    }

    public interface TooltipDisplay<T> {
        SafeHtml render(T data);
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

    // SecurityContextAware widget contract


    @Override
    public void setFilter(String resourceAddress) {
           // not used atm
    }

    @Override
    public String getFilter() {
        return null; // not used atm
    }

    @Override
    public void onSecurityContextChanged() {

        SecurityContext securityContext = SECURITY_SERVICE.getSecurityContext(token);
        //System.out.println("<< Process SecurityContext on finder column "+id+": "+securityContext+">>");
        applySecurity(securityContext, true);
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public HandlerRegistration addAttachHandler(AttachEvent.Handler handler) {
        return layout.addAttachHandler(handler);
    }

    @Override
    public boolean isAttached() {
        return layout.isAttached();
    }

    @Override
    public void fireEvent(GwtEvent<?> gwtEvent) {
        Console.getPlaceManager().fireEvent(gwtEvent);
    }
}
