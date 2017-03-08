package org.jboss.as.console.client.shared.subsys.jgroups;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;

/**
 * @author Heiko Braun
 * @date 2/22/12
 */
public class StackStep2 {

    private NewStackWizard presenter;
    private DefaultCellTable<JGroupsProtocol> table;
    private ListDataProvider<JGroupsProtocol> dataProvider;
    private HTML errorMessages;

    public StackStep2(NewStackWizard presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.getElement().setAttribute("style", "margin:15px; vertical-align:center;width:95%");

        layout.add(new HTML("<h3>"+ Console.CONSTANTS.subsys_jgroups_step2()+"</h3>"));

        // available protocols
        List<String> names = new ArrayList<>();
        for (Protocol element : Protocol.values()) {
            final String name = element.getLocalName();
            if (name!=null && !"TCP".equals(name) && !"UDP".equals(name))
                names.add(name);
        }

        final Form<JGroupsProtocol> form = new Form<JGroupsProtocol>(JGroupsProtocol.class);

        ComboBoxItem nameField = new ComboBoxItem("name", "Name");


        nameField.setValueMap(names);

        FormItem socket = new SuggestionResource("socketBinding", "Protocol Socket Binding", true,
                Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING))
                .buildFormItem();

        form.setFields(nameField, socket);

        layout.add(AddStackHelpPanel.helpStep2().asWidget());
        layout.add(form.asWidget());

        //  ------


        table = new DefaultCellTable<>(6, new ProvidesKey<JGroupsProtocol>() {
            @Override
            public Object getKey(JGroupsProtocol item) {
                return item.getName();
            }
        });
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(table);

        TextColumn<JGroupsProtocol> name = new TextColumn<JGroupsProtocol>() {
            @Override
            public String getValue(JGroupsProtocol record) {
                return record.getName();
            }
        };

        table.addColumn(name, "Name");

        final SingleSelectionModel<JGroupsProtocol> selectionModel = new SingleSelectionModel<>();
        table.setSelectionModel(selectionModel);

        ToolStrip toolstrip = new ToolStrip();

        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_append(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                FormValidation validation = form.validate();
                if(!validation.hasErrors())
                {
                    errorMessages.setVisible(false);
                    JGroupsProtocol protocol = form.getUpdatedEntity();
                    dataProvider.getList().add(protocol);
                    table.getSelectionModel().setSelected(protocol, true);
                }
            }
        });
        toolstrip.addToolButtonRight(addBtn);

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                JGroupsProtocol protocol = selectionModel.getSelectedObject();
                List<JGroupsProtocol> list = dataProvider.getList();
                list.remove(protocol);

                List<JGroupsProtocol> update = new LinkedList<>();
                update.addAll(list);

                dataProvider.setList(update);
            }
        });

        toolstrip.addToolButtonRight(removeBtn);


        layout.add(new ContentGroupLabel("Protocol Stack"));

        errorMessages = new HTML(Console.CONSTANTS.subsys_jgroups_err_protocols_required());
        errorMessages.setStyleName("error-panel");
        errorMessages.setVisible(false);

        toolstrip.addToolWidget(errorMessages);

        layout.add(toolstrip.asWidget());
        layout.add(table.asWidget());


        // ----


        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                List<JGroupsProtocol> list = dataProvider.getList();
                errorMessages.setVisible(false);
                if(list.isEmpty())
                    errorMessages.setVisible(true);
                else
                    presenter.onFinishStep2(list);

            }
        };

        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.cancel();
            }
        };

        DialogueOptions options = new DialogueOptions(
                "Done",submitHandler,
                Console.CONSTANTS.common_label_cancel(),cancelHandler
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
