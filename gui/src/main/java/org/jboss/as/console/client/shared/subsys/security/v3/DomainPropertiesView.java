package org.jboss.as.console.client.shared.subsys.security.v3;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.layout.SimpleLayout;
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
 * @since 20/05/15
 */
public class DomainPropertiesView {
    private final SecDomainFinder presenter;
    private final Property domain;
    private ModelNodeForm form;

    public DomainPropertiesView(SecDomainFinder presenter, Property domain) {

        this.presenter = presenter;
        this.domain = domain;
    }

    public Widget asWidget() {
        // forms
        final SecurityContext securityContext = presenter.getSecurityFramework()
                .getSecurityContext(presenter.getProxy().getNameToken());

        ResourceDescription localCacheDescription = presenter.getDescriptionRegistry().lookup(SecDomainFinder.SECURITY_DOMAIN);

        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setAddress(SecDomainFinder.SECURITY_DOMAIN.getTemplate())
                .setConfigOnly()
                .setSecurityContext(securityContext)
                .setResourceDescription(presenter.getDescriptionRegistry().lookup(SecDomainFinder.SECURITY_DOMAIN))
                .build();

        form = formAssets.getForm();
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveDomain(SecDomainFinder.SECURITY_DOMAIN, domain.getName(), form.getChangedValues());
            }

            @Override
            public void onCancel(Object entity) {

            }
        });


        SimpleLayout layout = new SimpleLayout()
                .setPlain(true)
                .setHeadline(Console.MESSAGES.securityDomainDescription(SafeHtmlUtils.fromString(domain.getName()).asString()))
                .setDescription(localCacheDescription.get("description").asString())
                .addContent(Console.CONSTANTS.help(), formAssets.getHelp().asWidget())
                .addContent(Console.CONSTANTS.common_label_attributes(), form.asWidget());

        return layout.build();
    }

    public void updateFrom(ModelNode value) {
        form.edit(value);
    }
}
