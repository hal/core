package org.jboss.as.console.client.shared.subsys.undertow;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
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
import org.jboss.dmr.client.Property;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public abstract class AbstractListenerView implements IsWidget {

    private final HttpPresenter presenter;
    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private AddressTemplate baseAddress;
    private String entityName;

    public AbstractListenerView(HttpPresenter presenter, AddressTemplate baseAddress, String entityName) {
        this.presenter = presenter;
        this.baseAddress = baseAddress;
        this.entityName = entityName;

        ProvidesKey<Property> providesKey = Property::getName;
        this.table = new DefaultCellTable<>(5, providesKey);
        this.dataProvider = new ListDataProvider<>(providesKey);
        this.dataProvider.addDataDisplay(table);
        this.table.setSelectionModel(new SingleSelectionModel<Property>());
    }

    @Override
    public Widget asWidget() {
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        TextColumn<Property> enabledColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return String.valueOf(node.getValue().get("enabled").asBoolean());
            }
        };

        table.addColumn(nameColumn, "Name");
        table.addColumn(enabledColumn, "Is Enabled?");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.onLaunchAddResourceDialog(baseAddress)));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(),
                event -> Feedback.confirm(Console.MESSAGES.deleteTitle(entityName),
                Console.MESSAGES.deleteConfirm(entityName + " '" + getCurrentSelection().getName() + "'"),
                isConfirmed -> {
                    if (isConfirmed) {
                        presenter.onRemoveResource(
                                baseAddress, getCurrentSelection().getName()
                        );
                    }
                })));

        SecurityContext securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(baseAddress);

        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .includeDeprecated(true)
                .build();


        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveResource(baseAddress, getCurrentSelection().getName(), changeset);
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
                .setHeadline(entityName)
                .setDescription("")
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available(entityName), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel);


        selectionModel = new SingleSelectionModel<>();
        selectionModel.addSelectionChangeHandler(event -> {
            Property server = selectionModel.getSelectedObject();
            if(server!=null)
            {
                formAssets.getForm().edit(server.getValue());
            }
            else
            {
                formAssets.getForm().clearValues();
            }
        });
        table.setSelectionModel(selectionModel);
        return layoutBuilder.build();
    }

    private Property getCurrentSelection() {
        Property selection = ((SingleSelectionModel<Property>) table.getSelectionModel()).getSelectedObject();
        return selection;
    }

    public void setData(List<Property> data) {
        dataProvider.setList(data);
        if (data.isEmpty()) {
            selectionModel.clear();
        } else {
            table.selectDefaultEntity();
        }
    }
}
