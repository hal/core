package org.jboss.as.console.client.core;

import com.google.gwt.user.client.ui.IsWidget;
import com.gwtplatform.mvp.client.View;

/**
 * @author Heiko Braun
 * @since 12/02/15
 */
public interface  MultiView extends View {
    void toggle(String mode);
    void createWidget();
    void register(String mode, IsWidget widget);
}
