package org.jboss.as.console.client.tools;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.mbui.widgets.AddressUtils;
import org.jboss.as.console.mbui.widgets.ModelNodeCellTable;
import org.jboss.as.console.mbui.widgets.ModelNodeColumn;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.ComboBox;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Heiko Braun
 * @since 24/07/14
 */
public class ChildView {

    private ModelNode currentAddress;

    private BrowserNavigation presenter;
    private ModelNodeCellTable table;
    private ListDataProvider<ModelNode> dataProvider;
    private SingleSelectionModel<ModelNode> selectionModel;
    private HTML header;
    private ToolStrip tools;
    private BrowserView.ChildInformation childInformation;

    Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton("Add", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                // onAdd singleton we need to request the specific type to be added

                int i=1;
                List<Property> addressTuple = currentAddress.asPropertyList();
                final ModelNode addressPrefix = new ModelNode();
                Property denominator = null;
                for(Property tuple : addressTuple)
                {
                    if(i==addressTuple.size())
                    {
                        denominator = tuple;
                        break;
                    }
                    else
                    {
                        addressPrefix.add(tuple.getName(), tuple.getValue());
                    }

                    i++;
                }

                if(childInformation.hasSingletons())
                {
                    final String denominatorType = denominator.getName();
                    SingletonDialog dialog = new SingletonDialog(
                            childInformation.getSingletons().get(denominatorType),
                            new SimpleCallback<String>() {

                                @Override
                                public void onSuccess(String result) {

                                    addressPrefix.add(denominatorType, result);
                                    presenter.onPrepareAddChildResource(addressPrefix, true);
                                }
                            }
                    );
                    dialog.setWidth(320);
                    dialog.setHeight(240);
                    dialog.center();

                }
                else
                {
                    presenter.onPrepareAddChildResource(currentAddress, false);
                }

            }
        }));

        final ToolButton remove = new ToolButton("Remove", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ModelNode selection = selectionModel.getSelectedObject();
                if (selection != null)
                    presenter.onRemoveChildResource(currentAddress, selection);
            }
        });
        tools.addToolButtonRight(remove);
        remove.setEnabled(false);

        table = new ModelNodeCellTable(12);
        table.addColumn(new ModelNodeColumn(new ModelNodeColumn.ValueAdapter() {
            @Override
            public String getValue(ModelNode model) {
                return model.asString();
            }
        }), "Child Resource" );


        Column<ModelNode, ModelNode> option = new Column<ModelNode, ModelNode>(
                new ViewLinkCell<ModelNode>(Console.CONSTANTS.common_label_view(), new ActionCell.Delegate<ModelNode>() {
                    @Override
                    public void execute(ModelNode selection) {
                        presenter.onViewChild(currentAddress, selection.asString());
                    }
                })
        ) {
            @Override
            public ModelNode getValue(ModelNode model) {
                return model;
            }
        };
        table.addColumn(option, "Option");

        dataProvider = new ListDataProvider<ModelNode>();
        dataProvider.addDataDisplay(table);


        selectionModel = new SingleSelectionModel<ModelNode>();
        table.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                ModelNode selection = selectionModel.getSelectedObject();
                remove.setEnabled(selection!=null);
            }
        });

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        // -----

        header = new HTML();

        layout.add(header);
        layout.add(tools);
        layout.add(table);
        layout.add(pager);
        return layout;

    }

    public void setPresenter(BrowserNavigation presenter) {
        this.presenter = presenter;
    }

    public void setChildren(ModelNode address, List<ModelNode> modelNodes, BrowserView.ChildInformation childInformation) {

        this.currentAddress = address;
        this.childInformation = childInformation;

        boolean hasSingletons = childInformation.hasSingletons();
        String text = hasSingletons ? "Singleton Child Resources" : "Child Resources";
        header.setHTML("<h2 class='homepage-secondary-header'>"+text+" ("+modelNodes.size()+")</h2>");
        dataProvider.setList(modelNodes);


    }

    /**
     * Callback for creation of add dialogs.
     * Will be invoked once the presenter has loaded the resource description.
     * @param address
     * @param isSingleton
     * @param securityContext
     * @param description
     */
    public void showAddDialog(final ModelNode address, boolean isSingleton, SecurityContext securityContext, ModelNode description) {

        String resourceAddress = AddressUtils.asKey(address, isSingleton);
        if(securityContext.getOperationPriviledge(resourceAddress, "add").isGranted()) {
            _showAddDialog(address, securityContext, description);
        }
        else
        {
            Feedback.alert("Authorisation Required", "You seem to lack permissions to add new resources!");
        }

    }

    private void _showAddDialog(final ModelNode address, SecurityContext securityContext, ModelNode description) {
        List<Property> tuples = address.asPropertyList();
        String type = "";
        if(tuples.size()>0)
        {
            type = tuples.get(tuples.size()-1).getName();
        }

        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setCreateMode(true)
                .setResourceDescription(description)
                .setSecurityContext(securityContext);

        ModelNodeFormBuilder.FormAssets assets = builder.build();

        final ModelNodeForm form = assets.getForm();
        form.setEnabled(true);

        if(form.hasWritableAttributes()) {
            final DefaultWindow window = new DefaultWindow("Create Resource '" + type + "'");
            window.addStyleName("browser-view");

            DialogueOptions options = new DialogueOptions(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    // save
                    FormValidation validation = form.validate();
                    if(!validation.hasErrors())
                    {
                        presenter.onAddChildResource(address, form.getUpdatedEntity());
                        window.hide();
                    }
                }
            }, new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    // cancel
                    window.hide();
                }
            });

            VerticalPanel layout = new VerticalPanel();
            layout.setStyleName("fill-layout-width");
            ModelNode opDescription = description.get("operations").get("add").get("description");
            ContentDescription text = new ContentDescription(opDescription.asString());
            layout.add(text);
            layout.add(form.asWidget());

            WindowContentBuilder content = new WindowContentBuilder(layout, options);
            window.trapWidget(content.build());
            window.setGlassEnabled(true);
            window.setWidth(480);
            window.setHeight(360);
            window.center();
        }
        else
        {
            // no writable attributes
            Feedback.alert("Cannot create child resource", "There are no configurable attributes on resources " + address);
        }
    }

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    class SingletonDialog extends DefaultWindow {

        private final CellList<String> cellList;
        private final SingleSelectionModel<String> selectionModel;


        public SingletonDialog(Set<String> singletonTypes, final SimpleCallback callback) {
            super("Select Resource type");

            // Create a CellList that uses the cell.
            cellList = new CellList<String>(new TextCell()
            {
                @Override
                public void render(Context context, String data, SafeHtmlBuilder sb) {
                    String cssName = (context.getIndex() %2 > 0) ? "combobox-item combobox-item-odd" : "combobox-item";

                    if(data.equals(selectionModel.getSelectedObject()))
                        cssName+=" combobox-item-selected";

                    sb.append(TEMPLATE.item(cssName, data));
                }

            });

            cellList.setStyleName("fill-layout-width");
            selectionModel = new SingleSelectionModel<String>();

            cellList.setSelectionModel(selectionModel);
            cellList.setRowCount(singletonTypes.size(), true);
            ArrayList<String> values = new ArrayList<String>(singletonTypes);
            cellList.setRowData(0, values);

            selectionModel.setSelected(values.get(0), true);

            VerticalPanel panel = new VerticalPanel();
            panel.setStyleName("fill-layout-width");
            panel.add(cellList.asWidget());
            Widget widget = new WindowContentBuilder(panel, new DialogueOptions(
                    new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {

                            SingletonDialog.this.hide();
                            callback.onSuccess(selectionModel.getSelectedObject());
                        }
                    },
                    new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            SingletonDialog.this.hide();
                        }
                    }
            )).build();

            setWidget(widget);

        }

        public String getSelectedValue() {
            return selectionModel.getSelectedObject();
        }
    }
}
