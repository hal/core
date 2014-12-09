package org.jboss.as.console.client.tools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PopupViewImpl;
import org.jboss.as.console.client.widgets.DefaultSplitLayoutPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.common.DefaultButton;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 6/15/12
 */
public class BrowserView extends PopupViewImpl implements BrowserPresenter.MyView,
        BrowserNavigation {

    private static final String DEFAULT_ROOT = "Management Model";
    private BrowserPresenter presenter;
    private SplitLayoutPanel layout;

    private DefaultWindow window;
    private VerticalPanel treeContainer;
    private Tree tree;

    private FormView formView;
    private DescriptionView descView;
    private ChildView childView;
    private SecurityView securityView;

    private PageHeader nodeHeader;
    private DeckPanel deck;
    private String currentRootKey;
    private ModelNode addressOffset;
    private Button filter;

    private VerticalPanel offsetDisplay;
    private TabPanel tabs;

    @Inject
    public BrowserView(EventBus eventBus) {
        super(eventBus);
        createWidget();
    }

    @Override
    public void setPresenter(BrowserPresenter presenter) {
        this.presenter = presenter;
        this.formView.setPresenter(presenter);
        this.childView.setPresenter(this);
        this.nodeHeader.setPresenter(this);
    }

    @Override
    public void center() {

        int width = Window.getClientWidth() - 50;
        int height = Window.getClientHeight() - 50;
        window.hide();
        window.setPopupPosition(25, 25);
        window.setWidth(width+"px");
        window.setHeight(height+"px");
    }

    @Override
    public Widget asWidget() {
        return window;
    }

    private void createWidget() {
        window = new DefaultWindow("Management Model View");
        window.addStyleName("model-browser-window");

        window.setGlassEnabled(true);

        tree = new Tree(ModelBrowserResources.INSTANCE);

        tree.getElement().addClassName("browser-tree");
        tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection(SelectionEvent<TreeItem> selection) {

                if(!tree.getItem(0).equals(selection.getSelectedItem()))
                    tree.getItem(0).setSelected(false); // clear initial selection

                onItemSelected(selection.getSelectedItem());
            }
        });

        layout = new DefaultSplitLayoutPanel(2);
        layout.addStyleName("model-browser");
        treeContainer = new VerticalPanel();
        treeContainer.setStyleName("fill-layout");
        treeContainer.setStyleName("browser-view-nav");

        HorizontalPanel tools = new HorizontalPanel();
        tools.getElement().setAttribute("style", "margin-left: 10px;");

        Button refresh = new DefaultButton("<i class='icon-undo'></i>");
        refresh.getElement().setAttribute("title", "Refresh Model");
        refresh.addClickHandler(
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        presenter.onPinTreeSelection(null);
                    }
                }
        );


        filter = new DefaultButton("<i class='icon-filter'></i>");
        filter.getElement().setAttribute("title", "Filter Subtree");
        filter.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                TreeItem selectedItem = tree.getSelectedItem();
                if (selectedItem != null && selectedItem instanceof ModelTreeItem) {
                    ModelTreeItem modelItem = (ModelTreeItem) selectedItem;
                    presenter.onPinTreeSelection(modelItem.getAddress());
                }
            }
        });

        tools.add(refresh);
        tools.add(filter);

        // displays the remainder of set tree thats not visible
        offsetDisplay = new VerticalPanel();
        treeContainer.add(offsetDisplay);
        treeContainer.add(tree);

       /* treeContainer.add(new HTML("<hr/>"));

        treeContainer.add(new HTML("<div style='color:#cccccc;margin-left:12px'><i class=\"icon-keyboard\"></i><br/>" +
                "- Up arrow - select upper item<br/>" +
                "- Down arrow - select lower item<br/>" +
                "- Right arrow - open item<br/>" +
                "- Left arrow - close item</div>"));
*/
        ScrollPanel scroll = new ScrollPanel(treeContainer);

        LayoutPanel lhs = new LayoutPanel();
        lhs.setStyleName("fill-layout");

        lhs.add(tools);
        lhs.add(scroll);

        lhs.setWidgetTopHeight(tools, 10, Style.Unit.PX, 30, Style.Unit.PX);
        lhs.setWidgetTopHeight(scroll, 41, Style.Unit.PX, 100, Style.Unit.PCT);

        layout.addWest(lhs, 300);

        formView = new FormView();
        descView = new DescriptionView();
        nodeHeader = new PageHeader();
        childView = new ChildView();
        securityView = new SecurityView();

        tabs = new TabPanel();
        tabs.setStyleName("default-tabpanel");
        tabs.addStyleName("browser-view");
        tabs.getElement().setAttribute("style", "margin-top:15px;");

        tabs.add(descView.asWidget(), "Description");
        tabs.add(formView.asWidget(), "Data");
        if(!GWT.isScript())
        {
            tabs.add(securityView.asWidget(), "Access Control");
        }

        tabs.selectTab(0);

        // --

        deck = new DeckPanel();
        deck.setStyleName("fill-layout");
        deck.add(childView.asWidget());
        deck.add(tabs);
        deck.add(new HTML("")); // loading page

        deck.showWidget(2);

        VerticalPanel contentPanel = new VerticalPanel();
        contentPanel.setStyleName("rhs-content-panel");

        Widget headerWidget = nodeHeader.asWidget();
        contentPanel.add(headerWidget);
        contentPanel.add(deck);

        ScrollPanel contentScroll = new ScrollPanel(contentPanel);
        layout.add(contentScroll);

        tree.addOpenHandler(new OpenHandler<TreeItem>() {
            @Override
            public void onOpen(OpenEvent<TreeItem> event) {
                onItemOpenend(event.getTarget());
            }
        });

        window.setWidget(layout);

    }

    // ------------------------

    /**
     * When a tree item is clicked we load the resource.
     *
     * @param treeItem
     */
    private void onItemSelected(TreeItem treeItem) {

        treeItem.getElement().focus();
        final LinkedList<String> path = resolvePath(treeItem);

        formView.clearDisplay();
        descView.clearDisplay();

        ModelNode address = toAddress(path);
        ModelNode displayAddress = address.clone();


        if(path.size()%2==0) {
            // addressable resources
            presenter.readResource(address);
            toggleEditor(true);
            filter.setEnabled(true);

        }
        else {
            toggleEditor(false);
            // display tweaks
            displayAddress.add(path.getLast(), "*");

            // force loading of children upon selection
            loadChildren((ModelTreeItem)treeItem, false);
            filter.setEnabled(false);
        }

        nodeHeader.updateDescription(displayAddress);
    }

    /**
     * When a tree item is opened, we load the children.
     *
     * @param treeItem
     */
    private void onItemOpenend(TreeItem treeItem) {

        loadChildren((ModelTreeItem)treeItem, true);
    }

    /**
     * Child selection within editor components (outside left hand tree)
     * @param address
     * @param childName
     */
    @Override
    public void onViewChild(ModelNode address, String childName) {
        TreeItem rootNode = findTreeItem(tree, address);
        TreeItem childNode = null;
        for(int i=0; i<rootNode.getChildCount(); i++)
        {
            TreeItem candidate = rootNode.getChild(i);
            if(childName.equals(candidate.getText()))
            {
                childNode = candidate;
                break;
            }
        }

        if(null==childNode)
            throw new IllegalArgumentException("No such child "+ childName + " on "+ address.toString());


        // deselect previous
        tree.setSelectedItem(null, false);

        // select next
        tree.setSelectedItem(childNode, false);
        tree.ensureSelectedItemVisible();

        onItemSelected(childNode);
    }

    private void toggleEditor(final boolean showEditor) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                deck.showWidget( showEditor ? 1 : 0 );
            }
        });
    }

    private void loadChildren(ModelTreeItem treeItem,  boolean performOnTypes) {
        boolean hasChildren = treeItem.getChildCount() > 0;

        // placeholder identify child subtrees that have not been loaded
        boolean notHasBeenLoaded = treeItem.getChild(0) instanceof PlaceholderItem;

        if(hasChildren && notHasBeenLoaded)
        {

            final List<String> path = resolvePath(treeItem.getChild(0));
            final ModelNode address = toAddress(path);

            if(path.size()%2==0) {
                presenter.readChildrenNames(address);
            }
            else {
                if(performOnTypes)
                    presenter.readChildrenTypes(address, false);
            }
        }
        else if(!notHasBeenLoaded)
        {
            // if it has been loaded before we need to update the child view
            // the data exists with the tree
            List<ModelNode> model = new ArrayList<ModelNode>(treeItem.getChildCount());
            boolean hasSingletons = false;
            for(int i=0; i<treeItem.getChildCount(); i++)
            {
                ModelTreeItem child = (ModelTreeItem)treeItem.getChild(i);
                hasSingletons = child.isSingleton(); // either all or none children are hasSingletons
                model.add(new ModelNode().set(child.getText()));
            }

            final List<String> path = resolvePath(treeItem);
            path.add("*");
            final ModelNode address = toAddress(path);

            ChildInformation childInformation = treeItem.getParentItem()!=null ?
                    ((ModelTreeItem) treeItem.getParentItem()).getChildInformation() : treeItem.getChildInformation();

            childView.setChildren(address, model, childInformation);
        }
    }


    // ------------------------

    public static ModelNode toAddress(List<String> path)
    {

        ModelNode address = new ModelNode();
        address.setEmptyList();

        if(path.size()<2) return address;

        for(int i=1; i<path.size();i+=2)
        {
            if(i%2!=0 )
                address.add(path.get(i-1), path.get(i));
            else
                address.add(path.get(i), "*");
        }

        return address;

    }

    /**
     * Update root node. Basicaly a refresh of the tree.
     *
     * @param address
     * @param modelNodes
     */
    public void updateRootTypes(ModelNode address, List<ModelNode> modelNodes) {

        deck.showWidget(0);
        tree.clear();
        descView.clearDisplay();
        formView.clearDisplay();
        offsetDisplay.clear();
        nodeHeader.updateDescription(address);

        // IMPORTANT: when pin down is active, we need to consider the offset to calculate the real address
        addressOffset = address;

        List<Property> offset = addressOffset.asPropertyList();
        if(offset.size()>0)
        {
            String parentName = offset.get(offset.size() - 1).getName();
            HTML parentTag = new HTML("<div class='gwt-ToggleButton gwt-ToggleButton-down' title='Remove Filter'>&nbsp;" + parentName + "&nbsp;<i class='icon-remove'></i></div>");
            parentTag.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    presenter.onPinTreeSelection(null);
                }
            });
            offsetDisplay.add(parentTag);
        }

        TreeItem rootItem = null;
        String rootTitle = null;
        String key = null;
        if(address.asList().isEmpty())
        {
            rootTitle = DEFAULT_ROOT;
            key = DEFAULT_ROOT;
        }
        else
        {
            List<ModelNode> tuples = address.asList();
            rootTitle = tuples.get(tuples.size() - 1).asProperty().getValue().asString();
            key = rootTitle;
        }

        SafeHtmlBuilder html = new SafeHtmlBuilder().appendHtmlConstant(rootTitle);
        rootItem = new ModelTreeItem(html.toSafeHtml(), key, address, false);
        tree.addItem(rootItem);


        deck.showWidget(1);
        rootItem.setSelected(true);

        currentRootKey = key;

        addChildrenTypes((ModelTreeItem)rootItem, modelNodes);
    }

    /**
     * Update sub parts of the tree
     * @param address
     * @param modelNodes
     */
    @Override
    public void updateChildrenTypes(ModelNode address, List<ModelNode> modelNodes) {

        TreeItem  rootItem = findTreeItem(tree, address);
        addChildrenTypes((ModelTreeItem)rootItem, modelNodes);

    }

    @Override
    public void updateChildrenNames(ModelNode address, List<ModelNode> modelNodes) {

        TreeItem rootItem = findTreeItem(tree, address);

        assert rootItem!=null : "unable to find matching tree item: "+address;

        // update the tree
        addChildrenNames((ModelTreeItem)rootItem, modelNodes);

        // update the append child panel
        // the parent of the current node contains the child info
        ChildInformation childInformation = ((ModelTreeItem) rootItem.getParentItem()).getChildInformation();
        childView.setChildren(address, modelNodes, childInformation);
    }

    @Override
    public void updateResource(ModelNode address, SecurityContext securityContext, ModelNode description, ModelNode resource) {

        // description
        nodeHeader.updateDescription(address,description);
        descView.updateDescription(address, description);

        // data
        final List<Property> tokens = address.asPropertyList();
        String name = tokens.isEmpty() ? DEFAULT_ROOT : tokens.get(tokens.size()-1).getValue().asString();

        formView.display(address, description, securityContext, new Property(name, resource));

        if(!GWT.isScript())
        {
            securityView.display(securityContext);
        }
    }

    private void addChildrenTypes(ModelTreeItem rootItem, List<ModelNode> modelNodes) {

        rootItem.removeItems();

        final ChildInformation childInformation = parseChildrenTypes(modelNodes);

        for(String child : childInformation.getNames())
        {
            final ModelNode address = getNodeAddress(rootItem, child);

            SafeHtmlBuilder html = new SafeHtmlBuilder();
            html.appendHtmlConstant("<i class='icon-folder-close-alt'></i>&nbsp;");
            html.appendHtmlConstant(child);
            TreeItem childItem = new ModelTreeItem(html.toSafeHtml(), child, address, childInformation.isSingleton(child));
            childItem.addItem(new PlaceholderItem());
            rootItem.addItem(childItem);
            rootItem.updateChildInfo(childInformation);
        }

        rootItem.setState(true);
    }

    private ChildInformation parseChildrenTypes(List<ModelNode> childrenTypes)
    {
        Set<String> names = new HashSet<>();
        Map<String, Set<String>> singletons = new HashMap<>();
        for(ModelNode child : childrenTypes)
        {
            String item = child.asString();
            int idx = item.indexOf("=");
            boolean isSingleton = idx != -1;
            String key = isSingleton ? item.substring(0, idx) : item;
            String value = isSingleton ? item.substring(idx+1, item.length()) : item;

            names.add(key);
            if(isSingleton) {
                if(null==singletons.get(key))
                    singletons.put(key, new HashSet<String>());

                singletons.get(key).add(value);
            }

        }

        return new ChildInformation(names, singletons);

    }

    private void addChildrenNames(ModelTreeItem rootItem, List<ModelNode> modelNodes) {

        rootItem.removeItems();

        if(modelNodes.isEmpty())
            rootItem.addItem(new PlaceholderItem());

        for(ModelNode child : modelNodes)
        {

            String childName = child.asString();
            boolean isSingleton = rootItem.hasChildInformation() ? rootItem.getChildInformation().isSingleton(childName) : false;
            final ModelNode address = getNodeAddress(rootItem, childName);

            SafeHtmlBuilder html = new SafeHtmlBuilder();

            String icon = isSingleton ? "icon-exclamation-sign" : "icon-file-text-alt";
            html.appendHtmlConstant("<i class='"+icon+"'></i>&nbsp;");
            html.appendHtmlConstant(childName);
            TreeItem childItem = new ModelTreeItem(html.toSafeHtml(), childName, address, isSingleton);
            childItem.addItem(new PlaceholderItem());
            rootItem.addItem(childItem);
        }
    }

    private ModelNode getNodeAddress(TreeItem rootItem, String childName) {
        final List<String> path = resolvePath(rootItem);
        if(path.size()%2==0)
        {
            // non-addressable resource
            path.add(childName);
            path.add("*");
        }
        else
        {
            // addressable resource
            path.add(childName);
        }

        return toAddress(path);
    }

    private TreeItem findTreeItem(Tree tree, ModelNode address) {
        List<Property> subAddress = getSubaddress(address);

        LinkedList<String> path = new LinkedList<String>();
        path.add(currentRootKey);

        for(Property prop : subAddress)
        {
            path.add(prop.getName());
            final String value = prop.getValue().asString();
            if(!"*".equals(value)) {
                path.add(value);
            }
        }

        final Iterator<String> iterator = path.iterator();

        TreeItem next = null;

        if(iterator.hasNext())
        {
            final String pathName = iterator.next();
            for(int i=0; i<tree.getItemCount(); i++)
            {
                if(tree.getItem(i).getText().equals(pathName))
                {
                    next = tree.getItem(i);
                    break;
                }
            }
        }

        if(next==null)
            return null;
        else if (!iterator.hasNext())
            return next;
        else
            return findTreeItem(next, iterator);
    }

    public List<Property> getSubaddress(ModelNode address) {
        // consider address offset
        List<Property> offsetTuple = addressOffset.asPropertyList();
        List<Property> addressTuple = address.asPropertyList();
        List<Property> subAddress  = new LinkedList<Property>();

        // sub tree calculation
        if(offsetTuple.size()>0)
        {
            int offsetIndex = (addressTuple.size() - offsetTuple.size());     // always >= 0
            subAddress = addressTuple.subList(-offsetIndex + addressTuple.size(), addressTuple.size());
        }
        else
        {
            subAddress = addressTuple;
        }

        return subAddress;
    }

    private static TreeItem findTreeItem(TreeItem tree, Iterator<String> iterator)
    {
        TreeItem next = null;
        if(iterator.hasNext())
        {
            final String pathName = iterator.next();
            for(int i=0; i<tree.getChildCount(); i++)
            {

                if(tree.getChild(i).getText().equals(pathName))
                {
                    next = tree.getChild(i);
                    break;
                }
            }
        }

        if(next==null)
            return null;
        else if (!iterator.hasNext())
            return next;
        else
            return findTreeItem(next, iterator);

    }

    public LinkedList<String> resolvePath(TreeItem item)
    {
        LinkedList<String> path = new LinkedList<String>();
        recurseToTop(item, path);


        // consider the offset
        List<Property> reverseAddress = addressOffset.asPropertyList();
        Collections.reverse(reverseAddress);

        for(Property tuple : reverseAddress)
        {
            // reverse order, because it will be prepended to the list
            path.addFirst(tuple.getValue().asString());
            path.addFirst(tuple.getName());
        }

        return path;
    }

    private void recurseToTop(TreeItem item, LinkedList<String> address)
    {
        if(item.getText().equals(currentRootKey)) return;

        address.addFirst(item.getText());

        if(item.getParentItem()!=null)
        {
            recurseToTop(item.getParentItem(), address);
        }
    }

    @Override
    public void onRemoveChildResource(ModelNode address, ModelNode selection) {
        presenter.onRemoveChildResource(address, selection);
    }

    @Override
    public void onPrepareAddChildResource(ModelNode address, boolean isSquatting) {
        presenter.onPrepareAddChildResource(address, isSquatting);
    }

    @Override
    public void onAddChildResource(ModelNode address, ModelNode resource) {
        presenter.onAddChildResource(address, resource);
    }

    // ------
    // supporting classes

    class PlaceholderItem extends TreeItem {

        PlaceholderItem() {
            super(new SafeHtmlBuilder().appendEscaped("*").toSafeHtml());
        }
    }

    class ModelTreeItem extends TreeItem {

        private String key;
        private ModelNode address;
        private boolean isSingleton = false;
        private ChildInformation childInformation;

        ModelTreeItem(SafeHtml html, String key, ModelNode address, boolean isSingleton) {
            super(html);
            this.key = key;
            this.address = address;
            this.isSingleton = isSingleton;
        }

        @Override
        public String getText() {
            return key;
        }

        public ModelNode getAddress() {
            return address;
        }

        public String getKey() {
            return key;
        }

        public boolean isSingleton() {
            return isSingleton;
        }

        public void updateChildInfo(ChildInformation childInformation) {

            this.childInformation = childInformation;
        }

        public ChildInformation getChildInformation() {
            return childInformation;
        }

        public boolean hasChildInformation() {
            return childInformation!=null;
        }
    }

    // TODO: The DMR euals for Property.class is not correct / or given at all
    private static boolean propertyEquals(Property a, Property b) {
        return (a.getName().equals(b.getName())) && (a.getValue().asString().equals(b.getValue().asString()));
    }

    @Override
    public void showAddDialog(ModelNode address, boolean isSingleton, SecurityContext securityContext, ModelNode desc) {
        childView.showAddDialog(address, isSingleton, securityContext, desc);
    }

    class ChildInformation {

        private final Set<String> names;
        private final Map<String, Set<String>> singletons;

        public ChildInformation(Set<String> names, Map<String, Set<String>> singletons) {

            this.names = names;
            this.singletons = singletons;
        }

        /*public ChildInformation() {
            this.names = new HashSet<>();
            this.singletons = new HashMap<String, Set<String>>();
        }*/

        public Set<String> getNames() {
            return names;
        }

        public Map<String, Set<String>> getSingletons() {
            return singletons;
        }

        public boolean isSingleton(String key) {
            if(!names.contains(key))
                throw new IllegalArgumentException("Invalid key "+key);
            return singletons.containsKey(key);
        }

        public boolean hasSingletons() {
            return singletons.size()>0;
        }
    }
}
