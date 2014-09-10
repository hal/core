package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.as.console.mbui.widgets.ModelDrivenWidget;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;

import java.util.Map;

/**
 * @author Heiko Braun
 * @since 09/09/14
 */
public class EEGlobalAttributesView extends ModelDrivenWidget {

    final static String RESOURCE_ADDRESS= "{selected.profile}/subsystem=ee";
    private ModelNodeForm form;

    private final EEPresenter presenter;
    public EEGlobalAttributesView(EEPresenter presenter) {
        super(RESOURCE_ADDRESS);
        this.presenter = presenter;
    }

    public void setData(ModelNode data) {
        if(form!=null)
            form.edit(data);
        // else replay
    }

    @Override
    public Widget buildWidget(ResourceAddress address, ResourceDefinition definition) {
        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());

        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setFields(
                        "annotation-property-replacement",
                        "ear-subdeployments-isolated",
                        "jboss-descriptor-property-replacement",
                        "spec-descriptor-property-replacement"
                )
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();

        form = formAssets.getForm();
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveResource(RESOURCE_ADDRESS, null, changeset);
            }

            @Override
            public void onCancel(Object entity) {
                form.cancel();
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(form.asWidget());

        // ----
        return formPanel;
    }
}
