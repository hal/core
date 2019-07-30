package org.jboss.as.console.client.shared.state;

import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

public class CompositeOperationWarningProcessor extends AbstractWarningProcessor {

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