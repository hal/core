package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
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

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 10/09/14
 */
public class ServiceViewTemplate {

    private final EEPresenter presenter;

    private final AddressTemplate address;
    private final DefaultCellTable table;
    private final ListDataProvider<Property> dataProvider;

    private String title;
    private SingleSelectionModel<Property> selectionModel;

    public ServiceViewTemplate(EEPresenter presenter, String title, AddressTemplate address) {

        this.title = title;
        this.presenter = presenter;
        this.address = address;
        ProvidesKey<Property> providesKey = Property::getName;
        this.table = new DefaultCellTable<>(5, providesKey);
        this.dataProvider = new ListDataProvider<>(providesKey);
        this.dataProvider.addDataDisplay(table);
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
                presenter.onLaunchAddResourceDialog(address);
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle(title),
                        Console.MESSAGES.deleteConfirm(title +" "+ getCurrentSelection().getName()),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onRemoveResource(
                                            address, getCurrentSelection().getName()
                                    );
                                }
                            }
                        });
            }
        }));

        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(address);
        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());

        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();


        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveResource(address, getCurrentSelection().getName(), changeset);
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
                .setHeadline(title)
                .setDescription("")
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available(title), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel);


        selectionModel = new SingleSelectionModel<Property>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property selection = selectionModel.getSelectedObject();
                if(selection!=null)
                {
                    formAssets.getForm().edit(selection.getValue());
                }
                else
                {
                    formAssets.getForm().clearValues();
                }
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
        if(selectionModel!=null) {
            dataProvider.setList(data);
            table.selectDefaultEntity();
            SelectionChangeEvent.fire(selectionModel); // updates ModelNodeForm's editedEntity with current value
        }
    }
}
