package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.activemq.GenericResourceView;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.JGROUPS_CHANNEL;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;
import static org.jboss.dmr.client.ModelDescriptionConstants.SOCKET_BINDING;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class DiscoveryGroupList extends GenericResourceView {

    public DiscoveryGroupList(final ResourceDescription resourceDescription,
            final MsgClusteringPresenter presenter, final String title,
            final AddressTemplate addressTemplate) {
        super(resourceDescription, presenter, title, addressTemplate);
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

}
