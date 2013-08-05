package org.jboss.as.console.client.rbac;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public class ResourceAccessLog {

    private Map<String, Set<String>> token2address = new HashMap<String, Set<String>>();
    private Set<Listener> listeners = new HashSet<Listener>();

    public static ResourceAccessLog INSTANCE = new ResourceAccessLog();

    public void log(String token, String address)
    {
        if(null == token2address.get(token))
            token2address.put(token, new HashSet<String>());


        if(!token2address.get(token).contains(address))
            token2address.get(token).add(address);

        fireChange();
    }

    public Iterator<String> getKeys(){
        return token2address.keySet().iterator();
    }

    private void fireChange() {
        for(Listener l : listeners)
            l.onChange();
    }

    public Set<String> getAddresses(String token) {
        if(null == token2address.get(token))
            token2address.put(token, new HashSet<String>());

        return token2address.get(token);
    }

    public void flush() {
        token2address.clear();
        fireChange();
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public interface Listener {
        void onChange();
    }
}
