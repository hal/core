package org.jboss.as.console.client.shared.state;

import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 8/22/13
 */
public class ResponseProcessorDelegate {

    static ResponseProcessor[] processors = {
            new DomainResponseProcessor(),
            new StandaloneResponseProcessor()
    };

    private ReloadState reloadState;

    public ResponseProcessorDelegate() {
        this.reloadState = new ReloadState();
    }

    public void process(ModelNode response) {

        for(ResponseProcessor proc : processors)
        {
            if(proc.accepts(response))
            {
                proc.process(response, reloadState);
                break;
            }
        }
    }
}
