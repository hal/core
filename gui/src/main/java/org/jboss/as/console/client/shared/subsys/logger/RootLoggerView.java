package org.jboss.as.console.client.shared.subsys.logger;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
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
 */
public class RootLoggerView {

    final static AddressTemplate ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=logging/root-logger=ROOT");

    private final LoggerPresenter presenter;
    private ModelNodeForm commonForm;

    public RootLoggerView(LoggerPresenter presenter) {
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
                .setHeadline("Root Logger")
                .setDescription(definition.get("description").asString())
                .addDetail(Console.CONSTANTS.common_label_attributes(), commonAssets.asWidget());

        return layoutBuilder.build();
    }
}

