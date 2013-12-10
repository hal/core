package org.jboss.as.console.client.domain.hosts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 10/27/11
 */
public class ServerConfigDetails {

    private ServerConfigPresenter presenter;
    private Form<Server> form;
    private ComboBoxItem socketItem;
    private ComboBoxItem groupItem;
    private List<ServerGroupRecord> groups = Collections.EMPTY_LIST;

    public ServerConfigDetails(ServerConfigPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        form = new Form<Server>(Server.class);

        FormToolStrip<Server> toolStrip = new FormToolStrip<Server>(
                form, new FormToolStrip.FormCallback<Server>() {

            @Override
            public void onSave(Map<String, Object> changeset) {
                presenter.onSaveChanges(form.getUpdatedEntity(), changeset);
            }

            @Override
            public void onDelete(Server entity) {
                Feedback.confirm(
                        Console.MESSAGES.deleteServerConfig(),
                        Console.MESSAGES.deleteServerConfigConfirm(form.getEditedEntity().getName()),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed)
                                    presenter.tryDelete(form.getEditedEntity());
                            }
                        });
            }
        });

        toolStrip.providesDeleteOp(false);

        layout.add(toolStrip.asWidget());


        TextItem nameItem = new TextItem("name", "Name");

        CheckBoxItem startedItem = new CheckBoxItem("autoStart", Console.CONSTANTS.common_label_autoStart());

        groupItem = new ComboBoxItem("group", "Server Group");

        // ------------------------------------------------------

        final NumberBoxItem portOffset = new NumberBoxItem("portOffset", Console.CONSTANTS.common_label_portOffset());

        socketItem = new ComboBoxItem("socketBinding", Console.CONSTANTS.common_label_socketBinding())
        {
            @Override
            public boolean validate(String value) {
                boolean parentValid = super.validate(value);
                //boolean portDefined = !portOffset.isModified();
                return parentValid ;//&& portDefined;
            }

            @Override
            public String getErrMessage() {
                return Console.MESSAGES.common_validation_portOffsetUndefined(super.getErrMessage());
            }
        };

        form.setFields(nameItem, startedItem, groupItem, socketItem, portOffset);

        final FormHelpPanel helpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = new ModelNode();
                        address.add("host", presenter.getSelectedHost());
                        address.add("server-config", "*");
                        return address;
                    }
                }, form
        );
        layout.add(helpPanel.asWidget());

        form.setEnabled(false);
        layout.add(form.asWidget());
        form.setSecurityContextFilter("/{selected.host}/server-config=*");

        return layout;
    }

    public void setAvailableSockets(List<String> result) {
        socketItem.clearValue();
        socketItem.clearSelection();

        socketItem.setValueMap(result);
    }

    public void bind(final DefaultCellTable table) {
        form.bind(table);

        table.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                final Server server = ((SingleSelectionModel<Server>) table.getSelectionModel()).getSelectedObject();
                if(server!=null && "".equals(server.getSocketBinding()))
                {
                    // preselect inherited socket binding value
                    for(final ServerGroupRecord group : groups)
                    {
                        if(group.getName().equals(server.getGroup()))
                        {
                            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    socketItem.setValue(group.getSocketBinding());
                                }
                            });
                            break;
                        }
                    }
                }
            }
        });
    }

    public void setAvailableGroups(List<ServerGroupRecord> result) {

        this.groups = result;

        List<String> names = new ArrayList<String>(result.size());
        for(ServerGroupRecord rec : result)
            names.add(rec.getName());

        groupItem.setValueMap(names);
    }
}
