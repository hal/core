package org.jboss.as.console.client.shared.state;

import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.client.ModelNode;

public class WarningProcessor extends AbstractWarningProcessor {

    @Override
    public boolean accepts(ModelNode response) {
        // response-headers->warnings
        if (response.hasDefined(RESPONSE_HEADERS)) {
            final ModelNode result = response.get(RESPONSE_HEADERS);
            if (result.hasDefined(WARNINGS)) {
                return true;
            }
        }
        return false;
    }

    protected List<ModelNode> fetchWarnings(final ModelNode response) {
        List<ModelNode> warnings = new ArrayList<ModelNode>();
        warnings.addAll(response.get(RESPONSE_HEADERS).get(WARNINGS).asList());
        return warnings;
    }

}
