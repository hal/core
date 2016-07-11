package org.jboss.as.console.client.shared.subsys.activemq.forms;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.activemq.cluster.BroadcastGroupList;
import org.jboss.as.console.client.shared.subsys.activemq.cluster.MsgClusteringPresenter;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.JGROUPS_CHANNEL;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.JGROUPS_STACK;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class BroadcastGroupForm {


    private final MsgClusteringPresenter presenter;

    private boolean isCreate;
    private Property data;
    private ModelNodeFormBuilder.FormAssets formAssets;

    public BroadcastGroupForm(MsgClusteringPresenter presenter) {

        this.presenter = presenter;
    }

    public Widget asWidget() {

        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(NameTokens.ActivemqMsgClusteringPresenter);
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(BroadcastGroupList.BASE_ADDRESS);

        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .addFactory("socket-binding", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("socket-binding", "Socket binding", false,
                            Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("jgroups-stack", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("jgroups-stack", "Jgroups stack", false,
                            Console.MODULES.getCapabilities().lookup(JGROUPS_STACK));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("jgroups-channel", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("jgroups-channel", "Jgroups channel", false,
                            Console.MODULES.getCapabilities().lookup(JGROUPS_CHANNEL));
                    return suggestionResource.buildFormItem();
                })
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();


        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveResource(BroadcastGroupList.BASE_ADDRESS, BroadcastGroupForm.this.data.getName(), changeset);
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

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
        /*this.oracle.clear();
        this.oracle.addAll(socketBindings);*/
    }

    public ModelNodeForm getForm() {
        return formAssets.getForm();
    }
}
