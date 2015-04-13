package org.jboss.as.console.client.shared.subsys.undertow;

import com.gwtplatform.mvp.client.proxy.PlaceManager;

import java.util.Map;

/**
 * @author Heiko Braun
 * @since 07/04/15
 */
public interface CommonHttpPresenter {

    String getNameToken();

    PlaceManager getPlaceManager();

    void onSaveResource(String addressString, String name, Map<String, Object> changeset);
}
