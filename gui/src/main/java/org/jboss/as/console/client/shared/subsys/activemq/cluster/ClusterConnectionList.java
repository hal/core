package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import org.jboss.as.console.client.shared.subsys.activemq.GenericResourceView;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class ClusterConnectionList extends GenericResourceView {


    public ClusterConnectionList(final ResourceDescription resourceDescription,
            final MsgClusteringPresenter presenter, final String title,
            final AddressTemplate addressTemplate) {
        super(resourceDescription, presenter, title, addressTemplate);
    }

    @Override
    protected void onAddCallbackBeforeSubmit(final ModelNode payload) {
        // "allow-direct-connections-only" requires static-connectors
        // but the "allow-direct-connections-only" attribute is in the payload even if it is false
        // so it is removed from the payload, otherwise the add operation will fail as static-connector is not set.
        if (payload.hasDefined("allow-direct-connections-only") && !payload.get("allow-direct-connections-only").asBoolean()) {
            payload.remove("allow-direct-connections-only");
        }
    }

    @Override
    protected void customAddFormBuilder(ModelNodeFormBuilder formBuilder) {
        formBuilder.requiresAtLeastOne("discovery-group", "static-connectors");
    }
}
