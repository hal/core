<#-- @ftlvariable name="packageName" type="java.lang.String" -->
<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="tokenInfos" type="java.util.Set<org.jboss.hal.processors.NameTokenProcessor.NameTokenInfo>" -->
package ${packageName};

import com.google.common.collect.HashMultimap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.Generated;

import static java.util.Arrays.asList;

/*
* WARNING! This class is generated. Do not modify.
*/
@Generated("org.jboss.hal.processors.NameTokenProcessor")
public class ${className} implements RequiredResourcesRegistry {

    private final Set<String> tokens;
    private final HashMultimap<String, String> resources;
    private final HashMultimap<String, String> operations;
    private final Map<String, Boolean> recursive;

    public ${className}() {
        tokens = new HashSet<>();
        resources = HashMultimap.create();
        operations = HashMultimap.create();
        recursive = new HashMap<>();

        <#list tokenInfos as tokenInfo>
        tokens.add("${tokenInfo.token}");
        <#if (tokenInfo.resources?size > 0)>
        resources.putAll("${tokenInfo.token}", asList(<#list tokenInfo.resources as resource>"${resource}"<#if resource_has_next>, </#if></#list>));
        </#if>
        <#if (tokenInfo.operations?size > 0)>
        operations.putAll("${tokenInfo.token}", asList(<#list tokenInfo.operations as operation>"${operation}"<#if operation_has_next>, </#if></#list>));
        </#if>
        recursive.put("${tokenInfo.token}", ${tokenInfo.recursive?c});
        </#list>
    }

    @Override
    public Set<String> getTokens() {
        return tokens;
    }

    @Override
    public Set<String> getResources(String token) {
        if (resources.containsKey(token)) {
            return resources.get(token);
        } else {
            return Collections.<String>emptySet();
        }
    }

    @Override
    public Set<String> getOperations(String token) {
        if (operations.containsKey(token)) {
            return operations.get(token);
        } else {
            return Collections.<String>emptySet();
        }
    }

    @Override
    public boolean isRecursive(String token) {
        if (recursive.containsKey(token)) {
            return recursive.get(token);
        } else {
            return false;
        }
    }
}
