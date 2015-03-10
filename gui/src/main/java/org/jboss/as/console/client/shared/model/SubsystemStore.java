package org.jboss.as.console.client.shared.model;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 18/09/14
 */

@Store
public class SubsystemStore extends ChangeSupport {

    private final SubsystemLoader subsystemLoader;

    private Map<String, List<SubsystemRecord>> profileMap = new HashMap<>();

    @Inject
    public SubsystemStore(SubsystemLoader subsystemLoader) {
        this.subsystemLoader = subsystemLoader;
    }

    @Process(actionType = LoadProfile.class)
    public void onLoadProfile(final LoadProfile action, final Dispatcher.Channel channel) {


        if(profileMap.containsKey(action.getProfile()))
        {
            // return cached data
            channel.ack();
        }
        else {

            // load data and cache
            subsystemLoader.loadSubsystems(action.getProfile(), new AsyncCallback<List<SubsystemRecord>>() {
                @Override
                public void onFailure(Throwable caught) {
                    channel.nack(caught);
                }

                @Override
                public void onSuccess(List<SubsystemRecord> result) {
                    profileMap.put(action.getProfile(), result);
                    channel.ack(true);
                }
            });
        }
    }

    // -----------------------------------------
    // data access

    public List<SubsystemRecord> getSubsystems(String profile) {

        List<SubsystemRecord> subsystemRecords = profileMap.get(profile);

        if(null==subsystemRecords)
            throw new IllegalArgumentException("No subsystems for profile "+profile);

        return subsystemRecords;
    }
}