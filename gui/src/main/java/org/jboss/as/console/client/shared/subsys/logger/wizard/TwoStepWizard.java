package org.jboss.as.console.client.shared.subsys.logger.wizard;

import com.google.gwt.user.client.ui.HTMLPanel;
import org.jboss.as.console.client.shared.subsys.logger.LoggerPresenter;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.wizard.Wizard;
import org.jboss.ballroom.client.rbac.SecurityContext;

/**
 * Required attributes plus extra step for definition of (complex) file attribute
 *
 * @author Heiko Braun
 * @since 08/09/15
 */
public class TwoStepWizard extends Wizard<HandlerContext, HandlerSteps> {

    private final LoggerPresenter presenter;
    private final AddressTemplate address;
    private final SecurityContext securityContext;
    private final ResourceDescription resourceDescription;

    public TwoStepWizard(LoggerPresenter presenter, AddressTemplate address, SecurityContext securityContext, ResourceDescription resourceDescription) {

        super(HTMLPanel.createUniqueId(), new HandlerContext());
        this.presenter = presenter;
        this.address = address;
        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;

        addStep(HandlerSteps.ATTRIBUTES, new CommonAttributesStep(this, "Common Attributes"));
        addStep(HandlerSteps.FILE, new FileAttributeStep(this, "File Attribute"));
    }

    @Override
    protected HandlerSteps back(HandlerSteps state) {
        HandlerSteps next = null;
        switch (state)
        {
            case ATTRIBUTES:
                next = null;
                break;
            case FILE:
                next = HandlerSteps.ATTRIBUTES;
                break;
        }

        return next;
    }

    @Override
    protected HandlerSteps next(HandlerSteps state) {
        HandlerSteps next = null;
        switch (state)
        {
            case ATTRIBUTES:
                next = HandlerSteps.FILE;
                break;
        }

        return next;
    }

    public ResourceDescription getResourceDescription() {
        return resourceDescription;
    }

    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    @Override
    protected void finish() {

        super.finish();
        HandlerContext context = super.context;
        presenter.onCreateHandler(address, context);
    }
}
