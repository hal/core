package org.jboss.as.console.client.shared.state;

import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.notify.Notifications;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 8/22/13
 */
public class ResponseProcessorDelegate {

    static ResponseProcessor[] processors = {
            new DomainResponseProcessor(),
            new StandaloneResponseProcessor(),
            new CompositeOperationWarningProcessor(),
            new WarningProcessor()
    };


    public ResponseProcessorDelegate() {

    }

    public void process(ModelNode response) {
        Map<String, ServerState> serverStates = new HashMap<String, ServerState>();

        for(ResponseProcessor proc : processors)
        {
            if(proc.accepts(response))
            {
                proc.process(response, serverStates);
                //break;
            }
        }

        if(serverStates.size()>0)
            Notifications.fireReloadNotification(new ReloadNotification(serverStates));

    }
}
