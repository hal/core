<#-- @ftlvariable name="packageName" type="java.lang.String" -->
<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="categories" type="java.util.Set<String>" -->
<#-- @ftlvariable name="factories" type="java.util.Set<String>" -->
package ${packageName};

import javax.annotation.Generated;

import com.google.web.bindery.autobean.shared.AutoBeanFactory;

/*
* WARNING! This class is generated. Do not modify.
*/
@Generated("org.jboss.hal.processors.BeanFactoryProcessor")
<#if (categories?size > 0)>
@AutoBeanFactory.Category({<#list categories as category>${category}.class<#if category_has_next>,<#else>})</#if></#list>
</#if>
public interface ${className} extends
        <#list factories as factory>
        ${factory},
        </#list>
        AutoBeanFactory {
}
