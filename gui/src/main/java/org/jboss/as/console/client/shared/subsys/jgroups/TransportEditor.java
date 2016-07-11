package org.jboss.as.console.client.shared.subsys.jgroups;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.properties.NewPropertyWizard;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;
import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;
import static org.jboss.dmr.client.ModelDescriptionConstants.OP;

/**
 * @author Heiko Braun
 * @date 2/16/12
 */
public class TransportEditor implements PropertyManagement {

    private HTML headline;
    private JGroupsPresenter presenter;
    private PropertyEditor properyEditor;
    private Form<JGroupsTransport> form;
    private DefaultWindow propertyWindow;

    public TransportEditor(JGroupsPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {

        form = new Form<JGroupsTransport>(JGroupsTransport.class);
        form.setNumColumns(2);

        TextItem name = new TextItem("name", "Name");
        FormItem socket = new SuggestionResource("socketBinding", "Socket Binding", true,
                Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING))
                .buildFormItem();
        FormItem diagSocket = new SuggestionResource("diagSocketBinding", "Diagnostics Socket", false,
                Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING))
                .buildFormItem();
        
        CheckBoxItem shared= new CheckBoxItem("shared", "Is Shared?");
        TextBoxItem machine = new TextBoxItem("machine", "Machine", false);
        TextBoxItem site= new TextBoxItem("site", "Site", false);
        TextBoxItem rack= new TextBoxItem("rack", "Rack", false);

        /*
    @Binding(skip = true)
    List<PropertyRecord> getProperties();
    void setProperties(List<PropertyRecord> properties);

         */

        form.setFields(name, socket, diagSocket, machine, shared, site, rack);

        form.setEnabled(false);


        FormHelpPanel helpPanel = new FormHelpPanel(new FormHelpPanel.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = Baseadress.get();
                address.add("subsystem", "jgroups");
                address.add("stack", "*");
                address.add("transport", "TRANSPORT");
                return address;
            }
        }, form);

        FormToolStrip<JGroupsTransport> formToolStrip = new FormToolStrip<JGroupsTransport>(
                form, new FormToolStrip.FormCallback<JGroupsTransport>() {
            @Override
            public void onSave(Map<String, Object> changeset) {
                presenter.onSaveTransport(form.getEditedEntity().getName(), changeset);
            }

            @Override
            public void onDelete(JGroupsTransport entity) {

            }
        });
        formToolStrip.providesDeleteOp(false);

        Widget detail = new FormLayout()
                .setForm(form)
                .setHelp(helpPanel)
                .setTools(formToolStrip).build();

        headline = new HTML();
        headline.setStyleName("content-header-label");

        properyEditor = new PropertyEditor(this, true);

        Widget panel = new OneToOneLayout()
                .setPlain(true)
                .setTitle("JGroups")
                .setHeadlineWidget(headline)
                .setDescription(Console.CONSTANTS.subsys_jgroups_transport_desc())
                .addDetail("Transport Attributes", detail)
                .addDetail("Properties", properyEditor.asWidget())
                .build();

        return panel;
    }

    public void setStack(JGroupsStack stack) {
        headline.setText("Transport: Stack " + stack.getName());
        List<PropertyRecord> transportProperties = stack.getTransport().getProperties();
        if (transportProperties != null) {
            properyEditor.setProperties(stack.getName() + "_#_" + stack.getTransport().getName(), transportProperties);
        } else {
            properyEditor.setProperties(stack.getName() + "_#_" + stack.getTransport().getName(), Collections.<PropertyRecord>emptyList());
        }
        form.edit(stack.getTransport());

    }

    @Override
    public void onCreateProperty(String reference, PropertyRecord prop) {
        closePropertyDialoge();

        String[] tokens = reference.split("_#_");

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "jgroups");
        operation.get(ADDRESS).add("stack", tokens[0]);
        operation.get(ADDRESS).add("transport", tokens[1]);
        operation.get(OP).set("map-put");
        operation.get(NAME).set("properties");
        operation.get("key").set(prop.getKey());
        operation.get("value").set(prop.getValue());

        presenter.getDispatcher().execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Transport Property"),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Transport Property"));
                    presenter.loadStacks(true);
                }
            }
        });
    }

    @Override
    public void onDeleteProperty(String reference, PropertyRecord prop) {
        String[] tokens = reference.split("_#_");

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "jgroups");
        operation.get(ADDRESS).add("stack", tokens[0]);
        operation.get(ADDRESS).add("transport", tokens[1]);
        operation.get(OP).set("map-remove");
        operation.get(NAME).set("properties");
        operation.get("key").set(prop.getKey());

        presenter.getDispatcher().execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Transport Property"), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Transport Property"));
                    presenter.loadStacks(true);
                }
            }
        });
    }

    @Override
    public void onChangeProperty(String reference, PropertyRecord prop) {
        // not supported
    }

    @Override
    public void launchNewPropertyDialoge(String reference) {
        propertyWindow = new DefaultWindow(Console.MESSAGES.createTitle("Transport Property"));
        propertyWindow.setWidth(480);
        propertyWindow.setHeight(360);
        propertyWindow.trapWidget(new NewPropertyWizard(this, reference).asWidget());
        propertyWindow.setGlassEnabled(true);
        propertyWindow.center();
    }

    @Override
    public void closePropertyDialoge() {
        if (propertyWindow != null) {
            propertyWindow.hide();
        }
    }
}
