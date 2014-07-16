package org.jboss.as.console.client.domain.runtime;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;

/**
 * @author Heiko Braun
 * @since 16/07/14
 */
public class NoServerView extends SuspendableViewImpl implements NoServerPresenter.MyView {

    @Override
    public Widget createWidget() {
        return new HTML("No Server Selected");
    }
}
