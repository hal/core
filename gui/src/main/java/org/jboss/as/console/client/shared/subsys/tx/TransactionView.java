package org.jboss.as.console.client.shared.subsys.tx;

import com.google.common.base.Strings;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.dmr.client.ModelNode;

import java.util.List;
import java.util.Map;

import static org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter.*;

/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class TransactionView extends SuspendableViewImpl implements TransactionPresenter.MyView {

    private final static String[] PROCESS = new String[]{
            "process-id-uuid",
            "process-id-socket-binding",
            "process-id-socket-max-ports",
    };

    private final static String[] RECOVERY = new String[]{
            "socket-binding",
            "status-socket-binding",
            "recovery-listener",
    };

    private final static String[] PATH = new String[]{
            "object-store-path",
            "object-store-relative-to",
    };

    private final static String[] JDBC = new String[]{
            "use-jdbc-store",
            "jdbc-action-store-drop-table",
            "jdbc-action-store-table-prefix",
            "jdbc-communication-store-drop-table",
            "jdbc-communication-store-table-prefix",
            "jdbc-state-store-drop-table",
            "jdbc-state-store-table-prefix",
            "jdbc-store-datasource",
    };


    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private TransactionPresenter presenter;
    private ModelNodeFormBuilder.FormAssets commonAssets;
    private ModelNodeFormBuilder.FormAssets processAssets;
    private ModelNodeFormBuilder.FormAssets recoveryAssets;
    private ModelNodeFormBuilder.FormAssets pathAssets;
    private ModelNodeFormBuilder.FormAssets jdbcAssets;

    @Inject
    public TransactionView(SecurityFramework securityFramework, ResourceDescriptionRegistry descriptionRegistry) {
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;
    }

    @Override
    public Widget createWidget() {
        ResourceDescription description = descriptionRegistry.lookup(ROOT_ADDRESS_TEMPLATE);
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        commonAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .exclude("use-jdbc-store", "use-hornetq-store", "hornetq-store-enable-async-io")
                .exclude(PROCESS, RECOVERY, PATH, JDBC)
                .setResourceDescription(description)
                .setSecurityContext(securityContext)
                .build();
        commonAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                presenter.saveConfig(changeSet);
            }

            @Override
            public void onCancel(Object entity) {
                commonAssets.getForm().cancel();
            }
        });

        processAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(PROCESS)
                .setResourceDescription(description)
                .setSecurityContext(securityContext)
                .build();
        processAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                if (!changeSet.isEmpty()) {
                    Boolean uuid = null;
                    String socketBinding = null;
                    Integer maxPorts = null;
                    if (isDefined(PROCESS_ID_UUID, changeSet)) {
                        uuid = (Boolean) changeSet.get(PROCESS_ID_UUID);
                    }
                    if (isDefined(PROCESS_ID_SOCKET_BINDING, changeSet)) {
                        socketBinding = (String) changeSet.get(PROCESS_ID_SOCKET_BINDING);
                    }
                    if (isDefined(PROCESS_ID_SOCKET_MAX_PORTS, changeSet)) {
                        maxPorts = (Integer) changeSet.get(PROCESS_ID_SOCKET_MAX_PORTS);
                    }
                    presenter.saveProcessSettings(uuid, socketBinding, maxPorts);
                }
            }

            private boolean isDefined(String key, Map changeSet) {
                return changeSet.containsKey(key) && !FormItem.VALUE_SEMANTICS.UNDEFINED.equals(changeSet.get(key));
            }

            @Override
            public void onCancel(Object entity) {
                processAssets.getForm().cancel();
            }
        });
        processAssets.getForm().addFormValidator((formItems, outcome) -> {
            FormItem<Boolean> uuidItem = formItem(formItems, PROCESS_ID_UUID);
            FormItem<String> socketBindingItem = formItem(formItems, PROCESS_ID_SOCKET_BINDING);
            if (uuidItem != null && socketBindingItem != null) {
                boolean uuidGiven = uuidItem.getValue() != null && uuidItem.getValue();
                String socketBinding = Strings.emptyToNull(socketBindingItem.getValue());

                if ((uuidGiven && socketBinding != null) || (!uuidGiven && socketBinding == null)) {
                    socketBindingItem.setErrMessage("Please set either UUID or socket binding");
                    socketBindingItem.setErroneous(true);
                    outcome.addError(PROCESS_ID_SOCKET_BINDING);
                } else {
                    // TODO remove poor-api-workaround
                    // restore default
                    socketBindingItem.setErrMessage("No whitespace, no special chars allowed");
                }
            }
        });

        recoveryAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(RECOVERY)
                .setResourceDescription(description)
                .setSecurityContext(securityContext)
                .build();
        recoveryAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                presenter.saveConfig(changeSet);
            }

            @Override
            public void onCancel(Object entity) {
                recoveryAssets.getForm().cancel();
            }
        });

        pathAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(PATH)
                .setResourceDescription(description)
                .setSecurityContext(securityContext)
                .build();
        pathAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                presenter.saveConfig(changeSet);
            }

            @Override
            public void onCancel(Object entity) {
                pathAssets.getForm().cancel();
            }
        });

        jdbcAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(JDBC)
                .setResourceDescription(description)
                .setSecurityContext(securityContext)
                .build();
        jdbcAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                presenter.saveConfig(changeSet);
            }

            @Override
            public void onCancel(Object entity) {
                jdbcAssets.getForm().cancel();
            }
        });
        jdbcAssets.getForm().addFormValidator((formItems, outcome) -> {
            final FormItem<Boolean> useJdbc = formItem(formItems, "use-jdbc-store");
            final FormItem<String> datasource = formItem(formItems, "jdbc-store-datasource");

            if (useJdbc != null && useJdbc.getValue() == true) {
                if (datasource == null || datasource.getValue() == null || datasource.getValue().isEmpty()) {
                    datasource.setErrMessage("Please provide datasource JNDI name if using jdbc store.");
                    datasource.setErroneous(true);
                    outcome.addError("jdbc-store-datasource");
                }
            }
        });

        OneToOneLayout layout = new OneToOneLayout()
                .setPlain(true)
                .setTitle("Transactions")
                .setHeadline("Transaction Manager")
                .setDescription(Console.CONSTANTS.subys_tx_desc())
                .addDetail("Attributes", commonAssets.asWidget())
                .addDetail("Process ID", processAssets.asWidget())
                .addDetail("Recovery", recoveryAssets.asWidget())
                .addDetail("Path", pathAssets.asWidget())
                .addDetail("JDBC", jdbcAssets.asWidget());

        return layout.build();
    }

    @SuppressWarnings("unchecked")
    private <T> FormItem<T> formItem(List<FormItem> formItems, String name) {
        for (FormItem formItem : formItems) {
            if (name.equals(formItem.getName())) {
                return formItem;
            }
        }
        return null;
    }

    @Override
    public void setPresenter(TransactionPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateModel(ModelNode modelNode) {
        commonAssets.getForm().edit(modelNode);
        processAssets.getForm().edit(modelNode);
        recoveryAssets.getForm().edit(modelNode);
        pathAssets.getForm().edit(modelNode);
        jdbcAssets.getForm().edit(modelNode);
    }
}
