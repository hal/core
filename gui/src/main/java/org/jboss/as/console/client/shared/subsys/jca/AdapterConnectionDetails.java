package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.jca.model.ConnectionDefinition;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.dmr.client.ModelNode;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 7/19/11
 */
public class AdapterConnectionDetails {

    private VerticalPanel layout;
    private Form<ConnectionDefinition> form;
    private ResourceAdapterPresenter presenter;

    public AdapterConnectionDetails(final ResourceAdapterPresenter presenter) {

        this.presenter = presenter;

        layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        form = new Form<ConnectionDefinition>(ConnectionDefinition.class);
        form.setNumColumns(2);

        FormToolStrip<ConnectionDefinition> toolStrip = new FormToolStrip<ConnectionDefinition>(
                form,
                new FormToolStrip.FormCallback<ConnectionDefinition>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveConnection(form.getEditedEntity(), form.getChangedValues());
                    }

                    @Override
                    public void onDelete(ConnectionDefinition entity) {

                    }
                });

        toolStrip.providesDeleteOp(false);


        layout.add(toolStrip.asWidget());

        // ----

        TextBoxItem name = new TextBoxItem("name", "Name");
        TextBoxItem jndiItem = new TextBoxItem("jndiName", "JNDI");
        TextBoxItem classItem = new TextBoxItem("connectionClass", "Connection Class");
        //CheckBoxItem enabled = new CheckBoxItem("enabled", "Enabled?");

        form.setFields(name, jndiItem, classItem);

        final FormHelpPanel helpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = Baseadress.get();
                        address.add("subsystem", "resource-adapters");
                        address.add("resource-adapter", "*");
                        address.add("connection-definitions", "*");
                        return address;
                    }
                }, form
        );
        layout.add(helpPanel.asWidget());
        form.setEnabled(true);
        layout.add(form.asWidget());
    }

    Widget asWidget() {
        return layout;
    }

    public Form<ConnectionDefinition> getForm() {
        return form;
    }

    public void setEnabled(boolean b) {
        form.setEnabled(b);
    }

    public ConnectionDefinition getCurrentSelection() {
        return form.getEditedEntity();
    }
}
