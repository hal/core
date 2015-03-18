<#-- @ftlvariable name="packageName" type="java.lang.String" -->
<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="storeInfos" type="java.util.Set<org.jboss.hal.processors.StoreInitProcessor.StoreInfo>" -->
package ${packageName};

import javax.annotation.Generated;

import org.jboss.as.console.spi.GinExtension;

/*
* WARNING! This class is generated. Do not modify.
*/
@GinExtension
@Generated("org.jboss.hal.processors.StoreInitProcessor")
public interface ${className} {

    <#list storeInfos as storeInfo>
    ${storeInfo.packageName}.${storeInfo.storeDelegate} get${storeInfo.storeDelegate}();
    ${storeInfo.packageName}.${storeInfo.storeAdapter} get${storeInfo.storeAdapter}();
    </#list>
}
