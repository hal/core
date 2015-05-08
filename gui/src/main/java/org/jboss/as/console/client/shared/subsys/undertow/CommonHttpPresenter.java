package org.jboss.as.console.client.shared.subsys.undertow;

import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;

import java.util.Map;

/**
 * @author Heiko Braun
 * @since 07/04/15
 */
public interface CommonHttpPresenter {

    String getNameToken();

    PlaceManager getPlaceManager();

    ResourceDescriptionRegistry getDescriptionRegistry();

    void onSaveResource(AddressTemplate resourceAddress, String name, Map<String, Object> changeset);
}
