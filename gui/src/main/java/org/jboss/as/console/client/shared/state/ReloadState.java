package org.jboss.as.console.client.shared.state;

import org.jboss.as.console.client.Console;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 12/14/11
 */
@Singleton
public class ReloadState {

    private Map<String, ServerState> serverStates = new HashMap<String, ServerState>();
    private int lastFiredSize = 0;


    public boolean isStaleModel() {
        return serverStates.size()>0;
    }

    public void reset() {
        serverStates.clear();
        lastFiredSize = 0;
    }

    public void propagateChanges() {
        if(isStaleModel() && lastFiredSize<serverStates.size())
        {
            lastFiredSize = serverStates.size();

            StringBuffer sb = new StringBuffer();
            ServerState serverState = serverStates.values().iterator().next();
            String message = serverState.isReloadRequired() ?
                    Console.CONSTANTS.server_instance_servers_needReload() :
                    Console.CONSTANTS.server_instance_servers_needRestart();

            sb.append(message).append("\n\n");

            for(ServerState server : serverStates.values())
            {
                sb.append("  - ");
                sb.append(server.getName());
                sb.append("\n");
            }

            // state update, log warning
            Console.warning(Console.CONSTANTS.server_configuration_changed(), sb.toString(), true);
        }
    }

    /*public void resetServer(String name) {
        serverStates.remove(name);
        lastFiredSize--;
    } */

    public void updateFrom(Map<String, ServerState> states) {
        this.serverStates = states;
    }

    public Map<String, ServerState> getServerStates() {
        return serverStates;
    }
}
