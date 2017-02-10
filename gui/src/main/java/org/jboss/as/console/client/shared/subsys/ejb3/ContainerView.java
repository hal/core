package org.jboss.as.console.client.shared.subsys.ejb3;

import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.EJB_APPLICATION_SECURITY_DOMAIN;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.EJB_CACHE;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.SECURITY_DOMAIN;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.STATELESS_SESSION_BEAN_POOL;

/**
 * @author Heiko Braun
 * @since 08/09/14
 */
public class ContainerView {

    final static AddressTemplate ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=ejb3");

    private final EJB3Presenter presenter;
    private ModelNodeForm commonForm;

    public ContainerView(EJB3Presenter presenter) {
        this.presenter = presenter;
    }

    public void setData(ModelNode data) {
        commonForm.edit(data);
    }

    public Widget asWidget() {

        final SecurityContext securityContext =
                presenter.getSecurityFramework().getSecurityContext(
                        presenter.getProxy().getNameToken()
                );

        final ResourceDescription definition = presenter.getDescriptionRegistry().lookup(ADDRESS);

        final ModelNodeFormBuilder.FormAssets commonAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .addFactory("default-entity-bean-instance-pool", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("default-entity-bean-instance-pool", "Default entity bean instance pool", false,
                            Console.MODULES.getCapabilities().lookup(STATELESS_SESSION_BEAN_POOL));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("default-mdb-instance-pool", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("default-mdb-instance-pool", "Default mdb instance pool", false,
                            Console.MODULES.getCapabilities().lookup(STATELESS_SESSION_BEAN_POOL));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("default-slsb-instance-pool", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("default-slsb-instance-pool", "Default slsb instance pool", false,
                            Console.MODULES.getCapabilities().lookup(STATELESS_SESSION_BEAN_POOL));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("default-security-domain", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("default-security-domain", "Default security domain", false,
                            Iterables.concat(Console.MODULES.getCapabilities().lookup(SECURITY_DOMAIN),
                                    Console.MODULES.getCapabilities().lookup(EJB_APPLICATION_SECURITY_DOMAIN)));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("default-sfsb-cache", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("default-sfsb-cache", "Default sfsb cache", false,
                            Console.MODULES.getCapabilities().lookup(EJB_CACHE));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("default-sfsb-passivation-disabled-cache", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("default-sfsb-passivation-disabled-cache", "Default sfsb passivation disabled cache", false,
                            Console.MODULES.getCapabilities().lookup(EJB_CACHE));
                    return suggestionResource.buildFormItem();
                })

                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .build();

        commonForm = commonAssets.getForm();

        FormCallback callback = new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                if(commonForm.getEditedEntity().isDefined()) {
                    presenter.onSaveResource(ADDRESS, changeset);
                }
            }

            @Override
            public void onCancel(Object entity) {
                commonForm.cancel();
            }
        };

        commonAssets.getForm().setToolsCallback(callback);

        // ----
        OneToOneLayout layoutBuilder = new OneToOneLayout()
                .setPlain(true)
                .setHeadline("EJB Container Settings")
                .setDescription(definition.get("description").asString())
                .addDetail(Console.CONSTANTS.common_label_attributes(), commonAssets.asWidget());

        return layoutBuilder.build();
    }
}

