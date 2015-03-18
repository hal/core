package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.inject.Inject;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.ProfileStore;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.CurrentProfileSelection;
import org.jboss.gwt.flow.client.Control;

import java.util.List;

/**
 * If links come in from external contexts, the initialization might
 * get out of order. However the current profile must be selected, otherwise Baseadress.get()
 * will yield wrong results.
 *
 * @author Heiko Braun
 * @date 1/13/12
 */
public class EagerLoadProfiles implements BootstrapStep {

    private final ProfileStore profileStore;
    private final CurrentProfileSelection profileSelection;

    @Inject
    public EagerLoadProfiles(ProfileStore profileStore, CurrentProfileSelection profileSelection) {
        this.profileStore = profileStore;
        this.profileSelection = profileSelection;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {

        final BootstrapContext context = control.getContext();
        if (!context.isStandalone()) {
            profileStore.loadProfiles(new SimpleCallback<List<ProfileRecord>>() {
                @Override
                public void onFailure(Throwable caught) {
                    context.setlastError(caught);
                    control.abort();
                }

                @Override
                public void onSuccess(List<ProfileRecord> result) {
                    context.setInitialProfiles(result);
                    // default profile
                    if (!result.isEmpty()) {
                        selectDefaultProfile(result);
                    }
                    control.proceed();
                }
            });

        } else {
            // standalone
            control.proceed();
        }
    }

    private void selectDefaultProfile(List<ProfileRecord> result) {
        if (!profileSelection.isSet()) {
            String match = null;
            String pref = "full";
            for (ProfileRecord record : result) {
                if (record.getName().equals(pref)) {
                    match = record.getName();
                    break;
                }
            }
            if (match == null) {
                match = result.get(0).getName();
            }
            profileSelection.setName(match);
        }
    }
}
