package org.jboss.as.console.client.administration.role;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 */
public class ScopedRoleEditor implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private final BeanFactory beanFactory;
    private final DispatchAsync dispatcher;

    public ScopedRoleEditor(final RoleAssignmentPresenter presenter, final BeanFactory beanFactory,
            final DispatchAsync dispatcher) {
        this.presenter = presenter;
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
    }

    @Override
    public Widget asWidget() {
        return new Label("ScopedRoleEditor not yet implemented");
    }

    public void reset() {
    }
}
