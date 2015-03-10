package org.jboss.as.console.client.shared.model;

import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;
import org.useware.kernel.model.structure.Select;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 18/09/14
 */
@Store
public class PerspectiveStore extends ChangeSupport {

    private Map<String, String> perspectiveMap = new HashMap<>();

    @Process(actionType = SelectPerspective.class)
    public void onLoadProfile(final SelectPerspective action, final Dispatcher.Channel channel) {
        perspectiveMap.put(action.getParent(), action.getChild());
        channel.ack();
    }

    // -----------------------------------------
    // data access

    public String getChild(String parent) {
        return perspectiveMap.get(parent);
    }

    public boolean hasChild(String parent) {
        return perspectiveMap.get(parent)!=null;
    }
}