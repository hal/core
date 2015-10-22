package org.jboss.as.console.client.shared.subsys.generic;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.tools.ModelBrowser;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * @author Heiko Braun
 * @since 22/10/15
 */
public class GenericSubsystemView extends SuspendableViewImpl implements GenericSubsystemPresenter.MyView {

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private ModelBrowser modelBrowser;

    @Inject
    public GenericSubsystemView(DispatchAsync dispatcher, StatementContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
    }

    @Override
    public void showDetails(final ResourceAddress resourceAddress) {
        modelBrowser.onReset(resourceAddress);
    }

    @Override
    public Widget createWidget() {
        modelBrowser = new ModelBrowser(dispatcher, statementContext, Footer.PROGRESS_ELEMENT);
        return modelBrowser.asWidget();
    }
}
