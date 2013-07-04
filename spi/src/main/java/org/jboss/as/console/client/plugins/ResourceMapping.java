package org.jboss.as.console.client.plugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public class ResourceMapping {
    private Map<String, Set<String>> token2address = new HashMap<String, Set<String>>();

    public void put(String token, String address){
        if(null==token2address.get(token))
            token2address.put(token, new HashSet<String>());

        token2address.get(token).add(address);
    }

    public Set<String> getResources(String token)
    {
        if(null==token2address.get(token))
            token2address.put(token, new HashSet<String>());

        return token2address.get(token);
    }
}
