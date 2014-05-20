package org.jboss.as.console.client.shared.subsys.ws.model;

import org.jboss.as.console.client.widgets.forms.Binding;

/**
 * @author Heiko Braun
 * @date 6/10/11
 */
public interface WebServiceEndpoint {

    String getName();
    void setName(String name);

    String getContext();
    void setContext(String context);

    String getClassName();
    void setClassName(String classname);

    String getType();
    void setType(String type);

    @Binding(detypedName = "wsdl-url")
    String getWsdl();
    void setWsdl(String wsdl);

    @Binding(skip = true)
    String getDeployment();
    void setDeployment(String name);

    @Binding(detypedName = "min-processing-time")
    Integer getMinProcessingTime();
    void setMinProcessingTime(Integer minProcessingTime);

    @Binding(detypedName = "average-processing-time")
    Integer getAverageProcessingTime();
    void setAverageProcessingTime(Integer averageProcessingTime);

    @Binding(detypedName = "max-processing-time")
    Integer getMaxProcessingTime();
    void setMaxProcessingTime(Integer maxProcessingTime);

    @Binding(detypedName = "total-processing-time")
    Integer getTotalProcessingTime();
    void setTotalProcessingTime(Integer totalProcessingTime);

    @Binding(detypedName = "fault-count")
    Integer getFaultCount();
    void setFaultCount(Integer faultCount);

    @Binding(detypedName = "request-count")
    Integer getRequestCount();
    void setRequestCount(Integer requestCount);

    @Binding(detypedName = "response-count")
    Integer getResponseCount();
    void setResponseCount(Integer responseCount);
}
