package org.jboss.as.console.client.shared.subsys.infinispan.v3;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.Map;

/**
 * @author Heiko Braun
 * @since 28/04/15
 */
public class ContainerView {

    private final CacheFinderPresenter presenter;
    private final Property cacheContainer;

    private static final AddressTemplate address =
            AddressTemplate.of("{selected.profile}/subsystem=infinispan/cache-container=*");
    private ModelNodeForm form;

    public ContainerView(CacheFinderPresenter presenter, Property cacheContainer) {
        this.presenter = presenter;
        this.cacheContainer = cacheContainer;
    }

    public Widget asWidget() {

        // forms
        final SecurityContext securityContext = presenter.getSecurityFramework()
                .getSecurityContext(presenter.getProxy().getNameToken());

        ResourceDescription localCacheDescription = presenter.getDescriptionRegistry().lookup(address);

        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setAddress(address.getTemplate())
                .setConfigOnly()
                .setSecurityContext(securityContext)
                .setResourceDescription(presenter.getDescriptionRegistry().lookup(address))
                .build();

        form = formAssets.getForm();
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                if (form.getEditedEntity() == null || !form.getEditedEntity().isDefined()) {
                    presenter.onCreateTransport(address, cacheContainer, form.getUpdatedEntity());
                }
                else
                {
                    presenter.onSaveTransport(address, cacheContainer, form.getChangedValues());
                }
            }

            @Override
            public void onCancel(Object entity) {

            }
        });


        SimpleLayout layout = new SimpleLayout()
                .setPlain(true)
                .setHeadline("Settings for container: " + cacheContainer.getName())
                .setDescription(localCacheDescription.get("description").asString())
                .addContent(((UIConstants) GWT.create(UIConstants.class)).help(), formAssets.getHelp().asWidget())
                .addContent(Console.CONSTANTS.common_label_attributes(), form.asWidget());

        return layout.build();

    }

    public void updateFrom(ModelNode transportSettings) {
        form.edit(transportSettings);
    }
}
