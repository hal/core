package org.jboss.as.console.client.core.bootstrap;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TokenFormatter;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.dispatch.AsyncCommand;

/**
 *
 * Either loads the default place or one specified from external context (URL tokens)
 *
 * @author Heiko Braun
 * @date 12/7/11
 */
public class LoadMainApp implements AsyncCommand<Boolean> {

    @Override
    public void execute(AsyncCallback<Boolean> callback) {

        String initialToken = History.getToken();

        System.out.println("init from: " +initialToken);

        PlaceManager placeManager = Console.MODULES.getPlaceManager();
        TokenFormatter fromatter = Console.MODULES.getTokenFormatter();

        if(!initialToken.isEmpty())
            placeManager.revealPlaceHierarchy(fromatter.toPlaceRequestHierarchy(initialToken));
        else
            placeManager.revealDefaultPlace();

        callback.onSuccess(Boolean.TRUE);
    }
}