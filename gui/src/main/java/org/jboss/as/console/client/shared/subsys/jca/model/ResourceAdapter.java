package org.jboss.as.console.client.shared.subsys.jca.model;

import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 7/19/11
 */
@Address("/subsystem=resource-adapters/resource-adapter={0}")
public interface ResourceAdapter {

    @Binding(key = true)
    String getName();
    void setName(String name);

    String getArchive();
    void setArchive(String archive);

    String getModule();
    void setModule(String module);

    @Binding(detypedName = "transaction-support")
    String getTransactionSupport();
    void setTransactionSupport(String txSupport);

    @Binding(detypedName = "bootstrap-context")
    String getBootstrapContext();
    void setBootstrapContext(String s);

    @Binding(detypedName = "statistics-enabled")
    boolean isStatisticsEnabled();
    void setStatisticsEnabled(boolean b);

    @Binding(detypedName = "beanvalidationgroups", listType="java.lang.String")
    List<String> getBeanValidationGroups();
    void setBeanValidationGroups(List<String> list);

    @Binding(detypedName = "wm-security")
    boolean isWmEnabled();
    void setWmEnabled(boolean b);

    @Binding(detypedName = "wm-security-default-groups", listType="java.lang.String")
    List<String> getWmDefaultGroups();
    void setWmDefaultGroups(List<String> list);

    @Binding(detypedName = "wm-security-default-principal")
    String getWmDefaultPrincipal();
    void setWmDefaultPrincipal(String s);

    @Binding(detypedName = "wm-security-domain")
    String getWmSecurityDomain();
    void setWmSecurityDomain(String s);

    @Binding(detypedName = "wm-security-mapping-required")
    boolean isWmMappingRequired();
    void setWmMappingRequired(boolean b);

    @Binding(skip = true)
    List<PropertyRecord> getProperties();
    void setProperties(List<PropertyRecord> props);

    @Binding(skip = true)
    void setConnectionDefinitions(List<ConnectionDefinition> connections);
    List<ConnectionDefinition> getConnectionDefinitions();

    @Binding(skip=true)
    List<AdminObject> getAdminObjects();
    void setAdminObjects(List<AdminObject> admins);

}
