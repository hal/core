package org.jboss.as.console.client.core.bootstrap;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.TokenFormatter;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * Either loads the default place or one specified from external context (URL tokens)
 *
 * @author Heiko Braun
 * @date 12/7/11
 */
public class LoadMainApp implements Command
{

    private PlaceManager placeManager;
    private TokenFormatter formatter;
    private BootstrapContext bootstrapContext;

    // blackisted token can not be linked externally...
    private static Set<String> BLACK_LIST = new HashSet<String>();

    static {
        BLACK_LIST.add(NameTokens.SettingsPresenter);
        BLACK_LIST.add(NameTokens.ToolsPresenter);
    }

    public LoadMainApp(BootstrapContext bootstrapContext, PlaceManager placeManager, TokenFormatter formatter) {
        this.bootstrapContext = bootstrapContext;
        this.placeManager = placeManager;
        this.formatter = formatter;
    }

    @Override
    public void execute() {

       /*

       Currently disabled due to RBAC constraints (init, etc)

       String initialToken = History.getToken();

        if(!initialToken.isEmpty() && !isBlackListed(initialToken))
        {
            List<PlaceRequest> hierarchy = formatter.toPlaceRequestHierarchy(initialToken);
            final PlaceRequest placeRequest = hierarchy.get(hierarchy.size() - 1);

            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    placeManager.revealPlace(placeRequest, true);
                }
            });

            bootstrapContext.setInitialPlace(placeRequest.getNameToken());

        }
        else {
            placeManager.revealDefaultPlace();
        }
        */

        // RBAC edgecase
        if(!bootstrapContext.isStandalone()
            && (bootstrapContext.isGroupManagementDisabled() || bootstrapContext.isHostManagementDisabled())
                )
        {
            Feedback.confirm(
                    "Access Denied",
                    "You seem to lack host or server group permissions required to access this interface.",
                    new Feedback.ConfirmationHandler() {
                        @Override
                        public void onConfirmation(boolean isConfirmed) {
                            // nada
                        }
                    });
        }
        else
        {
            placeManager.revealDefaultPlace();
        }
    }

    /*private static boolean isBlackListed (String token)
    {
        boolean match = false;
        for(String listed : BLACK_LIST)
        {
            if(token.startsWith(listed))
            {
                match =true;
                break;
            }
        }
        return match;
    } */
}