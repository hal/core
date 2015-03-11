package org.jboss.as.console.client.shared.subsys.remoting.store;

import org.jboss.as.console.client.EchoContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.StaticDispatcher;
import org.jboss.dmr.client.StaticDmrResponse;
import org.jboss.gwt.circuit.NoopChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.jboss.as.console.client.shared.subsys.remoting.store.RemotingStore.*;
import static org.junit.Assert.*;

/**
 * @author Harald Pehl
 */
public class RemotingStoreTest {

    // ------------------------------------------------------ setup

    private StaticDispatcher dispatcher;
    private RemotingStore store;

    @Before
    public void setUp() {
        dispatcher = new StaticDispatcher();
        store = new RemotingStore(dispatcher, new EchoContext());
    }


    // ------------------------------------------------------ read resources

    @Test
    public void endpointConfiguration() {
        // Verify two DMR requests:
        // 1. modify endpoint configuration
        // 2. refresh modified configuration
        // Push them in reverse order to the dispatcher stack
        ModelNode endpointConfiguration = new ModelNode();
        endpointConfiguration.get("foo").set("bar");
        dispatcher.push(StaticDmrResponse.ok(endpointConfiguration));
        dispatcher.push(StaticDmrResponse.ok(new ModelNode()));
        store.modifyEndpointConfiguration(new ModifyEndpointConfiguration(Collections.<String, Object>emptyMap()),
                NoopChannel.INSTANCE);

        assertEquals(endpointConfiguration, store.getEndpointConfiguration());
    }

    @Test
    public void readRemoteConnector() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.readConnector(new ReadConnector(REMOTE_CONNECTOR_ADDRESS), NoopChannel.INSTANCE);

        assertEquals(3, store.getRemoteConnectors().size());
        assertTrue(store.getRemoteHttpConnectors().isEmpty());
        assertTrue(store.getLocalOutboundConnections().isEmpty());
        assertTrue(store.getOutboundConnections().isEmpty());
        assertTrue(store.getRemoteOutboundConnections().isEmpty());
    }

    @Test
    public void readRemoteHttpConnector() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.readConnector(new ReadConnector(REMOTE_HTTP_CONNECTOR_ADDRESS), NoopChannel.INSTANCE);

        assertTrue(store.getRemoteConnectors().isEmpty());
        assertEquals(3, store.getRemoteHttpConnectors().size());
        assertTrue(store.getLocalOutboundConnections().isEmpty());
        assertTrue(store.getOutboundConnections().isEmpty());
        assertTrue(store.getRemoteOutboundConnections().isEmpty());
    }

    @Test
    public void readLocalOutboundConnection() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.readConnection(new ReadConnection(LOCAL_OUTBOUND_CONNECTION_ADDRESS), NoopChannel.INSTANCE);

        assertTrue(store.getRemoteConnectors().isEmpty());
        assertTrue(store.getRemoteHttpConnectors().isEmpty());
        assertEquals(3, store.getLocalOutboundConnections().size());
        assertTrue(store.getOutboundConnections().isEmpty());
        assertTrue(store.getRemoteOutboundConnections().isEmpty());
    }

    @Test
    public void readOutboundConnection() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.readConnection(new ReadConnection(OUTBOUND_CONNECTION_ADDRESS), NoopChannel.INSTANCE);

        assertTrue(store.getRemoteConnectors().isEmpty());
        assertTrue(store.getRemoteHttpConnectors().isEmpty());
        assertTrue(store.getLocalOutboundConnections().isEmpty());
        assertEquals(3, store.getOutboundConnections().size());
        assertTrue(store.getRemoteOutboundConnections().isEmpty());
    }

    @Test
    public void readRemoteOutboundConnection() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.readConnection(new ReadConnection(REMOTE_OUTBOUND_CONNECTION_ADDRESS), NoopChannel.INSTANCE);

        assertTrue(store.getRemoteConnectors().isEmpty());
        assertTrue(store.getRemoteHttpConnectors().isEmpty());
        assertTrue(store.getLocalOutboundConnections().isEmpty());
        assertTrue(store.getOutboundConnections().isEmpty());
        assertEquals(3, store.getRemoteOutboundConnections().size());
    }


    // ------------------------------------------------------ update resources

    @Test
    public void updateRemoteConnector() {
        // Verify the name of the last modified instance
        dispatcher.push(StaticDmrResponse.ok(propertyList(1, "foo")));
        dispatcher.push(StaticDmrResponse.ok(new ModelNode()));
        store.updateConnector(new UpdateConnector(REMOTE_CONNECTOR_ADDRESS, "foo", Collections.<String, Object>emptyMap()),
                NoopChannel.INSTANCE);

        assertEquals("foo", store.getLastModifiedInstance());
    }


    // ------------------------------------------------------ delete resources

    @Test
    public void deleteRemoteConnector() {
        // Make sure we execute two DMR requests
        // 1. Delete
        // 2. Refresh
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        dispatcher.push(StaticDmrResponse.ok(new ModelNode()));
        store.deleteConnector(new DeleteConnector(REMOTE_CONNECTOR_ADDRESS, "foo"), NoopChannel.INSTANCE);

        assertNull(store.getLastModifiedInstance());
        assertEquals(3, store.getRemoteConnectors().size());
        assertTrue(store.getRemoteHttpConnectors().isEmpty());
        assertTrue(store.getLocalOutboundConnections().isEmpty());
        assertTrue(store.getOutboundConnections().isEmpty());
        assertTrue(store.getRemoteOutboundConnections().isEmpty());
    }


    // ------------------------------------------------------ node factory methods

    private ModelNode propertyList(int size, String... names) {
        ModelNode list = new ModelNode();
        for (int i = 0; i < size; i++) {
            list.get(names[i]).set(new ModelNode());
        }
        return list;
    }
}