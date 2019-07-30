package org.jboss.as.console.client.shared.state;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.notify.Notifications;

public abstract class AbstractWarningProcessor implements ResponseProcessor {
    protected static final String LEVEL = "level";
    protected static final String RESPONSE_HEADERS = "response-headers";
    protected static final String RESULT = "result";
    protected static final String STEP_1 = "step-1";
    protected static final String WARNING = "warning";
    protected static final String WARNINGS = "warnings";

    @Override
    public void process(ModelNode response, Map<String, ServerState> serverStates) {
        final List<ModelNode> warnings = fetchWarnings(response);
        for (ModelNode warning : warnings) {
            final WarningNotification warningNotification = new WarningNotification(warning.get(WARNING).asString(),
                    Level.parse(warning.get(LEVEL).asString()));
            Notifications.fireWarningNotification(warningNotification);
        }
    }
    
    protected abstract List<ModelNode> fetchWarnings(final ModelNode response);
}
