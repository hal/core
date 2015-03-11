<#-- @ftlvariable name="packageName" type="java.lang.String" -->
<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="bindings" type="java.util.Set<String>" -->
package ${packageName};

import javax.annotation.Generated;

import com.google.gwt.inject.client.AbstractGinModule;

/*
* WARNING! This class is generated. Do not modify.
*/
@Generated("org.jboss.hal.processors.GinProcessor")
public class ${className} extends AbstractGinModule {
    @Override
    protected void configure() {
        <#list bindings as binding>
        install(new ${binding}());
        </#list>
    }
}
