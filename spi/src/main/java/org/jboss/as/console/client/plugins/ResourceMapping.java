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
    private Map<String, MetaData> mappings = new HashMap<String, MetaData>();

    public void put(String token, String address, String facet, String recursive){
        if(null==token2address.get(token))
            token2address.put(token, new HashSet<String>());

        token2address.get(token).add(address);

        mappings.put(token, new MetaData(token, address, facet, Boolean.valueOf(recursive)));
    }

    public Set<String> getResources(String token)
    {
        if(null==token2address.get(token))
            token2address.put(token, new HashSet<String>());

        return token2address.get(token);
    }

    public String getFacet(String token) {
        return mappings.get(token)!=null ? mappings.get(token).facet : "configuration";
    }

    public boolean isRecursive(String token) {
        return mappings.get(token) == null || mappings.get(token).recursive;
    }

    class MetaData {
        String token;
        String address;
        String facet;
        boolean recursive;

        MetaData(String token, String address, String facet, boolean recursive) {
            this.token = token;
            this.address = address;
            this.facet = facet;
            this.recursive = recursive;
        }
    }
}
