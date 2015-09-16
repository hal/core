package org.jboss.as.console.mbui.behaviour;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;

import java.util.Map;

/**
 * Minimum contract between a view and it's presenter that manages DMR model entities.
 *
 * @author Heiko Braun
 * @since 08/09/14
 */
@Deprecated
public interface DefaultPresenterContract {

    void onLaunchAddResourceDialog(AddressTemplate addressString);

    void onRemoveResource(String addressString, String name);

    void onSaveResource(String addressString, String name, Map<String, Object> changeset);
}
