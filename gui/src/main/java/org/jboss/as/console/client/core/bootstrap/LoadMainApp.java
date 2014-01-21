package org.jboss.as.console.client.core.bootstrap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.TokenFormatter;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.NameTokens;

/**
 * Either loads the default place or one specified from external context (URL tokens)
 *
 * @author Heiko Braun
 */
public class LoadMainApp implements Command {

    // blackisted token can not be linked externally...
    private static Set<String> BLACK_LIST = new HashSet<String>();

    static {
        BLACK_LIST.add(NameTokens.SettingsPresenter);
        BLACK_LIST.add(NameTokens.ToolsPresenter);
        BLACK_LIST.add(NameTokens.AdministrationPresenter);
        BLACK_LIST.add(NameTokens.RoleAssignmentPresenter);
    }

    private static boolean isBlackListed(String token) {
        boolean match = false;
        for (String listed : BLACK_LIST) {
            if (token.startsWith(listed)) {
                match = true;
                break;
            }
        }
        return match;
    }

    private PlaceManager placeManager;
    private TokenFormatter formatter;
    private BootstrapContext bootstrapContext;

    public LoadMainApp(BootstrapContext bootstrapContext, PlaceManager placeManager, TokenFormatter formatter) {
        this.bootstrapContext = bootstrapContext;
        this.placeManager = placeManager;
        this.formatter = formatter;
    }

    @Override
    public void execute() {
        String initialToken = History.getToken();
        if (!initialToken.isEmpty() && !isBlackListed(initialToken)) {
            List<PlaceRequest> hierarchy = formatter.toPlaceRequestHierarchy(initialToken);
            final PlaceRequest placeRequest = hierarchy.get(hierarchy.size() - 1);

            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    placeManager.revealPlace(placeRequest, true);
                }
            });
            bootstrapContext.setInitialPlace(placeRequest.getNameToken());
        } else {
            placeManager.revealDefaultPlace();
        }
    }
}