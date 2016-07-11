package org.jboss.as.console.client.shared.subsys.tx;

import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.DATASOURCE;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;
import static org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter.PROCESS_ID_SOCKET_BINDING;
import static org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter.PROCESS_ID_SOCKET_MAX_PORTS;
import static org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter.PROCESS_ID_UUID;
import static org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter.ROOT_ADDRESS_TEMPLATE;

/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class TransactionView extends SuspendableViewImpl implements TransactionPresenter.MyView {

    private final static String[] PROCESS = new String[]{
            "process-id-uuid",
            "process-id-socket-binding", //- added as factory to use the SuggestionBox custom implementation.
            "process-id-socket-max-ports",
    };

    private final static String[] RECOVERY = new String[]{
            "socket-binding", //- added as factory to use the SuggestionBox custom implementation.
            "status-socket-binding",//- added as factory to use the SuggestionBox custom implementation.
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
            "jdbc-store-datasource" //- added as factory to use the SuggestionBox custom implementation.
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
        commonAssets.getForm().addFormValidator((formItems, outcome) -> {
            final FormItem<Boolean> journalStoreEnableAsyncIoItem = formItem(formItems, "journal-store-enable-async-io");
            final FormItem<Boolean> useJournalStoreItem = formItem(formItems, "use-journal-store");

            if (journalStoreEnableAsyncIoItem != null) {
                final boolean journalStoreEnableAsyncIo = journalStoreEnableAsyncIoItem.getValue() != null && journalStoreEnableAsyncIoItem.getValue();
                final boolean useJournalStore = useJournalStoreItem != null && useJournalStoreItem.getValue() != null && useJournalStoreItem.getValue();

                if (journalStoreEnableAsyncIo && !useJournalStore) {
                    useJournalStoreItem.setErrMessage("Journal store needs to be enabled before enabling asynchronous IO.");
                    useJournalStoreItem.setErroneous(true);
                    outcome.addError("use-journal-store");
                }
            }
        });

        processAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(PROCESS)
                .addFactory("process-id-socket-binding", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("process-id-socket-binding", 
                            "Process id socket binding", false,
                        Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
                    return suggestionResource.buildFormItem();
                })
                .setResourceDescription(description)
                .setSecurityContext(securityContext)
                .build();
        processAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                if (!changeSet.isEmpty()) {
                    Boolean uuid;
                    String socketBinding;
                    Integer maxPorts;

                    if (changeSet.containsKey(PROCESS_ID_UUID)) {
                        uuid = (Boolean) undefinedToNull(changeSet.get(PROCESS_ID_UUID));
                    } else {
                        // if not in changeSet, get current value from edited entity
                        uuid = getCurrentValue(PROCESS_ID_UUID).asBoolean();
                    }

                    if (changeSet.containsKey(PROCESS_ID_SOCKET_BINDING)) {
                        socketBinding = (String) undefinedToNull(changeSet.get(PROCESS_ID_SOCKET_BINDING));
                    } else {
                        socketBinding = getCurrentValue(PROCESS_ID_SOCKET_BINDING).isDefined() ?
                                getCurrentValue(PROCESS_ID_SOCKET_BINDING).asString() : null;
                    }

                    if (changeSet.containsKey(PROCESS_ID_SOCKET_MAX_PORTS)) {
                        maxPorts = (Integer) undefinedToNull(changeSet.get(PROCESS_ID_SOCKET_MAX_PORTS));
                    } else {
                        maxPorts = getCurrentValue(PROCESS_ID_SOCKET_MAX_PORTS).isDefined() ?
                                getCurrentValue(PROCESS_ID_SOCKET_MAX_PORTS).asInt() : null;
                    }

                    presenter.saveProcessSettings(uuid, socketBinding, maxPorts);
                }
            }

            private ModelNode getCurrentValue(String field) {
                return processAssets.getForm().getEditedEntity().get(field);
            }

            private Object undefinedToNull(Object object) {
                return FormItem.VALUE_SEMANTICS.UNDEFINED.equals(object) ? null : object;
            }

            @Override
            public void onCancel(Object entity) {
                processAssets.getForm().cancel();
            }
        });
        processAssets.getForm().addFormValidator((formItems, outcome) -> {
            FormItem<Boolean> uuidItem = formItem(formItems, PROCESS_ID_UUID);
            FormItem<String> socketBindingItem = formItem(formItems, PROCESS_ID_SOCKET_BINDING);
            FormItem<Number> socketMaxPortsItem = formItem(formItems, PROCESS_ID_SOCKET_MAX_PORTS);
            if (uuidItem != null && socketBindingItem != null) {
                boolean uuidGiven = uuidItem.getValue() != null && uuidItem.getValue();
                String socketBinding = Strings.emptyToNull(socketBindingItem.getValue());

                if ((uuidGiven && socketBinding != null) || (!uuidGiven && socketBinding == null)) {
                    socketBindingItem.setErrMessage("Please set either UUID or socket binding");
                    socketBindingItem.setErroneous(true);
                    outcome.addError(PROCESS_ID_SOCKET_BINDING);

                    // TODO remove poor-api-workaround
                    // restore default
                    socketBindingItem.setErrMessage("No whitespace, no special chars allowed");
                }
            }
            if (socketBindingItem != null && socketMaxPortsItem != null) {
                String socketBinding = Strings.emptyToNull(socketBindingItem.getValue());
                Number socketMaxPorts = socketMaxPortsItem.getValue();

                if (socketBinding == null && socketMaxPorts != null && socketMaxPortsItem.isModified()) {
                    socketMaxPortsItem.setErrMessage("Can't be set if socket binding is not set");
                    socketMaxPortsItem.setErroneous(true);
                    outcome.addError(PROCESS_ID_SOCKET_MAX_PORTS);

                    // TODO remove poor-api-workaround
                    // restore default
                    socketMaxPortsItem.setErrMessage("Invalid numeric value");
                }
            }
        });

        recoveryAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(RECOVERY)
                .addFactory("status-socket-binding", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("status-socket-binding", "Status socket binding", true,
                            Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("socket-binding", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("socket-binding", "Socket binding", true,
                            Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
                    return suggestionResource.buildFormItem();
                })
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
                .addFactory("jdbc-store-datasource", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("jdbc-store-datasource", 
                            "Jdbc store datasource", false,
                        Console.MODULES.getCapabilities().lookup(DATASOURCE));
                    return suggestionResource.buildFormItem();
                })
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
                .addDetail(Console.CONSTANTS.common_label_attributes(), commonAssets.asWidget())
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
