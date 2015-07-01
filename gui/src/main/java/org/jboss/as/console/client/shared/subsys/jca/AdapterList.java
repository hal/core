package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.properties.NewPropertyWizard;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.jca.model.ResourceAdapter;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.ListItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 12/12/11
 */
public class AdapterList implements PropertyManagement {

    private ResourceAdapterPresenter presenter;

    private Form<ResourceAdapter> attributesForm;
    private Form<ResourceAdapter> wmForm;
    private PropertyEditor propertyEditor;
    private DefaultWindow window;
    private HTML title;
    private ResourceAdapter selectedEntity;

    public AdapterList(ResourceAdapterPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {


        VerticalPanel attributesFormPanel = new VerticalPanel();
        attributesFormPanel.setStyleName("fill-layout-width");

        attributesForm = new Form<ResourceAdapter>(ResourceAdapter.class);
        attributesForm.setNumColumns(2);

        FormToolStrip<ResourceAdapter> attributesToolStrip = new FormToolStrip<ResourceAdapter>(
                attributesForm,
                new FormToolStrip.FormCallback<ResourceAdapter>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSave(attributesForm.getEditedEntity(), attributesForm.getChangedValues());
                    }

                    @Override
                    public void onDelete(ResourceAdapter entity) {

                    }
                });

        attributesToolStrip.providesDeleteOp(false);


        attributesFormPanel.add(attributesToolStrip.asWidget());

        // ----

        TextItem nameItem = new TextItem("name", "Name");
        TextBoxItem archiveItem = new TextBoxItem("archive", "Archive");
        TextBoxItem moduleItem = new TextBoxItem("module", "Module");
        ComboBoxItem txItem = new ComboBoxItem("transactionSupport", "Transaction Support");
        txItem.setDefaultToFirstOption(true);
        txItem.setValueMap(new String[]{"NoTransaction", "LocalTransaction", "XATransaction"});
        CheckBoxItem statisticsEnabled = new CheckBoxItem("statisticsEnabled", "Statistics Enabled");
        TextBoxItem bootstrapContext = new TextBoxItem("bootstrapContext", "Bootstrap Context");
        ListItem beanValidationGroups = new ListItem("beanValidationGroups", "Bean Validation Groups");

        attributesForm.setFields(nameItem, archiveItem, moduleItem, txItem, statisticsEnabled, bootstrapContext,
                beanValidationGroups);

        final FormHelpPanel attributesHelpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = Baseadress.get();
                        address.add("subsystem", "resource-adapters");
                        address.add("resource-adapter", "*");
                        return address;
                    }
                }, attributesForm
        );
        attributesFormPanel.add(attributesHelpPanel.asWidget());

        attributesFormPanel.add(attributesForm.asWidget());

        attributesForm.setEnabled(false);



        // -------

        VerticalPanel wmFormPanel = new VerticalPanel();
        wmFormPanel.setStyleName("fill-layout-width");

        wmForm = new Form<ResourceAdapter>(ResourceAdapter.class);
        wmForm.setNumColumns(2);

        FormToolStrip<ResourceAdapter> wmToolStrip = new FormToolStrip<ResourceAdapter>(
                wmForm,
                new FormToolStrip.FormCallback<ResourceAdapter>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSave(wmForm.getEditedEntity(), wmForm.getChangedValues());
                    }

                    @Override
                    public void onDelete(ResourceAdapter entity) {

                    }
                });

        wmToolStrip.providesDeleteOp(false);
        wmFormPanel.add(wmToolStrip.asWidget());

        // ----

        CheckBoxItem wmEnabled = new CheckBoxItem("wmEnabled", "Enabled");
        ListItem wmDefaultGroups = new ListItem("wmDefaultGroups", "Default Groups");
        TextBoxItem wmDefaultPrincipal = new TextBoxItem("wmDefaultPrincipal", "Default Principal");
        TextBoxItem wmSecurityDomain = new TextBoxItem("wmSecurityDomain", "Security Domain");
        CheckBoxItem wmMappingRequired = new CheckBoxItem("wmMappingRequired", "Mapping Required");

        wmForm.setFields(wmEnabled, wmDefaultGroups, wmDefaultPrincipal, wmSecurityDomain, wmMappingRequired);

        final FormHelpPanel wmHelpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = Baseadress.get();
                        address.add("subsystem", "resource-adapters");
                        address.add("resource-adapter", "*");
                        return address;
                    }
                }, wmForm
        );
        wmFormPanel.add(wmHelpPanel.asWidget());

        wmFormPanel.add(wmForm.asWidget());

        wmForm.setEnabled(false);


        propertyEditor = new PropertyEditor(this, true);

        // ----

        title = new HTML();
        title.setStyleName("content-header-label");

        OneToOneLayout layoutBuilder = new OneToOneLayout()
                .setPlain(true)
                .setTitle("Resource Adapter")
                .setHeadlineWidget(title)
                .setDescription(Console.CONSTANTS.subsys_jca_resource_adapter_desc())
                .addDetail("Attributes", attributesFormPanel)
                .addDetail("Work Manager Security", wmFormPanel)
                .addDetail("Properties", propertyEditor.asWidget());

        return layoutBuilder.build();
    }


    private ResourceAdapter getCurrentSelection() {
        return selectedEntity;
    }

    @Override
    public void onCreateProperty(String reference, PropertyRecord prop) {
        closePropertyDialoge();

        presenter.onCreateAdapterProperty(getCurrentSelection(), prop);
    }

    @Override
    public void onDeleteProperty(String reference, PropertyRecord prop) {
        presenter.onRemoveAdapterProperty(getCurrentSelection(), prop);
    }

    @Override
    public void onChangeProperty(String reference, PropertyRecord prop) {
        // not used
    }

    @Override
    public void launchNewPropertyDialoge(String reference) {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Config Property"));
        window.setWidth(480);
        window.setHeight(360);

        window.trapWidget(
                new NewPropertyWizard(this, "").asWidget()
        );

        window.setGlassEnabled(true);
        window.center();
    }

    @Override
    public void closePropertyDialoge() {
        window.hide();
    }

    public void setAdapter(ResourceAdapter ra) {
        propertyEditor.clearValues();

        this.selectedEntity = ra;
        title.setHTML("Resource Adapter "+ ra.getName());

        attributesForm.edit(ra);
        wmForm.edit(ra);
        propertyEditor.setProperties(ra.getName(), ra.getProperties());

    }
}
