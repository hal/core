package org.jboss.as.console.client.shared.subsys.messaging;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefiniton;
import org.jboss.as.console.mbui.widgets.ModelDrivenWidget;
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
 * @date 1/17/12
 */
public class ProviderList extends ModelDrivenWidget {
    
    private CommonMsgPresenter presenter;
    private DefaultCellTable table;
    private ListDataProvider<Property> dataProvider;
    private MessagingProviderEditor providerEditor;
    private String token;

    public ProviderList(CommonMsgPresenter presenter, String token) {
        super("{selected.profile}/subsystem=messaging/hornetq-server=*");
        this.presenter = presenter;
        this.token = token;
        this.table = new DefaultCellTable(5);
        this.dataProvider = new ListDataProvider<Property>();
        this.dataProvider.addDataDisplay(table);
        this.table.setSelectionModel(new SingleSelectionModel<Property>());
    }

    @Override
    public Widget buildWidget(ResourceAddress address, ResourceDefiniton definition) {

        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        Column<Property, String> option = new Column<Property, String>(
                new ViewLinkCell<String>(Console.CONSTANTS.common_label_view(), new ActionCell.Delegate<String>() {
                    @Override
                    public void execute(String selection) {
                        presenter.getPlaceManager().revealPlace(
                                new PlaceRequest(token).with("name", selection)
                        );
                    }
                })
        ) {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        table.addColumn(nameColumn, "Name");
        table.addColumn(option, "Option");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchAddProviderDialog();
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle("Messaging Provider"),
                        Console.MESSAGES.deleteConfirm("Messaging Provider '"+getCurrentSelection().getName()+"'"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.removeProvider(getCurrentSelection().getName());
                                }
                            }
                        });
            }
        }));

        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(token);

        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();


        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveProvider(getCurrentSelection().getName(), formAssets.getForm().getChangedValues());
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
                .setHeadline("JMS Messaging Provider")
                .setDescription("Please chose a provider from below for specific settings.")
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Messaging Provider"), table)
                .addDetail("Attributes", formPanel);


        final SingleSelectionModel<Property> selectionModel = new SingleSelectionModel<Property>();
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

    public void setProvider(List<Property> provider) {
        dataProvider.setList(provider);
        table.selectDefaultEntity();

    }
}
