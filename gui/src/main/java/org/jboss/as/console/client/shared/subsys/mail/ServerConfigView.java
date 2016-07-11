package org.jboss.as.console.client.shared.subsys.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_OUTBOUND_SOCKET_BINDING;

/**
 * @author Heiko Braun
 * @date 2/14/12
 */
public class ServerConfigView {

    private HTML headline;
    private String description;
    private Form<MailServerDefinition> form;
    private MailPresenter presenter;
    private ListDataProvider<MailServerDefinition> dataProvider;
    private String title;
    private DefaultCellTable<MailServerDefinition> table;
    private MailSession session;
    private com.google.gwt.view.client.SingleSelectionModel<MailServerDefinition> selectionModel;


    public ServerConfigView(
            String title, String description,
            MailPresenter presenter) {
        this.title= title;
        this.description = description;
        this.presenter = presenter;
    }

    Widget asWidget() {


        table = new DefaultCellTable<MailServerDefinition>(3, new ProvidesKey<MailServerDefinition>() {
            @Override
            public Object getKey(MailServerDefinition item) {
                return item.getType();
            }
        });

        dataProvider = new ListDataProvider<MailServerDefinition>();
        dataProvider.addDataDisplay(table);

        selectionModel = new com.google.gwt.view.client.SingleSelectionModel<>();
        table.setSelectionModel(selectionModel);

        TextColumn<MailServerDefinition> nameColumn = new TextColumn<MailServerDefinition>() {
            @Override
            public String getValue(MailServerDefinition record) {
                return record.getType().name().toUpperCase();

            }
        };

        table.addColumn(nameColumn, "Type");


        ToolStrip tableTools = new ToolStrip();

        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.launchNewServerWizard(session);
                    }
                });

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_remove(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        Feedback.confirm(
                                Console.MESSAGES.deleteTitle(Console.CONSTANTS.common_label_item()),
                                Console.MESSAGES.deleteConfirm(Console.CONSTANTS.common_label_item()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed) {
                                            presenter.onRemoveServer(session.getName(), form.getEditedEntity());
                                        }
                                    }
                                });
                    }
                });

        tableTools.addToolButtonRight(addBtn);
        tableTools.addToolButtonRight(removeBtn);

        // ----

        form = new Form<MailServerDefinition>(MailServerDefinition.class);

        SuggestionResource suggestionResource = new SuggestionResource("socketBinding", "Socket Binding", true,
                Console.MODULES.getCapabilities().lookup(NETWORK_OUTBOUND_SOCKET_BINDING));
        
        FormItem socket = suggestionResource.buildFormItem();
        TextBoxItem user = new TextBoxItem("username", "Username");
        PasswordBoxItem pass = new PasswordBoxItem("password", "Password");
        CheckBoxItem ssl = new CheckBoxItem("ssl", "Use SSL?");

        form.setFields(socket, ssl, user, pass);
        form.setEnabled(false);
        form.setNumColumns(2);

        FormToolStrip formTools = new FormToolStrip<MailServerDefinition>(form,
                new FormToolStrip.FormCallback<MailServerDefinition>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {

                        presenter.onSaveServer(session.getName(), form.getEditedEntity().getType(), changeset);
                    }

                    @Override
                    public void onDelete(MailServerDefinition entity) {

                    }
                });

        headline = new HTML();
        headline.setStyleName("content-header-label");

        final FormHelpPanel helpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = Baseadress.get();
                        address.add("subsystem", "mail");
                        address.add("mail-session", "*");
                        address.add("server", "smtp");
                        return address;
                    }
                }, form
        );


        VerticalPanel formlayout = new VerticalPanel();
        formlayout.setStyleName("fill-layout-width");

        formlayout.add(helpPanel.asWidget());
        formlayout.add(form.asWidget());

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setTitle("Mail Session")
                .setHeadlineWidget(headline)
                .setDescription(description)
                .setMaster(Console.MESSAGES.available("Mail Server"), table)
                .setMasterTools(tableTools)
                .setDetailTools(formTools.asWidget())
                .setDetail(Console.CONSTANTS.common_label_selection(), formlayout);


        form.bind(table);

        return layout.build();

    }


    public void updateFrom(MailSession session) {
        this.session = session;
        headline.setText("Mail Session: " + session.getName());


        List<MailServerDefinition> server = new ArrayList<>();
        if(session.getImapServer()!=null)
            server.add(session.getImapServer());

        if(session.getSmtpServer()!=null)
            server.add(session.getSmtpServer());

        if(session.getPopServer()!=null)
            server.add(session.getPopServer());

        dataProvider.setList(server);
        table.selectDefaultEntity();
        form.clearValues();
    }
}
