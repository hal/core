package org.jboss.as.console.client.shared.subsys.activemq.connections;

import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.shared.subsys.activemq.connections.MsgConnectionsPresenter.MESSAGING_SERVER;
import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;

/**
 * @author Claudio Miranda
 * @date 11/18/16
 */
public class PooledConnectionFactoryView {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> provider;
    private MsgConnectionsPresenter presenter;
    private SecurityContext securityContext;
    private ResourceDescription pooledConnectionDescription;
    protected SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets modelForm;

    public PooledConnectionFactoryView(MsgConnectionsPresenter presenter) {
        this.presenter = presenter;
        securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        // r-r-d the parent server, because mgmt op doesn't work to read resource with 2 wildcards 
        // as blabla/server=*/pooled-connection-factory=*, see WFCORE-2022
        pooledConnectionDescription = presenter.getDescriptionRegistry().lookup(MESSAGING_SERVER);
        pooledConnectionDescription = pooledConnectionDescription.getChildDescription("pooled-connection-factory");
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        ProvidesKey<Property> providesKey = Property::getName;
        table = new DefaultCellTable<>(10, providesKey);
        provider = new ListDataProvider<>(providesKey);
        provider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<>(providesKey);

        setupTable();

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(modelForm.getHelp().asWidget());
        formPanel.add(modelForm.getForm().asWidget());

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(SafeHtmlUtils.fromString(pooledConnectionDescription.get(DESCRIPTION).asString()))
                .setMaster(Console.MESSAGES.available("Pooled Connection Factory"), table)
                .setMasterTools(setupMasterTools())
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel.asWidget());

        return layout.build();
    }
    
    private void setupTable() {
        Column<Property, String> name = new Column<Property, String>(new TextCell()) {
            @Override
            public String getValue(Property object) {
                return object.getName();
            }
        };
        Column<Property, String> jndi = new Column<Property, String>(new TextCell()) {
            @Override
            public String getValue(Property object) {
                return object.getValue().get("entries").asString().replace("\"", "");
            }
        };

        table.addColumn(name, "Name");
        table.addColumn(jndi, "JNDI");
        table.setColumnWidth(name, 40, Style.Unit.PCT);
        table.setColumnWidth(jndi, 60, Style.Unit.PCT);
        name.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        jndi.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(pooledConnectionDescription)
                .setSecurityContext(securityContext);
        modelForm = formBuilder.build();
        modelForm.getForm().addFormValidator(this::validateForm);

        modelForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                presenter.onSavePooledCF(selectionModel.getSelectedObject().getName(), changeset);
            }

            @Override
            public void onCancel(final Object entity) {
                modelForm.getForm().cancel();
            }
        });

        selectionModel.addSelectionChangeHandler(event -> editSelected());
        table.setSelectionModel(selectionModel);
    }

    private void editSelected() {
        Property selected = selectionModel.getSelectedObject();
        if (selected != null) {
            modelForm.getForm().edit(selected.getValue());
        } else {
            modelForm.getForm().clearValues();
        }
    }

    private ToolStrip setupMasterTools() {
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(), clickEvent ->
                        onAdd()
                ));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Pooled Connection Factory"),
                        Console.MESSAGES.deleteConfirm("Pooled Connection Factory " + selectionModel.getSelectedObject().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeletePooledCF(selectionModel.getSelectedObject().getName());
                            }
                        })));
        return tools;
    }

    private void onAdd() {
        ModelNodeFormBuilder.FormAssets addFormAssets = new ModelNodeFormBuilder()
                .setResourceDescription(pooledConnectionDescription)
                .setCreateMode(true)
                .unsorted()
                .setSecurityContext(securityContext)
                .include("connectors", "discovery-group", "entries")
                .build();
        addFormAssets.getForm().setEnabled(true);
        addFormAssets.getForm().addFormValidator(this::validateForm);

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Pooled Connection Factory"));
        AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
            @Override
            public void onAdd(ModelNode payload) {
                presenter.addPooledConnectionFactory(payload);
                dialog.hide();
            }

            @Override
            public void onCancel() {
                dialog.hide();
            }
        };
        AddResourceDialog addDialog = new AddResourceDialog(addFormAssets, pooledConnectionDescription, callback);
        dialog.setWidth(540);
        dialog.setHeight(560);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    private void validateForm(final List<FormItem> formItems,
            final FormValidation formValidation) {// at least one field is necessary to fill
        FormItem entriesItem = PooledConnectionFactoryView.this.formItem(formItems, "entries");
        FormItem connectorsItem = PooledConnectionFactoryView.this.formItem(formItems, "connectors");
        FormItem discoveryGroupItem = PooledConnectionFactoryView.this.formItem(formItems, "discovery-group");
        List entriesList = (List) entriesItem.getValue();
        List connectorsList = (List) connectorsItem.getValue();
        String discoveryGroupStr = (String) discoveryGroupItem.getValue();

        boolean entriesValid = !entriesItem.isUndefined() && entriesList.size() > 0;
        boolean connectorsValid = !connectorsItem.isUndefined() && connectorsList.size() > 0;
        boolean discoveryGroupValid = !discoveryGroupItem.isUndefined() && discoveryGroupStr.length() > 0;

        if (!entriesValid) {
            formValidation.addError("entries");
            entriesItem.setErrMessage("Required field.");
            entriesItem.setErroneous(true);
        } else {
            // validate if both are filled
            // OR none are filled
            if ((connectorsValid && discoveryGroupValid)
                    || !(connectorsValid || discoveryGroupValid)) {
                formValidation.addError("connectors");
                connectorsItem.setErrMessage("Only one of connectors or discovery group should be filled.");
                connectorsItem.setErroneous(true);
                discoveryGroupItem.setErrMessage("Only one of connectors or discovery group should be filled.");
                discoveryGroupItem.setErroneous(true);

            }
        }
    }

    public void setModel(List<Property> models) {
        modelForm.getForm().clearValues();
        provider.setList(models);
        serverName.setText("Pooled Connection Factory: Provider " + presenter.getCurrentServer());
        table.selectDefaultEntity();
        editSelected();
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
