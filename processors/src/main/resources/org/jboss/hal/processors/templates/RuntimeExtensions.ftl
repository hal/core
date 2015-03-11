<#-- @ftlvariable name="packageName" type="java.lang.String" -->
<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="runtimeExtensions" type="java.util.List<org.jboss.as.console.client.plugins.RuntimeExtensionMetaData>" -->
package ${packageName};

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.List;

/*
* WARNING! This class is generated. Do not modify.
*/
@Generated("org.jboss.hal.processors.ExtensionProcessor")
public class ${className} implements RuntimeExtensionRegistry {

    private List<RuntimeExtensionMetaData> extensions;

    public RuntimeLHSItemExtensionRegistryImpl() {
        extensions = new ArrayList<RuntimeExtensionMetaData>();
        <#list runtimeExtensions as runtimeExtension>
        extensions.add(new RuntimeExtensionMetaData("${runtimeExtension.getName()}",
                "${runtimeExtension.getToken()}",
                "${runtimeExtension.getGroup()}",
                "${runtimeExtension.getKey()}"));
        </#list>
    }

    public List<RuntimeExtensionMetaData> getExtensions() {
        return extensions;
    }
}
