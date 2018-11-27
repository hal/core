package org.jboss.as.console.client.shared.subsys.jca;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.PropertyEditor;
import org.jboss.as.console.client.v3.widgets.SubResourceAddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.SubResourcePropertyManager;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.SECURITY_DOMAIN;

/**
 * @author Heiko Braun
 * @date 12/12/11
 */
public class AdapterDetails {

    private final static AddressTemplate BASE_ADDRESS = AddressTemplate
            .of("{selected.profile}/subsystem=resource-adapters/resource-adapter=*");
    private final static AddressTemplate CONFIG_PROPS = BASE_ADDRESS.append("config-properties=*");

    private ResourceAdapterPresenter presenter;

    private ModelNodeForm attributesForm;
    private ModelNodeForm workmanagerForm;

    private HTML title;
    private Property selectedEntity;
    private PropertyEditor configProperties;
    private SubResourceAddPropertyDialog addDialog;

    public AdapterDetails(ResourceAdapterPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {

        title = new HTML();
        title.setStyleName("content-header-label");

        // forms
        final SecurityContext securityContext = presenter.getSecurityFramework()
                .getSecurityContext(presenter.getProxy().getNameToken());

        ResourceDescription description = presenter.getDescriptionRegistry().lookup(BASE_ADDRESS);

        ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setAddress(BASE_ADDRESS.getTemplate())
                .setConfigOnly()
                .setSecurityContext(securityContext)
                .setResourceDescription(description)
                .include("archive", "beanvalidationgroups", "bootstrap-context",
                        "module", "statistics-enabled", "transaction-support")
                .build();


        ModelNodeFormBuilder.FormAssets formAssets2 = new ModelNodeFormBuilder()
                .setAddress(BASE_ADDRESS.getTemplate())
                .setConfigOnly()
                .setSecurityContext(securityContext)
                .setResourceDescription(description)
                .createValidators(true)
                .include("wm-security", "wm-elytron-security-domain", "wm-security-default-groups",
                        "wm-security-default-principal", "wm-security-domain",
                        "wm-security-mapping-required", "wm-security-mapping-users", "wm-security-mapping-groups")
                .addFactory("wm-security-domain",
                        attributeDescription -> new SuggestionResource("wm-security-domain", "Wm Security Domain",
                                false, Console.MODULES.getCapabilities().lookup(SECURITY_DOMAIN)).buildFormItem())
                .build();


        SubResourcePropertyManager propertyManager = new SubResourcePropertyManager(CONFIG_PROPS,
                presenter.getStatementContext(), presenter.getDispatcher()) {
            @Override
            public void onAdd(Property property, AddPropertyDialog addDialog) {
                presenter.onCreateProperty(CONFIG_PROPS, property.getValue(), property.getName());
                AdapterDetails.this.addDialog.hide();
            }

            @Override
            public void onRemove(Property property) {
                presenter.onRemoveProperty(CONFIG_PROPS, property.getName());
                AdapterDetails.this.addDialog.hide();
            }
        };

        addDialog =
                new SubResourceAddPropertyDialog(
                        propertyManager,
                        securityContext,
                        description.getChildDescription("config-properties")
                );

        configProperties = new PropertyEditor.Builder(propertyManager)
                .addDialog(addDialog)
                .operationAddress(CONFIG_PROPS) // this address is used in the security context
                .build();

        FormCallback callback = new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSave(BASE_ADDRESS, selectedEntity.getName(), changeset);
            }

            @Override
            public void onCancel(Object entity) {

            }
        };

        attributesForm = formAssets.getForm();
        attributesForm.setToolsCallback(callback);

        workmanagerForm = formAssets2.getForm();
        workmanagerForm.setToolsCallback(callback);

        OneToOneLayout layoutBuilder = new OneToOneLayout()
                .setPlain(true)
                .setTitle("Resource Adapter")
                .setHeadlineWidget(title)
                .setDescription(description.get("description").asString())
                .addDetail(Console.CONSTANTS.common_label_attributes(), formAssets.asWidget())
                .addDetail("Work Manager Security", formAssets2.asWidget())
                .addDetail(Console.CONSTANTS.common_label_properties(), configProperties.asWidget()
                );


        return layoutBuilder.build();
    }

    public void setAdapter(Property ra) {

        this.selectedEntity = ra;
        title.setHTML("Resource Adapter " + SafeHtmlUtils.fromString(ra.getName()).asString());

        attributesForm.edit(ra.getValue());
        workmanagerForm.edit(ra.getValue());

        List<Property> props = ra.getValue().hasDefined("config-properties") ?
                ra.getValue().get("config-properties").asPropertyList() : Collections.EMPTY_LIST;

        configProperties.update(props);
    }
}
