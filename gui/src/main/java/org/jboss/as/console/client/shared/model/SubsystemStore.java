package org.jboss.as.console.client.shared.model;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.stores.domain.ProfileStore;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 18/09/14
 */

@Store
public class SubsystemStore extends ChangeSupport {

    private final SubsystemLoader subsystemLoader;
    private final ProfileStore profileStore;
    private final BootstrapContext bootstrap;

    private Map<String, List<SubsystemRecord>> profileMap = new HashMap<>();

    @Inject
    public SubsystemStore(SubsystemLoader subsystemLoader, ProfileStore profileStore, BootstrapContext bootstrap) {
        this.subsystemLoader = subsystemLoader;
        this.profileStore = profileStore;
        this.bootstrap = bootstrap;
    }

    @Process(actionType = LoadProfile.class)
    public void onLoadProfile(final LoadProfile action, final Dispatcher.Channel channel) {


        if(profileMap.containsKey(action.getProfile()))
        {
            // return cached data
            channel.ack();
        }
        else {

            if(bootstrap.isStandalone())
            {
                subsystemLoader.loadSubsystems("default", new AsyncCallback<List<SubsystemRecord>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        channel.nack(caught);
                    }

                    @Override
                    public void onSuccess(List<SubsystemRecord> result) {
                        profileMap.put("default", result);
                        channel.ack();
                    }
                });
            }
            else
            {
                ProfileRecord profile = profileStore.getProfile(action.getProfile());

                List<Function<FunctionContext>> fns = new LinkedList<>();

                // included profiles
                for (String includedProfile : getAllParents(profile)) {
                    fns.add(new Function<FunctionContext>() {
                        @Override
                        public void execute(Control<FunctionContext> control) {
                            subsystemLoader.loadSubsystems(includedProfile, new AsyncCallback<List<SubsystemRecord>>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    control.abort();
                                }

                                @Override
                                public void onSuccess(List<SubsystemRecord> result) {
                                    profileMap.put(includedProfile, result);
                                    control.proceed();
                                }
                            });
                        }
                    });
                }

                // the profile itself
                fns.add(new Function<FunctionContext>() {
                    @Override
                    public void execute(Control<FunctionContext> control) {
                        subsystemLoader.loadSubsystems(action.getProfile(), new AsyncCallback<List<SubsystemRecord>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                control.abort();
                            }

                            @Override
                            public void onSuccess(List<SubsystemRecord> result) {
                                profileMap.put(action.getProfile(), result);
                                control.proceed();
                            }
                        });
                    }
                });


                new Async().waterfall(new FunctionContext(), new Outcome<FunctionContext>() {
                    @Override
                    public void onFailure(FunctionContext context) {
                        channel.nack(context.getError());
                    }

                    @Override
                    public void onSuccess(FunctionContext context) {
                        channel.ack(true);
                    }
                }, fns.toArray(new Function[]{}));
            }
        }
    }

    private List<String> getAllParents(ProfileRecord profile) {
        List<String> accum = new ArrayList<>();
        _getAllParents(accum, profile);
        return accum;
    }

    private void _getAllParents(List<String> accum, ProfileRecord profile) {

        List<String> parents = profile.getIncludes();
        for (String parent : parents) {
            _getAllParents(accum, profileStore.getProfile(parent));
        }

        accum.addAll(parents);
    }

    // -----------------------------------------
    // data access

    public List<SubsystemRecord> getSubsystems(String profile) {

        List<SubsystemRecord> subsystemRecords = profileMap.get(profile);

        if(null==subsystemRecords)
            throw new IllegalArgumentException("No subsystems for profile "+profile);

        return subsystemRecords;
    }

    public List<SubsystemReference> getActualSubsystems(String profile) {
        List<SubsystemReference> actualProfile = new LinkedList<SubsystemReference>();


        // included from parent
        getIncludedSubsystems(actualProfile, profile);


        // within current profile
        List<SubsystemRecord> subsystems = getSubsystems(profile);

        for (SubsystemRecord current : subsystems) {
            actualProfile.add(new SubsystemReference(current, false));
        }


        return actualProfile;
    }

    private void getIncludedSubsystems(List<SubsystemReference> accum, String profile) {

        // included from parent
        List<String> includes = profileStore.getProfile(profile).getIncludes();
        if(includes.size()>0)
        {
            for (String parentName : includes) {

                getIncludedSubsystems(accum, parentName);

                List<SubsystemRecord> parentSubsystems = getSubsystems(parentName);
                for (SubsystemRecord parentSubsys: parentSubsystems) {
                    accum.add(new SubsystemReference(parentSubsys, true, parentName));
                }
            }
        }
    }
}