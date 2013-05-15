package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.View;

/**
 * @author Heiko Braun
 * @date 5/15/13
 */
public interface SimpleView extends View {
    void show(Widget widget);
}
