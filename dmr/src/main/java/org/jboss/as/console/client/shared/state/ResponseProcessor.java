package org.jboss.as.console.client.shared.state;

import org.jboss.dmr.client.ModelNode;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 1/17/12
 */
public interface ResponseProcessor {

    boolean accepts(ModelNode response);
    void process(ModelNode response, Map<String, ServerState> serverStates);
}
