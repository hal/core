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
 * @since 08/09/14
 */
public class JSPView extends ModelDrivenWidget {

    final static String RESOURCE_ADDRESS= "{selected.profile}/subsystem=undertow/servlet-container={selected.container}/setting=jsp";
    private final ServletPresenter presenter;
    private ModelNodeForm form;

    public JSPView(ServletPresenter presenter) {
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
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();


        form = formAssets.getForm();
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveResource(RESOURCE_ADDRESS, "jsp", changeset);
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
        SimpleLayout layoutBuilder = new SimpleLayout()
                .setPlain(true)
                .setHeadline("JSP Settings")
                .setDescription("JSP container configuration.")
                .addContent("", formPanel);

        return layoutBuilder.build();
    }
}
