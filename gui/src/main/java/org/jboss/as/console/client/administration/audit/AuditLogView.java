package org.jboss.as.console.client.administration.audit;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.rbac.RolesHelpPanel;

/**
 * @author Harald Pehl
 * @date 08/12/2013
 */
public class AuditLogView extends SuspendableViewImpl implements AuditLogPresenter.MyView {

    private AuditLogPresenter presenter;

    @Override
    public void setPresenter(final AuditLogPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        SimpleLayout layout = new SimpleLayout()
                .setTitle(Console.CONSTANTS.administration_audit())
                .setHeadline(Console.CONSTANTS.administration_audit_log())
                .setDescription(Console.CONSTANTS.administration_audit_log_desc())
                .addContent("nyi", new Label("Not yet implemented"));
        return layout.build();
    }
}
