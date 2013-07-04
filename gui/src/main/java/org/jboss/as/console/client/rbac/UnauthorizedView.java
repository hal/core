package org.jboss.as.console.client.rbac;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.SimpleLayout;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public class UnauthorizedView extends SuspendableViewImpl implements AuthorisationPresenter.MyView {

    private AuthorisationPresenter presenter;

    @Override
    public void setPresenter(AuthorisationPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {

        SimpleLayout layout = new SimpleLayout()
                .setTitle("Unauthorized")
                .setHeadline("Authorisation Required")
                .setDescription("You don't have the permissions to access this resource.");

        return layout.build();
    }
}
