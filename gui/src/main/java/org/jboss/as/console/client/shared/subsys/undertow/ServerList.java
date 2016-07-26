package org.jboss.as.console.client.shared.subsys.undertow;

import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.dmr.client.Property;

/**
 * @author Heiko Braun
 * @date 1/17/12
 */
public class ServerList {

    private final AddressTemplate RESOURCE_ADDRESS;

    private CommonHttpPresenter presenter;
    private final boolean isRuntimeView;
    private DefaultCellTable table;
    private ListDataProvider<Property> dataProvider;

    private ContentDescription statsText = new ContentDescription("Statistics status: ");

    public ServerList(CommonHttpPresenter presenter, boolean isRuntimeView) {

        this.presenter = presenter;
        this.isRuntimeView = isRuntimeView;

        ProvidesKey<Property> keyProvider = new ProvidesKey<Property>() {
            @Override
            public Object getKey(Property property) {
                return property.getName();
            }
        };

        if (isRuntimeView)
        {
            this.RESOURCE_ADDRESS = AddressTemplate.of("/{implicit.host}/{selected.server}/subsystem=undertow/server=*");
        }
        else
        {
            this.RESOURCE_ADDRESS = AddressTemplate.of("{selected.profile}/subsystem=undertow/server={undertow.server}");
        }

        this.table = new DefaultCellTable(5, keyProvider);
        this.dataProvider = new ListDataProvider<Property>();
        this.dataProvider.addDataDisplay(table);
        this.table.setSelectionModel(new SingleSelectionModel<Property>(keyProvider));
    }

    public Widget asWidget() {

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
                                new PlaceRequest(presenter.getNameToken()).with("name", selection)
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

        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(presenter.getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(RESOURCE_ADDRESS);

        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();


        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {

                presenter.onSaveResource(
                   RESOURCE_ADDRESS, getCurrentSelection().getName(), changeset
                );
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
                .setHeadline("HTTP Server ")
                .setMaster(Console.MESSAGES.available("HTTP Server "), table);
        
        String description = "Please chose a server from below for further settings.";
        if (isRuntimeView) {
            // adds the buttons to enable and disable the statistics 
            description += " If no metrics are shown, you might need to enable statistics in the configuration section for the desired profile.";
            layoutBuilder.addDetail("Statistics", statsText);
        } else {
            layoutBuilder.addDetail(Console.CONSTANTS.common_label_attributes(), formPanel);
        }
        layoutBuilder.setDescription(SafeHtmlUtils.fromString(description));


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

    public void setServer(List<Property> provider) {
        dataProvider.setList(provider);
        table.selectDefaultEntity();

    }

    public void setStatistcsEnabled(final boolean stats) {
        if (stats) {
            statsText.setText("Status: ON");
        } else {
            statsText.setText("Status: OFF");
        }
    }
}
