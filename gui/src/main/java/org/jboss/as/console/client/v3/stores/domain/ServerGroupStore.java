package org.jboss.as.console.client.v3.stores.domain;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.ServerGroupDAO;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.v3.stores.domain.actions.CreateServerGroup;
import org.jboss.as.console.client.v3.stores.domain.actions.DeleteServerGroup;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServerGroups;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 21/04/15
 */
@Store
public class ServerGroupStore extends ChangeSupport {

    private ServerGroupDAO dao;
    private List<ServerGroupRecord> groups = Collections.EMPTY_LIST;

    @Inject
    public ServerGroupStore(ServerGroupDAO dao) {
        this.dao = dao;
    }

    @Process(actionType = RefreshServerGroups.class)
    public void onRefreshGroups(final RefreshServerGroups action, final Dispatcher.Channel channel) {

        refresh(channel);
    }

    private void refresh(final Dispatcher.Channel channel) {
        dao.loadServerGroups(new SimpleCallback<List<ServerGroupRecord>>() {
            @Override
            public void onSuccess(List<ServerGroupRecord> result) {

                ServerGroupStore.this.groups = result;

                channel.ack();
            }
        });
    }

    private ServerGroupRecord lookup(String name) {
        ServerGroupRecord match = null;
        for (ServerGroupRecord group : groups) {
            if(group.getName().equals(name)) {
                match = group;
                break;
            }
        }

        return match;
    }

    @Process(actionType = DeleteServerGroup.class)
    public void onDeleteGroup(final DeleteServerGroup action, final Dispatcher.Channel channel) {

        ServerGroupRecord group = lookup(action.getName());

        if(null==group) {
            Console.error("Unknown group " + action.getName());
        }
        else
        {

            dao.delete(group, new SimpleCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean wasSuccessful) {
                    if (wasSuccessful) {
                        Console.info(Console.MESSAGES.deleted("Server Group "+action.getName()));
                    } else {
                        Console.error(Console.MESSAGES.deletionFailed("Server Group "+action.getName()));
                    }

                    refresh(channel);
                }
            });
        }
    }

    @Process(actionType = CreateServerGroup.class)
    public void onCreateGroup(final CreateServerGroup action, final Dispatcher.Channel channel) {
        dao.create(action.getGroup(), new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {

                if (success) {

                    Console.info(Console.MESSAGES.added("Server Group "+action.getGroup().getName()));
                    refresh(channel);

                } else {
                    Console.error(Console.MESSAGES.addingFailed("Server Group "+ action.getGroup().getName()));
                }
            }
        });

    }

    public List<ServerGroupRecord> getServerGroups() {
        return groups;
    }
}
