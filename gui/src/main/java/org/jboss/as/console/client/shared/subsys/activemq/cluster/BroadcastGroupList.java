package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import java.util.List;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.activemq.GenericResourceView;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.JGROUPS_CHANNEL;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;
import static org.jboss.dmr.client.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.dmr.client.ModelDescriptionConstants.NILLABLE;
import static org.jboss.dmr.client.ModelDescriptionConstants.REQUIRED;
import static org.jboss.dmr.client.ModelDescriptionConstants.SOCKET_BINDING;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class BroadcastGroupList extends GenericResourceView {

    public BroadcastGroupList(final ResourceDescription resourceDescription,
            final MsgClusteringPresenter presenter, final String title,
            final AddressTemplate addressTemplate) {
        super(resourceDescription, presenter, title, addressTemplate);

        // connectors attribute should be required=true in the subsystem
        // https://issues.jboss.org/browse/JBEAP-4530
        ModelNode connectorsDesc = resourceDescription.get(ATTRIBUTES).get("connectors");
        connectorsDesc.get(REQUIRED).set(true);
        connectorsDesc.get(NILLABLE).set(false);
    }

    @Override
    protected void customEditFormBuilder(final ModelNodeFormBuilder formBuilder) {
        formBuilder.requiresAtLeastOne("jgroups-channel", SOCKET_BINDING)
                .addFactory(SOCKET_BINDING, attributeDescription -> {
                    SuggestionResource suggestionResource = new SuggestionResource(SOCKET_BINDING, "Socket binding",
                            false,
                            Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("jgroups-channel", attributeDescription -> {
                    SuggestionResource suggestionResource = new SuggestionResource("jgroups-channel", "Jgroups channel",
                            false,
                            Console.MODULES.getCapabilities().lookup(JGROUPS_CHANNEL));
                    return suggestionResource.buildFormItem();
                });
    }

    @Override
    protected void customAddFormBuilder(final ModelNodeFormBuilder formBuilder) {
        formBuilder
                .requiresAtLeastOne("jgroups-channel", SOCKET_BINDING)
                .addFactory(SOCKET_BINDING, attributeDescription -> {
                    SuggestionResource suggestionResource = new SuggestionResource(SOCKET_BINDING, "Socket binding",
                            false,
                            Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("jgroups-channel", attributeDescription -> {
                    SuggestionResource suggestionResource = new SuggestionResource("jgroups-channel", "Jgroups channel",
                            false,
                            Console.MODULES.getCapabilities().lookup(JGROUPS_CHANNEL));
                    return suggestionResource.buildFormItem();
                })
        ;
    }

    @Override
    protected void addFormValidatorOnAddDialog(final List<FormItem> formItemList, final FormValidation formValidation) {
        FormItem connectorsFormItem = findFormItem(formItemList, "connectors");
        List connectors = (List) connectorsFormItem.getValue();
        if (connectors.size() == 0) {
            formValidation.addError("connectors");
            connectorsFormItem.setErrMessage("Value must not be empty.");
            connectorsFormItem.setErroneous(true);
        }
    }

}
