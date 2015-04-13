package org.jboss.as.console.client.shared.general;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.general.model.Interface;
import org.jboss.as.console.client.shared.general.validation.ValidationResult;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.DisclosureGroupRenderer;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.FormValidator;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 10/24/11
 */
public class InterfaceEditor {

    private DefaultCellTable<Interface> table;
    private ListDataProvider<Interface> dataProvider;

    private String title;
    private String description = null;
    private InterfaceManagement presenter;
    private Form<Interface> form;
    private ComboBoxItem anyAddress;

    public InterfaceEditor(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Widget asWidget() {

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setTitle(title)
                .setPlain(true);

        form = new Form<Interface>(Interface.class);

        ToolStrip topLevelTools = new ToolStrip();
        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add() , new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                presenter.launchNewInterfaceDialogue();
            }
        });
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_interfaceEditor());
        topLevelTools.addToolButtonRight(addBtn);
        
        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_remove(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {

                final Interface editedEntity = form.getEditedEntity();
                Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Interface"),
                        Console.MESSAGES.deleteConfirm("Interface " + editedEntity.getName()),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed)
                                    presenter.onRemoveInterface(editedEntity);
                            }
                        });
            }
        });
        removeBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_remove_interfaceEditor());
        topLevelTools.addToolButtonRight(removeBtn);

        layout.setHeadline("Network Interfaces");

        if(description!=null) {
            layout.setDescription(description);
        }

        table = new DefaultCellTable<Interface>(8, new ProvidesKey<Interface>() {
            @Override
            public Object getKey(Interface item) {
                return item.getName();
            }
        });

        dataProvider = new ListDataProvider<Interface>();
        dataProvider.addDataDisplay(table);

        TextColumn<Interface> nameColumn = new TextColumn<Interface>() {
            @Override
            public String getValue(Interface record) {
                return record.getName();
            }
        };

        table.addColumn(nameColumn, "Name");

        layout.setMasterTools(topLevelTools);
        layout.setMaster("", table);

        TextItem nameItem = new TextItem("name", "Name");
        TextBoxItem inetAddress = new TextBoxItem("inetAddress", "Inet Address", false);
        TextBoxItem nic = new TextBoxItem("nic", "Nic", false);
        TextBoxItem nicMatch = new TextBoxItem("nicMatch", "Nic Match", false);

        CheckBoxItem publicAddress = new CheckBoxItem("publicAddress", "Public Address");
        CheckBoxItem siteLocalAddress = new CheckBoxItem("siteLocal", "Site Local Address");
        CheckBoxItem linkLocalAddress = new CheckBoxItem("linkLocal", "Link Local Address");

        anyAddress = new ComboBoxItem("addressWildcard", "Address Wildcard") {
            {
                isRequired = false;
            }
        };

        anyAddress.setDefaultToFirstOption(true);
        anyAddress.setValueMap(new String[]{"", Interface.ANY_ADDRESS});
        anyAddress.setValue("");

        CheckBoxItem up = new CheckBoxItem("up", "Up");
        CheckBoxItem virtual = new CheckBoxItem("virtual", "Virtual");

        CheckBoxItem p2p = new CheckBoxItem("pointToPoint", "Point to Point");
        CheckBoxItem multicast = new CheckBoxItem("multicast", "Multicast");
        CheckBoxItem loopback = new CheckBoxItem("loopback", "Loopback");
        TextBoxItem loopbackAddress = new TextBoxItem("loopbackAddress", "Loopback Address", false);


        form.setFields(
                nameItem,
                inetAddress, anyAddress,
                nic, nicMatch,
                loopback, loopbackAddress);


        form.setFieldsInGroup(
                Console.CONSTANTS.common_label_advanced(),
                new DisclosureGroupRenderer(),
                up, virtual,
                publicAddress, siteLocalAddress,
                linkLocalAddress, multicast, p2p
        );

        final FormToolStrip<Interface> toolstrip = new FormToolStrip<Interface>(
                form,
                new FormToolStrip.FormCallback<Interface>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveInterface(form.getUpdatedEntity(), changeset);
                    }

                    @Override
                    public void onDelete(Interface entity) {

                    }
                });

        final HTML errorMessages = new HTML();
        errorMessages.setStyleName("error-panel");

        toolstrip.providesDeleteOp(false);

        form.bind(table);
        form.setEnabled(false);

        form.addFormValidator(new FormValidator() {
            @Override
            public void validate(List<FormItem> formItems, FormValidation outcome) {
                ValidationResult validation = presenter.validateInterfaceConstraints(
                        form.getUpdatedEntity(),
                        form.getChangedValues()
                );


                errorMessages.setHTML("");

                if(!validation.isValid())
                {
                    SafeHtmlBuilder html = new SafeHtmlBuilder();
                    int i=0;
                    for(String detail : validation.getMessages())
                    {
                        outcome.addError(detail);

                        if(i==0) html.appendHtmlConstant("<b>");
                        html.appendEscaped(detail).appendHtmlConstant("<br/>");
                        if(i==0) html.appendHtmlConstant("</b>");

                        i++;
                    }

                    //Feedback.alert("Invalid Interface Constraints", html.toSafeHtml());
                    errorMessages.setHTML(html.toSafeHtml());
                }
            }
        });

        FormHelpPanel helpPanel = new FormHelpPanel(new FormHelpPanel.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = new ModelNode();
                address.add("interface", "*");
                return address;
            }
        }, form);

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("fill-layout-width");

        panel.add(toolstrip.asWidget());
        panel.add(helpPanel.asWidget());
        panel.add(errorMessages);
        panel.add(form.asWidget());

        // clear messages upon cancel
        toolstrip.addCancelHandler(new Command() {
            @Override
            public void execute() {
                errorMessages.setHTML("");
                toolstrip.doCancel();
            }
        });

        layout.setDetail("", panel);
        return layout.build();
    }

    public void setInterfaces(List<Interface> interfaces) {

        anyAddress.clearSelection();
        form.clearValues();

        dataProvider.setList(interfaces);

        table.selectDefaultEntity();
    }

    public void setPresenter(InterfaceManagement presenter) {
        this.presenter = presenter;
    }
}
