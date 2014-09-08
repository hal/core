package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class UndertowView extends SuspendableViewImpl implements UndertowPresenter.MyView {
    private UndertowPresenter presenter;

    @Override
    public void setPresenter(UndertowPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        return new HTML("");
    }
}
