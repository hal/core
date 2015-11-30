package org.jboss.as.console.client.shared.subsys.messaging;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.Property;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 1/17/12
 */
public class ProviderView {

    private HornetqFinder presenter;
    private MessagingProviderEditor providerEditor;

    private final static AddressTemplate ADDRESS = AddressTemplate.of(
            "{selected.profile}/subsystem=messaging/hornetq-server=*"
    );

    private final static String[] COMMON = new String[] {
            "allow-failback",
            "backup",
            "backup-group-name",
            "check-for-live-server",
            "management-address",
            "management-notification-address",
            "statistics-enabled",
            "thread-pool-max-size",
            "scheduled-thread-pool-max-size",
            "transaction-timeout",
            "transaction-timeout-scan-period",
            "wild-card-routing-enabled",
            "persistence-enabled",
            "persist-id-cache",
            "failover-on-shutdown",

    };

    private final static String[] SECURITY = new String[] {
            "security-domain",
            "security-enabled",
            "security-invalidation-interval",
            "cluster-user",
            "cluster-password"
    };

    private final static String[] JOURNAL = new String[] {
            "journal-buffer-size",
            "journal-buffer-timeout",
            "journal-compact-min-files",
            "journal-compact-percentage",
            "journal-file-size",
            "journal-max-io",
            "journal-min-files",
            "journal-sync-non-transactional",
            "journal-sync-transactional",
            "journal-type",
            "create-journal-dir"
    };
    private ModelNodeFormBuilder.FormAssets commonForm;
    private ModelNodeFormBuilder.FormAssets secForm;
    private ModelNodeFormBuilder.FormAssets journalForm;
    private Property provider;
    private HTML title;

    public ProviderView(HornetqFinder presenter) {

        this.presenter = presenter;
    }

    private Property getProvider() {
        return provider;
    }

    public Widget asWidget() {

        final SecurityContext securityContext =
                presenter.getSecurityFramework().getSecurityContext(
                        presenter.getProxy().getNameToken()
                );

        final ResourceDescription definition = presenter.getDescriptionRegistry().lookup(ADDRESS);

        final FormCallback callback = new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveProvider(getProvider(), changeset);
            }

            @Override
            public void onCancel(Object entity) {

            }
        };

        // common
        commonForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .include(COMMON)
                .setSecurityContext(securityContext).build();

        commonForm.getForm().setToolsCallback(callback);

        // security
        secForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(SECURITY)
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();

        secForm.getForm().setToolsCallback(callback);

        // journal
        journalForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(JOURNAL)
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();

        journalForm.getForm().setToolsCallback(callback);

        title = new HTML();
        title.setStyleName("content-header-label");

        // ----
        OneToOneLayout layoutBuilder = new OneToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(title)
                .setDescription(definition.get("description").asString())
                .addDetail(Console.CONSTANTS.common_label_attributes(), commonForm.asWidget())
                .addDetail("Security", secForm.asWidget())
                .addDetail("Journal", journalForm.asWidget());

        return layoutBuilder.build();
    }

    public void updateFrom(Property provider) {

        this.provider = provider;

        this.title.setHTML("JMS Messaging Provider: "+provider.getName() );

        commonForm.getForm().edit(provider.getValue());
        secForm.getForm().edit(provider.getValue());
        journalForm.getForm().edit(provider.getValue());
    }
}
