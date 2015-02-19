package org.jboss.as.console.client.shared.subsys.remoting.store;

import org.jboss.as.console.client.EchoContext;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.StaticDispatcher;
import org.jboss.dmr.client.StaticDmrResponse;
import org.jboss.gwt.circuit.NoopChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.jboss.as.console.client.v3.stores.CrudAction.Crud.DELETE;
import static org.jboss.as.console.client.v3.stores.CrudAction.Crud.READ;
import static org.jboss.as.console.client.v3.stores.CrudAction.Crud.UPDATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        store.modifyEndpointConfiguration(Collections.<String, Object>emptyMap(), NoopChannel.INSTANCE);

        assertEquals(endpointConfiguration, store.getEndpointConfiguration());
    }

    @Test
    public void readRemoteConnector() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.crudRemoteConnector(READ, AddressTemplate.of("/subsystem=remoting/connector=*"),
                null, new ModelNode(), Collections.<String, Object>emptyMap(), NoopChannel.INSTANCE);

        assertEquals(3, store.getRemoteConnectors().size());
        assertTrue(store.getRemoteHttpConnectors().isEmpty());
        assertTrue(store.getLocalOutboundConnections().isEmpty());
        assertTrue(store.getOutboundConnections().isEmpty());
        assertTrue(store.getRemoteOutboundConnections().isEmpty());
    }

    @Test
    public void readRemoteHttpConnector() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.crudRemoteConnector(READ, AddressTemplate.of("/subsystem=remoting/http-connector=*"),
                null, new ModelNode(), Collections.<String, Object>emptyMap(), NoopChannel.INSTANCE);

        assertTrue(store.getRemoteConnectors().isEmpty());
        assertEquals(3, store.getRemoteHttpConnectors().size());
        assertTrue(store.getLocalOutboundConnections().isEmpty());
        assertTrue(store.getOutboundConnections().isEmpty());
        assertTrue(store.getRemoteOutboundConnections().isEmpty());
    }

    @Test
    public void readLocalOutboundConnection() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.crudOutboundConnection(READ, AddressTemplate.of("/subsystem=remoting/local-outbound-connection=*"),
                null, new ModelNode(), Collections.<String, Object>emptyMap(), NoopChannel.INSTANCE);

        assertTrue(store.getRemoteConnectors().isEmpty());
        assertTrue(store.getRemoteHttpConnectors().isEmpty());
        assertEquals(3, store.getLocalOutboundConnections().size());
        assertTrue(store.getOutboundConnections().isEmpty());
        assertTrue(store.getRemoteOutboundConnections().isEmpty());
    }

    @Test
    public void readOutboundConnection() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.crudOutboundConnection(READ, AddressTemplate.of("/subsystem=remoting/outbound-connection=*"),
                null, new ModelNode(), Collections.<String, Object>emptyMap(), NoopChannel.INSTANCE);

        assertTrue(store.getRemoteConnectors().isEmpty());
        assertTrue(store.getRemoteHttpConnectors().isEmpty());
        assertTrue(store.getLocalOutboundConnections().isEmpty());
        assertEquals(3, store.getOutboundConnections().size());
        assertTrue(store.getRemoteOutboundConnections().isEmpty());
    }

    @Test
    public void readRemoteOutboundConnection() {
        dispatcher.push(StaticDmrResponse.ok(propertyList(3, "a", "b", "c")));
        store.crudOutboundConnection(READ, AddressTemplate.of("/subsystem=remoting/remote-outbound-connection=*"),
                null, new ModelNode(), Collections.<String, Object>emptyMap(), NoopChannel.INSTANCE);

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
        store.crudRemoteConnector(UPDATE, AddressTemplate.of("/subsystem=remoting/remote-connector=*"),
                "foo", new ModelNode(), Collections.<String, Object>emptyMap(), NoopChannel.INSTANCE);

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
        store.crudRemoteConnector(DELETE, AddressTemplate.of("/subsystem=remoting/connector=*"),
                "foo", new ModelNode(), Collections.<String, Object>emptyMap(), NoopChannel.INSTANCE);

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