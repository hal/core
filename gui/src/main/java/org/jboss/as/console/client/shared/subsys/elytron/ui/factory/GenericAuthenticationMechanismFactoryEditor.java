/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.subsys.elytron.ui.factory;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.StringUtils;
import org.jboss.as.console.client.shared.subsys.elytron.store.AddListAttribute;
import org.jboss.as.console.client.shared.subsys.elytron.store.RemoveListAttribute;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class GenericAuthenticationMechanismFactoryEditor implements IsWidget {


    public static final String MECHANISM_CONFIGURATIONS = "mechanism-configurations";

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private Dispatcher circuit;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private String factoryName;
    private ModelNodeForm mechanismConfigurationForm;
    private VerticalPanel popupLayout = new VerticalPanel();
    private DefaultWindow mechanismConfigurationWindow;
    private AddressTemplate addressTemplate;
    private ToolButton addButton;
    private ToolButton removeButton;

    // button to hide the mechanism-configuration detail window
    // the cancel button is not displayed
    DialogueOptions popupDialogOptions = new DialogueOptions(Console.CONSTANTS.common_label_done(),

            // done
            new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    mechanismConfigurationWindow.hide();
                }
            },

            Console.CONSTANTS.common_label_cancel(),
            // cancel
            new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    mechanismConfigurationWindow.hide();
                }
            }
    );

    GenericAuthenticationMechanismFactoryEditor(final Dispatcher circuit, ResourceDescription resourceDescription,
            SecurityContext securityContext, final AddressTemplate addressTemplate) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.addressTemplate = addressTemplate;
        selectionModel = new SingleSelectionModel<>();

        // tweak to use ModelNodeFormBuilder automatic form generation
        this.resourceDescription = new ResourceDescription(resourceDescription.clone());
        // adds the mechanism-configuration to the request-properties of add operation
        ModelNode reqPropsDescription = this.resourceDescription.get("operations").get("add").get("request-properties");
        // adds the mechanism-configuration to the attribute list
        ModelNode attrPropsDescription = this.resourceDescription.get("attributes");
        ModelNode mechanismDescription = reqPropsDescription.get(MECHANISM_CONFIGURATIONS).get("value-type");
        reqPropsDescription.set(mechanismDescription);
        attrPropsDescription.set(mechanismDescription);
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-popupLayout-width");

        // table
        table = new DefaultCellTable<>(5);
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);

        // columns
        Column<ModelNode, String> mechanismNameColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                // as the mechanism-configuration  attribute is a list of attributes
                // none of them is required, so there is not a unique colum to show, so all defined attributes are
                // displayed, there is a "view" button that shows all attributes nicely formatted in a ModelNodeForm
                StringBuilder content = new StringBuilder();
                for (Property prop: node.asPropertyList()) {
                    content.append(prop.getName()).append(": ").append(prop.getValue().asString()).append(", ");
                }
                return StringUtils.shortenStringIfNecessary(content.toString().replaceAll(", $", ""), 120);
            }
        };

        Column<ModelNode, ModelNode> linkOpenDetailsColumn = new Column<ModelNode, ModelNode>(
                new ViewLinkCell<>(Console.CONSTANTS.common_label_view(), new ActionCell.Delegate<ModelNode>() {

                    @Override
                    public void execute(ModelNode selection) {
                        showMechanismConfigurationModal(selection);
                    }
                })
        ) {
            @Override
            public ModelNode getValue(ModelNode node) {
                return node;
            }
        };

        linkOpenDetailsColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(mechanismNameColumn, "");
        table.addColumn(linkOpenDetailsColumn, "Option");
        table.setColumnWidth(linkOpenDetailsColumn, 8, Style.Unit.PCT);

        panel.add(mainTableTools());
        panel.add(table);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);

        // ===================== mechanism configuration form popup
        popupLayout.setStyleName("window-content");

        // read-only form to show details of mechanism-configuration attribute
        ModelNodeFormBuilder.FormAssets mechanismConfigurationFormView = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setCreateMode(false)
                .unsorted()
                .setCreateNameAttribute(false)
                .setSecurityContext(securityContext)
                .exclude("mechanism-realm-configurations")
                .build();
        mechanismConfigurationForm = mechanismConfigurationFormView.getForm();

        popupDialogOptions.showCancel(false);
        Widget formWidget = mechanismConfigurationFormView.getForm().asWidget();
        popupLayout.add(formWidget);

        return panel;
    }

    private ToolStrip mainTableTools() {
        ToolStrip tools = new ToolStrip();
        addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> {

            ModelNodeFormBuilder.FormAssets addFormAssets = new ModelNodeFormBuilder()
                    .setResourceDescription(resourceDescription)
                    .setCreateMode(true)
                    .unsorted()
                    .setCreateNameAttribute(false)
                    .setSecurityContext(securityContext)
                    .exclude("mechanism-realm-configurations")
                    .build();
            addFormAssets.getForm().setEnabled(true);
            addFormAssets.getForm().addFormValidator((formItems, formValidation) -> {

                // at least one field is necessary to fill
                boolean allEmpty = true;
                for (FormItem formItem : formItems) {
                    if (!formItem.isUndefined()) {
                        allEmpty = false;
                        break;
                    }
                }
                if (allEmpty) {
                    formValidation.addError("mechanism-name");
                    FormItem mechanismNameFormItem = formItem(formItems, "mechanism-name");
                    mechanismNameFormItem.setErrMessage("At least one field must contain valid values.");
                    mechanismNameFormItem.setErroneous(true);
                }
            });

            DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Mechanism Configuration"));
            AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
                @Override
                public void onAdd(ModelNode payload) {
                    for (Property node: payload.asPropertyList()) {
                        // remove undefined attributes
                        if (!node.getValue().isDefined()) {
                            payload.remove(node.getName());
                        }

                    }
                    circuit.dispatch(new AddListAttribute(addressTemplate,
                            MECHANISM_CONFIGURATIONS,
                            factoryName,
                            payload));
                    dialog.hide();
                }

                @Override
                public void onCancel() {
                    dialog.hide();
                }
            };
            AddResourceDialog addDialog = new AddResourceDialog(addFormAssets, resourceDescription, callback);
            dialog.setWidth(480);
            dialog.setHeight(510);
            dialog.setWidget(addDialog);
            dialog.setGlassEnabled(true);
            dialog.center();
        });
        removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            final ModelNode selection = selectionModel.getSelectedObject();
            if (selection != null) {
                Feedback.confirm("Mechanism Configuration", Console.MESSAGES.deleteConfirm("Mechanism Configuration "  + selection.get("mechanism-name").asString()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveListAttribute(
                                        addressTemplate,
                                        factoryName,
                                        MECHANISM_CONFIGURATIONS,
                                        selection));
                            }
                        });
            }
        });

        addButton.setEnabled(false);
        removeButton.setEnabled(false);

        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(removeButton);
        return tools;
    }

    private void showMechanismConfigurationModal(final ModelNode selection) {

        mechanismConfigurationForm.editTransient(selection);
        Widget windowContent = new WindowContentBuilder(popupLayout, popupDialogOptions).build();

        mechanismConfigurationWindow = new DefaultWindow("Mechanism Configuration");
        mechanismConfigurationWindow.setWidth(480);
        mechanismConfigurationWindow.setHeight(430);
        mechanismConfigurationWindow.trapWidget(windowContent);
        mechanismConfigurationWindow.setGlassEnabled(true);
        mechanismConfigurationWindow.center();
    }

    public void update(Property prop) {
        factoryName = prop.getName();

        if (prop != null) {
            addButton.setEnabled(true);
            removeButton.setEnabled(true);
        } else {
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
        }

        if (prop.getValue().hasDefined(MECHANISM_CONFIGURATIONS)) {
            List<ModelNode> models = prop.getValue().get(MECHANISM_CONFIGURATIONS).asList();
            table.setRowCount(models.size(), true);

            List<ModelNode> dataList = dataProvider.getList();
            dataList.clear();
            dataList.addAll(models);
        } else {
            dataProvider.setList(new ArrayList<>());
        }
        selectionModel.clear();
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
        addButton.setEnabled(false);
        removeButton.setEnabled(false);
    }

    protected <T> FormItem<T> formItem(List<FormItem> formItems, String name) {
        for (FormItem formItem : formItems) {
            if (name.equals(formItem.getName())) {
                return formItem;
            }
        }
        return null;
    }


}