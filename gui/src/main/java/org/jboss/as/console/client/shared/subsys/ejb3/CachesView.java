package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
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
 * @since 05/09/14
 */
public class CachesView {

    private static final AddressTemplate BASE_ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=ejb3/cache=*");

    private final EJB3Presenter presenter;
    private final DefaultCellTable table;
    private final ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;

    public CachesView(EJB3Presenter presenter) {
        this.presenter = presenter;
        this.table = new DefaultCellTable(5);
        this.dataProvider = new ListDataProvider<Property>();
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
                presenter.onLaunchAddResourceDialog(BASE_ADDRESS);
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle("Caches"),
                        Console.MESSAGES.deleteConfirm("Cache '" + getCurrentSelection().getName() + "'"),
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

        SecurityContext securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(BASE_ADDRESS);

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
                .setHeadline("Caches")
                .setDescription(definition.get("description").asString())
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Cache "), table)
                .addDetail("Attributes", formPanel);


        selectionModel = new SingleSelectionModel<Property>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property server = selectionModel.getSelectedObject();
                if(server!=null)
                {
                    formAssets.getForm().edit(server.getValue());
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
        selectionModel.clear();
        dataProvider.setList(data);
        table.selectDefaultEntity();
    }
}
