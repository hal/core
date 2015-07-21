package org.jboss.as.console.client.shared.subsys.ws;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 1/11/12
 */
public class ProviderEditor {

    private static final AddressTemplate BASE_ADDRESS = AddressTemplate.of("{selected.profile}/subsystem=webservices");

    private WebServicePresenter presenter;
    private ModelNodeForm form;

    public ProviderEditor(WebServicePresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {

        SecurityContext securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(BASE_ADDRESS);

        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();

        form = formAssets.getForm();

        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveResource(BASE_ADDRESS, changeset);
            }

            @Override
            public void onCancel(Object entity) {
                form.cancel();
            }
        });

        SimpleLayout layout = new SimpleLayout()
                .setPlain(true)
                .setTitle("Provider")
                .setHeadline("Web Services Provider")
                .setDescription(Console.CONSTANTS.subsys_ws_desc())
                .addContent("", formAssets.asWidget()
                );

        return layout.build();

    }


    public void reset() {
        form.clearValues();
    }

    public void updateFrom(ModelNode modelNode) {
        form.edit(modelNode);
    }
}
