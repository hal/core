package org.jboss.as.console.client.shared.subsys.ejb3;

import java.util.Map;

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

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.EJB_THREAD_POOL;

/**
 * @author Heiko Braun
 * @since 08/09/14
 */
public class TimerSvcView {

    final static AddressTemplate ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=ejb3/service=timer-service");

    private final EJB3Presenter presenter;
    private ModelNodeForm commonForm;

    public TimerSvcView(EJB3Presenter presenter) {
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
                .addFactory("thread-pool-name", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("thread-pool-name", "Thread pool name", true,
                            Console.MODULES.getCapabilities().lookup(EJB_THREAD_POOL));
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
                .setHeadline("Timer Service")
                .setDescription(definition.get("description").asString())
                .addDetail(Console.CONSTANTS.common_label_attributes(), commonAssets.asWidget());

        return layoutBuilder.build();
    }
}

