package org.jboss.as.console.client.shared.subsys.activemq.connections;

import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.activemq.forms.AcceptorForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.AcceptorType;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAcceptor;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class AcceptorList {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<ActivemqAcceptor> table;
    private ListDataProvider<ActivemqAcceptor> provider;
    private MsgConnectionsPresenter presenter;
    private AcceptorForm acceptorForm;
    private AcceptorType type;
    private PropertyEditor properties;

    public AcceptorList(MsgConnectionsPresenter presenter, AcceptorType type) {
        this.presenter = presenter;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();
        table = new DefaultCellTable<>(10, ActivemqAcceptor::getName);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        provider = new ListDataProvider<>();
        provider.addDataDisplay(table);

        Column<ActivemqAcceptor, String> name = new Column<ActivemqAcceptor, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqAcceptor object) {
                return object.getName();
            }
        };
        table.addColumn(name, "Name");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(),
                        clickEvent -> presenter.launchNewAcceptorWizard(type)));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Acceptor"),
                        Console.MESSAGES.deleteConfirm("Acceptor " + getSelectedEntity().getSocketBinding()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteAcceptor(getSelectedEntity());
                            }
                        })));

        acceptorForm = new AcceptorForm(presenter, new FormToolStrip.FormCallback<ActivemqAcceptor>() {
            @Override
            public void onSave(Map<String, Object> changeset) {
                presenter.onSaveAcceptor(getSelectedEntity(), changeset);
            }

            @Override
            public void onDelete(ActivemqAcceptor entity) {}
        }, type);

        properties = new PropertyEditor(presenter, true);

        VerticalPanel layout = new VerticalPanel();
        layout.add(tools);
        layout.add(table);
        layout.add(pager);

        acceptorForm.getForm().bind(table);

        TabPanel tabs = new TabPanel();
        tabs.setStyleName("default-tabpanel");
        tabs.addStyleName("master_detail-detail");
        tabs.getElement().setAttribute("style", "margin-top:15px;");

        tabs.add(acceptorForm.asWidget(), Console.CONSTANTS.common_label_details());
        tabs.add(properties.asWidget(), Console.CONSTANTS.common_label_properties());

        layout.add(tabs);
        tabs.selectTab(0);

        table.getSelectionModel().addSelectionChangeHandler(selectionChangeEvent -> {
            List<PropertyRecord> props = getSelectedEntity().getParameter();
            String tokens = getSelectedEntity().getType().getResource() + "_#_" + getSelectedEntity().getName();
            properties.setProperties(tokens, props);
        });

        return layout;
    }

    public void setAcceptors(List<ActivemqAcceptor> acceptors) {
        properties.clearValues();
        provider.setList(acceptors);
        serverName.setText("Acceptors: Provider " + presenter.getCurrentServer());
        table.selectDefaultEntity();

    }

    @SuppressWarnings("unchecked")
    public ActivemqAcceptor getSelectedEntity() {
        SingleSelectionModel<ActivemqAcceptor> selectionModel = (SingleSelectionModel<ActivemqAcceptor>) table.getSelectionModel();
        return selectionModel.getSelectedObject();
    }
}
