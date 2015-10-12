package org.jboss.as.console.client.shared.subsys.ws;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.subsys.ws.model.WebServiceEndpoint;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

/**
 * @author Heiko Braun
 * @date 1/24/12
 */
public class BaseRegistry {

    final static String NO_METRICS = "WFLYWS0037: No metrics available";

    DispatchAsync dispatcher;BeanFactory factory;

    public BaseRegistry(BeanFactory factory, DispatchAsync dispatcher) {
        this.factory = factory;
        this.dispatcher = dispatcher;
    }

    protected void parseEndpoints(ModelNode model, List<WebServiceEndpoint> endpoints) {
        if(model.hasDefined(RESULT))
        {
            List<ModelNode> modelNodes = model.get(RESULT).asList();

            for(ModelNode node : modelNodes)
            {

                List<Property> addressTokens = node.get(ADDRESS).asPropertyList();

                ModelNode value = node.get(RESULT).asObject();
                WebServiceEndpoint endpoint = factory.webServiceEndpoint().as();

                endpoint.setName(value.get("name").asString());
                endpoint.setClassName(value.get("class").asString());
                endpoint.setContext(value.get("context").asString());
                endpoint.setType(value.get("type").asString());
                endpoint.setWsdl(value.get("wsdl-url").asString());
                endpoint.setDeployment(addressTokens.get(0).getValue().asString());

                // the following needs 'statistics-enabled == true'
                // TODO Is this error message valid / stable across community & product versions?
                if (NO_METRICS.equals(value.get("request-count").asString())) {
                    Console.warning("Web Service statistics are not enabled", "To see runtime data like number of requests, please turn on statistics in the webservice configuration.");
                } else {
                    endpoint.setRequestCount(value.get("request-count").asInt(0));
                    endpoint.setResponseCount(value.get("response-count").asInt(0));
                    endpoint.setFaultCount(value.get("fault-count").asInt(0));
                    endpoint.setMinProcessingTime(value.get("min-processing-time").asInt(0));
                    endpoint.setAverageProcessingTime(value.get("average-processing-time").asInt(0));
                    endpoint.setMaxProcessingTime(value.get("max-processing-time").asInt(0));
                    endpoint.setTotalProcessingTime(value.get("total-processing-time").asInt(0));
                }
                endpoints.add(endpoint);
            }
        }
    }
}
