package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.user.client.ui.VerticalPanel;
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
 * @since 04/04/15
 */
public class SubsystemView {

    private static final AddressTemplate BASE_ADDRESS = AddressTemplate.of("{selected.profile}/subsystem=undertow");

    HttpPresenter presenter;
    private ModelNodeForm form;

    public SubsystemView(HttpPresenter presenter) {

        this.presenter = presenter;
    }

    public Widget asWidget() {

        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(BASE_ADDRESS);

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
