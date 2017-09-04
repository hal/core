package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.History;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.TokenFormatter;
import elemental.client.Browser;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.NameTokens;

import java.util.HashSet;
import java.util.Set;

import static org.jboss.as.console.client.ProductConfig.Profile.PRODUCT;

/**
 * Either loads the default place or one specified from external context (URL tokens)
 *
 * @author Heiko Braun
 */
public class LoadMainApp implements ScheduledCommand {

    // blacklisted token can not be linked externally...
    private static Set<String> BLACK_LIST = new HashSet<>();

    static {
        BLACK_LIST.add(NameTokens.SettingsPresenter);
        BLACK_LIST.add(NameTokens.ToolsPresenter);
        BLACK_LIST.add(NameTokens.AccessControlFinder);
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

    private final PlaceManager placeManager;
    private final TokenFormatter formatter;
    private final ProductConfig productConfig;
    private final BootstrapContext bootstrapContext;

    public LoadMainApp(ProductConfig productConfig,
            BootstrapContext bootstrapContext,
            PlaceManager placeManager,
            TokenFormatter formatter) {
        this.productConfig = productConfig;
        this.bootstrapContext = bootstrapContext;
        this.placeManager = placeManager;
        this.formatter = formatter;
    }

    @Override
    public void execute() {
        String initialToken = History.getToken();
       /* if (!initialToken.isEmpty() && !isBlackListed(initialToken)) {
            List<PlaceRequest> hierarchy = formatter.toPlaceRequestHierarchy(initialToken);
            final PlaceRequest placeRequest = hierarchy.get(hierarchy.size() - 1);

            Scheduler.get().scheduleDeferred(() -> placeManager.revealPlace(placeRequest, true));
            bootstrapContext.setInitialPlace(placeRequest.getNameToken());
        } else {
            placeManager.revealDefaultPlace();
        }*/

        StringBuilder title = new StringBuilder();
        title.append(productConfig.getProfile() == PRODUCT ? "JBoss EAP" : "WildFly").append(" Management");
        title.append(" | ").append(bootstrapContext.getServerName());

        Browser.getDocument().setTitle(title.toString());

        // TODO (hbraun): disabled until we now how this should work on a finder access (relative url's)
        placeManager.revealDefaultPlace();
    }
}