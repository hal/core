package org.jboss.as.console.client.v3.stores.domain;

import org.jboss.as.console.client.domain.model.ProfileDAO;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshProfiles;
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
public class ProfileStore extends ChangeSupport {

    private ProfileDAO dao;
    private List<ProfileRecord> profiles = Collections.EMPTY_LIST;

    @Inject
    public ProfileStore(ProfileDAO dao) {
        this.dao = dao;
    }

    @Process(actionType = RefreshProfiles.class)
    public void onRefreshProfiles(final RefreshProfiles action, final Dispatcher.Channel channel) {

        refresh(channel);
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
}
