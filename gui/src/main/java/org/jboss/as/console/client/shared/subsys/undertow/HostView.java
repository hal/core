package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class HostView {

    private static final AddressTemplate BASE_ADDRESS = AddressTemplate .of(
            "{selected.profile}/subsystem=undertow/server={undertow.server}/host=*");

    private final HttpPresenter presenter;
    private final DefaultCellTable table;
    private final ListDataProvider<Property> dataProvider;
    private FilterRefEditor filterRefEditor;
    private List<Property> data;
    private final SecurityContext securityContext;
    private final ResourceDescription definition;
    private final SingleSelectionModel<Property> selectionModel;

    public HostView(HttpPresenter presenter) {
        this.presenter = presenter;
        this.table = new DefaultCellTable(5);
        this.dataProvider = new ListDataProvider<Property>();
        this.dataProvider.addDataDisplay(table);
        this.table.setSelectionModel(new SingleSelectionModel<Property>());
        securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        definition = presenter.getDescriptionRegistry().lookup(BASE_ADDRESS);
        filterRefEditor = new FilterRefEditor(presenter, BASE_ADDRESS.append("filter-ref=*"), definition);
        selectionModel = new SingleSelectionModel<>();
    }

    public Widget asWidget() {
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        table.addColumn(nameColumn, "Name");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onLaunchAddResourceDialog(BASE_ADDRESS);
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle("Host Setting"),
                        Console.MESSAGES.deleteConfirm("Host Setting '" + getCurrentSelection().getName() + "'"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onRemoveResource(
                                            BASE_ADDRESS, getCurrentSelection().getName()
                                    );
                                }
                            }
                        });
            }
        }));


        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();


        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveResource(BASE_ADDRESS, getCurrentSelection().getName(), changeset);
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());

        // ----
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Host Setting")
                .setDescription("")
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Host Settings"), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel)
                .addDetail("Reference to Filter", filterRefEditor.asWidget());


        selectionModel.addSelectionChangeHandler(event -> {
            Property hostname = selectionModel.getSelectedObject();
            if (hostname != null) {
                formAssets.getForm().edit(hostname.getValue());
                updateFilterRefsFromModel();
            } else {
                formAssets.getForm().clearValues();
            }
        });
        table.setSelectionModel(selectionModel);
        return layoutBuilder.build();
    }

    private Property getCurrentSelection() {
        Property selection = selectionModel.getSelectedObject();
        return selection;
    }
    
    private void updateFilterRefsFromModel() {
        ModelNode handlerItem = selectionModel.getSelectedObject().getValue();
        filterRefEditor.updateOperationAddressNames(getCurrentSelection().getName());
        if (handlerItem.hasDefined("filter-ref")) {
            List<Property> handlers = handlerItem.get("filter-ref").asPropertyList();
            filterRefEditor.update(handlers);
        } else {
            filterRefEditor.clearValues();
        }
        
    }

    public void setData(List<Property> data) {
        dataProvider.setList(data);

        if (data.isEmpty()) {
            selectionModel.clear();
            filterRefEditor.clearValues();
        } else {
            table.selectDefaultEntity();
            updateFilterRefsFromModel();
        }
        
        table.selectDefaultEntity();
    }

    public void selectModifiedHost(String hostname) {
        Property hit = null;
        for (Property property : dataProvider.getList()) {
            if (property.getName().equals(hostname)) {
                hit = property;
                break;
            }
        }
        if (hit != null) {
            selectionModel.setSelected(hit, true);
        } else {
            table.selectDefaultEntity();
        }
    }
}
