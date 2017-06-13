package org.jboss.as.console.client.shared.runtime.elytron;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.shared.runtime.elytron.ElytronRuntimePresenter.CREDENTIAL_STORE_TEMPLATE;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ElytronRuntimeView extends SuspendableViewImpl implements ElytronRuntimePresenter.MyView {

    private ElytronRuntimePresenter presenter;
    private ElytronGenericStoreRuntimeResourceView credentialStoreView;
    private ResourceDescriptionRegistry resourceDescriptionRegistry;
    private SecurityFramework securityFramework;

    @Inject
    public ElytronRuntimeView(final ResourceDescriptionRegistry resourceDescriptionRegistry,
            final SecurityFramework securityFramework) {

        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
    }

    public Widget createWidget() {

        ResourceDescription credentialStoreDescription = resourceDescriptionRegistry.lookup(CREDENTIAL_STORE_TEMPLATE);
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        credentialStoreView = new ElytronGenericStoreRuntimeResourceView(credentialStoreDescription, securityContext,
                "Credential Store", CREDENTIAL_STORE_TEMPLATE);
        credentialStoreView.setPresenter(presenter);

        PagedView panel = new PagedView(true);
        panel.addPage("Credential Store", credentialStoreView.asWidget());
        panel.showPage(0);

        return panel.asWidget();
    }

    @Override
    public void setPresenter(ElytronRuntimePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateCredentialReferences(final List<Property> models) {
        credentialStoreView.update(models);
    }

    @Override
    public void updateCredentialReferenceAliases(final List<ModelNode> models) {
        credentialStoreView.updateCredentialReferenceAliases(models);
    }

}
