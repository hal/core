package org.jboss.as.console.client.shared;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableView;

/**
 * @author Heiko Braun
 * @date 9/2/13
 */
public interface DialogView extends SuspendableView {
    void show(Widget widget);
}
