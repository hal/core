package org.jboss.as.console.client.v3.stores.domain;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.ProfileDAO;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.v3.stores.domain.actions.CloneProfile;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshProfiles;
import org.jboss.as.console.client.v3.stores.domain.actions.RemoveProfile;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 21/04/15
 */
@Store
public class ProfileStore extends ChangeSupport {

    private ProfileDAO dao;
    private final DispatchAsync dispatcher;
    private List<ProfileRecord> profiles = Collections.EMPTY_LIST;

    @Inject
    public ProfileStore(ProfileDAO dao, DispatchAsync dispatcher) {
        this.dao = dao;
        this.dispatcher = dispatcher;
    }

    @Process(actionType = RefreshProfiles.class)
    public void onRefreshProfiles(final RefreshProfiles action, final Dispatcher.Channel channel) {


        refresh(channel);
    }

    @Process(actionType = CloneProfile.class)
    public void onCloneProfiles(final CloneProfile action, final Dispatcher.Channel channel) {

        ModelNode op = new ModelNode();
        op.get(ADDRESS).add("profile", action.getFrom());
        op.get(OP).set("clone");
        op.get("to-profile").set(action.getTo());

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if(response.isFailure())
                {
                    Console.error("Failed to create new profile: "+response.getFailureDescription());
                }
                else
                {
                    Console.info("Successfully created new profile: " + action.getTo());
                    refresh(channel);
                }
            }
        });

    }

    @Process(actionType = RemoveProfile.class)
    public void onCloneProfiles(final RemoveProfile action, final Dispatcher.Channel channel) {

        ModelNode op = new ModelNode();
        op.get(ADDRESS).add("profile", action.getName());
        op.get(OP).set(REMOVE);


        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if(response.isFailure())
                {
                    Console.error("Failed to remove profile: "+response.getFailureDescription());
                }
                else
                {
                    Console.info("Successfully removed profile: "+action.getName());
                    refresh(channel);
                }
            }
        });

    }

    private void refresh(final Dispatcher.Channel channel) {
        dao.loadProfiles(new SimpleCallback<List<ProfileRecord>>() {
            @Override
            public void onSuccess(List<ProfileRecord> result) {

                ProfileStore.this.profiles = result;

                channel.ack();
            }
        });
    }

    /* private ProfileRecord lookup(String name) {
         ProfileRecord match = null;
         for (ProfileRecord profile : profiles) {
             if (profile.getName().equals(name)) {
                 match = profile;
                 break;
             }
         }

         return match;
     }
 */
    public List<ProfileRecord> getProfiles() {
        return profiles;
    }

    public ProfileRecord getProfile(String name) {
        ProfileRecord match = null;
        for (ProfileRecord profile : profiles) {
            if(profile.getName().equals(name))
            {
                match = profile;
                break;
            }
        }
        return match;
    }
}
