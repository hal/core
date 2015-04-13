package org.jboss.as.console.client.shared.subsys.undertow;

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
 * @since 04/04/15
 */
public class SubsystemView extends ModelDrivenWidget {

    private static final String BASE_ADDRESS = "{selected.profile}/subsystem=undertow";

    HttpPresenter presenter;
    private ModelNodeForm form;

    public SubsystemView(HttpPresenter presenter) {
        super(BASE_ADDRESS);
        this.presenter = presenter;
    }

    @Override
    public Widget buildWidget(ResourceAddress address, ResourceDefinition definition) {

        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());

        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();


        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {

                presenter.onSaveResource(
                        BASE_ADDRESS, null, changeset
                );
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        form = formAssets.getForm();

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());

        // ----
        SimpleLayout layoutBuilder = new SimpleLayout()
                .setPlain(true)
                .setHeadline("General Configuration")
                .setDescription("The general configuration for the HTTP subsystem.")
                .addContent("Attributes", formPanel);

        return layoutBuilder.build();

    }

    public void updateFrom(ModelNode data) {
        if(form!=null)
            form.edit(data);
    }
}
