package org.jboss.as.console.mbui.behaviour;

import java.util.Map;

/**
 * Minimum contract between a view and it's presenter that manages DMR model entities.
 *
 * @author Heiko Braun
 * @since 08/09/14
 */
public interface DefaultPresenterContract {

    void onLaunchAddResourceDialog(String addressString);

    void onRemoveResource(String addressString, String name);

    void onSaveResource(String addressString, String name, Map<String, Object> changeset);
}
