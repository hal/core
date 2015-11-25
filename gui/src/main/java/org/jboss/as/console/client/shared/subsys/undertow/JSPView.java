package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.OneToOneLayout;
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
 * @since 08/09/14
 */
public class JSPView {

    final static AddressTemplate ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=undertow/servlet-container=*/setting=jsp");

    private final ServletPresenter presenter;
    private ModelNodeForm commonForm;
    private ModelNodeForm devForm;

    private static String[] DEV_ATTRIBUTES = new String[] {
            "development", "keep-generated", "modification-test-interval", "recompile-on-fail",
            "error-on-use-bean-invalid-class-attribute", "display-source-fragment",
            "check-interval"
    };
    public JSPView(ServletPresenter presenter) {
        this.presenter = presenter;
    }

    public void setData(ModelNode data) {
        commonForm.edit(data);
        devForm.edit(data);
    }

    public Widget asWidget() {

        final SecurityContext securityContext =
                presenter.getSecurityFramework().getSecurityContext(
                        presenter.getProxy().getNameToken()
                );

        final ResourceDescription definition = presenter.getDescriptionRegistry().lookup(ADDRESS);

        final ModelNodeFormBuilder.FormAssets commonAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .exclude(DEV_ATTRIBUTES)
                .build();

        final ModelNodeFormBuilder.FormAssets devAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .include(DEV_ATTRIBUTES)
                .build();

        commonForm = commonAssets.getForm();
        devForm = devAssets.getForm();

        FormCallback callback = new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                if(commonForm.getEditedEntity().isDefined()) {
                    presenter.onSaveContainerSettings(ADDRESS, changeset);
                }
                else
                {
                    presenter.onCreateContainerSettings(ADDRESS, commonForm.getUpdatedEntity());
                }
            }

            @Override
            public void onCancel(Object entity) {
                commonForm.cancel();
            }
        };

        commonAssets.getForm().setToolsCallback(callback);
        devAssets.getForm().setToolsCallback(callback);

        // ----
        OneToOneLayout layoutBuilder = new OneToOneLayout()
                .setPlain(true)
                .setHeadline("JSP Settings")
                .setDescription(definition.get("description").asString())
                .addDetail(Console.CONSTANTS.common_label_attributes(), commonAssets.asWidget())
                .addDetail("Development", devAssets.asWidget());

        return layoutBuilder.build();
    }
}
