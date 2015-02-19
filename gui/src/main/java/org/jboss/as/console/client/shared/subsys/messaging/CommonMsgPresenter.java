package org.jboss.as.console.client.shared.subsys.messaging;

import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public interface CommonMsgPresenter extends AddResourceDialog.Callback {
    PlaceManager getPlaceManager();

    void launchAddProviderDialog();

    void removeProvider(String serverName);

    void onSaveProvider(String name, Map<String, Object> changedValues);

    void closeDialogue();
}
