package org.jboss.as.console.client.shared.subsys.ejb3;

import java.util.List;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

/**
 * Manages mapping between security domains used in deployments and Elytron security domains.
 *
 * @author Tomas Hofman (thofman@redhat.com)
 */
public class ApplicationSecurityDomainView implements IsWidget {

    private static final AddressTemplate BASE_ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=ejb3/application-security-domain=*");

    private final EJB3Presenter presenter;
    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;

    public ApplicationSecurityDomainView(EJB3Presenter presenter) {
        this.presenter = presenter;
        this.selectionModel = new SingleSelectionModel<>();
        ProvidesKey<Property> providesKey = Property::getName;
        this.table = new DefaultCellTable<>(5, providesKey);
        this.table.setSelectionModel(selectionModel);
        this.dataProvider = new ListDataProvider<>(providesKey);
        this.dataProvider.addDataDisplay(this.table);
    }

    @Override
    public Widget asWidget() {
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        TextColumn<Property> domainColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getValue().get("security-domain").asString();
            }
        };

        table.addColumn(nameColumn, "Name");
        table.addColumn(domainColumn, "Elytron Security Domain");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.onLaunchAddResourceDialog(BASE_ADDRESS)));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(),
                event -> Feedback.confirm(Console.MESSAGES.deleteTitle("Application Security Domain"),
                        Console.MESSAGES.deleteConfirm("Application Security Domain Mapping '" + selectionModel.getSelectedObject().getName() + "'"),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onRemoveResource(BASE_ADDRESS, selectionModel.getSelectedObject().getName());
                            }
                        })));

        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(BASE_ADDRESS);
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Application Security Domains Mapping")
                .setDescription(SafeHtmlUtils.fromString(definition.get("description").asString()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Application Security Domains"), table);
        return layoutBuilder.build();
    }

    public void setData(List<Property> data) {
        dataProvider.setList(data);
        table.selectDefaultEntity();
        SelectionChangeEvent.fire(selectionModel); // updates ModelNodeForm's editedEntity with current value
    }
}
