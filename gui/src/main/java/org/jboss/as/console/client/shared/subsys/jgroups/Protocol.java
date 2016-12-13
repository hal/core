package org.jboss.as.console.client.shared.subsys.jgroups;

import java.util.HashMap;
import java.util.Map;

/**
 * The set of valid JGroups protocol types.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public enum Protocol {
    UNKNOWN(null),
    ASYM_ENCRYPT("ASYM_ENCRYPT"),
    AUTH("AUTH"),
    AZURE_PING("azure.AZURE_PING"),
    BARRIER("BARRIER"),
    BPING("BPING"),
    CENTRAL_EXECUTOR("CENTRAL_EXECUTOR"),
    CENTRAL_LOCK("CENTRAL_LOCK"),
    COMPRESS("COMPRESS"),
    COUNTER("COUNTER"),
    DAISYCHAIN("DAISYCHAIN"),
    ENCRYPT("ENCRYPT"),
    FC("FC"),
    FD("FD"),
    FD_ALL("FD_ALL"),
    FD_ALL2("FD_ALL2"),
    FD_HOST("FD_HOST"),
    FD_SOCK("FD_SOCK"),
    FILE_PING("FILE_PING"),
    FORWARD_TO_COORD("FORWARD_TO_COORD"),
    FRAG("FRAG"),
    FRAG2("FRAG2"),
    HDRS("HDRS"),
    GOOGLE_PING("GOOGLE_PING"),
    JDBC_PING("JDBC_PING"),
    MERGE2("MERGE2"),
    MERGE3("MERGE3"),
    MFC("MFC"),
    MPING("MPING"),
    FLUSH("pbcast.FLUSH"),
    GMS("pbcast.GMS"),
    NAKACK("pbcast.NAKACK"),
    NAKACK2("pbcast.NAKACK2"),
    STABLE("pbcast.STABLE"),
    STATE("pbcast.STATE"),
    STATE_SOCK("pbcast.STATE_SOCK"),
    STATE_TRANSFER("pbcast.STATE_TRANSFER"),
    PDC("PDC"),
    PEER_LOCK("PEER_LOCK"),
    PERF("PERF"),
    PING("PING"),
    RACKSPACE_PING("RACKSPACE_PING"),
    RELAY("RELAY"),
    RELAY2("relay.RELAY2"),
    RSVP("RSVP"),
    SUPERVISOR("rules.SUPERVISOR"),
    S3_PING("S3_PING"),
    SASL("SASL"),
    SEQUENCER("SEQUENCER"),
    SCOPE("SCOPE"),
    SHARED_LOOPBACK_PING("SHARED_LOOPBACK_PING"),
    STOMP("STOMP"),
    SWIFT_PING("SWIFT_PING"),
    SYM_ENCRYPT("SYM_ENCRYPT"),
    TCP("TCP"),
    TCP_GOSSIP("TCPGOSSIP"),
    TCP_PING("TCPPING"),
    TOA("tom.TOA"),
    TRACE("TRACE"),
    UDP("UDP"),
    UFC("UFC"),
    UNICAST("UNICAST"),
    UNICAST2("UNICAST2"),
    UNICAST3("UNICAST3"),
    VERIFY_SUSPECT("VERIFY_SUSPECT"),
    ;

    private final String name;

    Protocol(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this protocol.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Protocol> elements;

    static {
        final Map<String, Protocol> map = new HashMap<String, Protocol>();
        for (Protocol element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        elements = map;
    }


    public static Protocol forName(String localName) {
        final Protocol element = elements.get(localName);
        return element == null ? UNKNOWN : element;
    }
}

