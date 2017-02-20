package org.jboss.as.console.client.shared.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.notify.Notifications;

public class WarningProcessor implements ResponseProcessor {

    protected static final String LEVEL = "level";
    protected static final String RESPONSE_HEADERS = "response-headers";
    protected static final String RESULT = "result";
    protected static final String STEP_1 = "step-1";
    protected static final String WARNING = "warning";
    protected static final String WARNINGS = "warnings";

    @Override
    public boolean accepts(ModelNode response) {
        // result->step-x->response-headers->warnings
        if (response.hasDefined(RESULT)) {
            final ModelNode result = response.get(RESULT);
            if (result.hasDefined(STEP_1)) {
                final List<Property> steps = result.asPropertyList();
                for (Property step : steps) {
                    final ModelNode stepContent = step.getValue();
                    if (stepContent.hasDefined(RESPONSE_HEADERS) && stepContent.get(RESPONSE_HEADERS).hasDefined(WARNINGS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void process(ModelNode response, Map<String, ServerState> serverStates) {
        final List<ModelNode> warnings = fetchWarnings(response);
        for (ModelNode warning : warnings) {
            final WarningNotification warningNotification = new WarningNotification(warning.get(WARNING).asString(),
                    Level.parse(warning.get(LEVEL).asString()));
            Notifications.fireWarningNotification(warningNotification);
        }
    }

    protected List<ModelNode> fetchWarnings(final ModelNode response) {
        List<ModelNode> warnings = new ArrayList<ModelNode>();
        for (Property step : response.get(RESULT).asPropertyList()) {
            final ModelNode value = step.getValue();
            if (value.hasDefined(RESPONSE_HEADERS) && value.get(RESPONSE_HEADERS).hasDefined(WARNINGS)) {
                warnings.addAll(value.get(RESPONSE_HEADERS).get(WARNINGS).asList());
            }
        }
        return warnings;
    }

}
