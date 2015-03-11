<#-- @ftlvariable name="packageName" type="java.lang.String" -->
<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="storeInfos" type="java.util.Set<org.jboss.hal.processors.StoreInitProcessor.StoreInfo>" -->
package ${packageName};

import javax.annotation.Generated;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/*
* WARNING! This class is generated. Do not modify.
*/
@Generated("org.jboss.hal.processors.StoreInitProcessor")
public class ${className} implements Function<BootstrapContext> {

    @Override
    public void execute(Control<BootstrapContext> control) {
        // instantiate store adapters to register the store callback handlers
        <#list storeInfos as storeInfo>
        Console.MODULES.get${storeInfo.storeAdapter}();
        </#list>

        control.proceed();
    }
}
