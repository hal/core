<#-- @ftlvariable name="packageName" type="java.lang.String" -->
<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="ginjectors" type="java.util.Set<String>" -->
<#-- @ftlvariable name="compositeBinding" type="java.lang.String" -->
package ${packageName};

import javax.annotation.Generated;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/*
* WARNING! This class is generated. Do not modify.
*/
@Generated("org.jboss.hal.processors.GinProcessor")
@GinModules(${compositeBinding}.class)
public interface ${className} extends
        <#list ginjectors as ginjector>
        ${ginjector},
        </#list>
        Ginjector {

    ${className} INSTANCE = GWT.create(${className}.class);
}
