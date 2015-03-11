<#-- @ftlvariable name="packageName" type="java.lang.String" -->
<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="subsystemExtensions" type="java.util.List<org.jboss.as.console.client.plugins.SubsystemExtensionMetaData>" -->
package ${packageName};

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.List;

/*
* WARNING! This class is generated. Do not modify.
*/
@Generated("org.jboss.hal.processors.ExtensionProcessor")
public class ${className} implements SubsystemRegistry {

    private List<SubsystemExtensionMetaData> extensions;

    public SubsystemRegistryImpl() {
        extensions = new ArrayList<>();
        <#list subsystemExtensions as subsystemExtension>
        extensions.add(new SubsystemExtensionMetaData("${subsystemExtension.getName()}",
                "${subsystemExtension.getToken()}",
                "${subsystemExtension.getGroup()}",
                "${subsystemExtension.getKey()}"));
        </#list>
    }

    public List<SubsystemExtensionMetaData> getExtensions() {
        return extensions;
    }
}
