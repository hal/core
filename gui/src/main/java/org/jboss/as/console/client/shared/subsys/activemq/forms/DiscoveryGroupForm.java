package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.activemq.cluster.DiscoveryGroupList;
import org.jboss.as.console.client.shared.subsys.activemq.cluster.MsgClusteringPresenter;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.Property;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class DiscoveryGroupForm {

    boolean isCreate = false;
    private final MsgClusteringPresenter presenter;
    private ModelNodeFormBuilder.FormAssets formAssets;
    private Property data;


    public DiscoveryGroupForm(MsgClusteringPresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {
        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(NameTokens.ActivemqMsgClusteringPresenter);
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(DiscoveryGroupList.BASE_ADDRESS);

        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .build();

        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveResource(DiscoveryGroupList.BASE_ADDRESS, DiscoveryGroupForm.this.data.getName(), changeset);
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        formAssets.getForm().addFormValidator(new DiscoveryGroupFormValidator());

        return formAssets.asWidget();
    }

    public void setData(Property data) {
        this.data = data;
        formAssets.getForm().edit(data.getValue());
    }

    public void setIsCreate(boolean create) {
        isCreate = create;
    }

    public void setSocketBindings(List<String> socketBindings) {
        // the suggestions aren't populated yet
//        this.oracle.clear();
//        this.oracle.addAll(socketBindings);
    }

    public ModelNodeForm getForm() {
        return formAssets.getForm();
    }
}
