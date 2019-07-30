package org.jboss.as.console.client.shared.subsys.infinispan.v3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

/**
 * @author Heiko Braun
 * @since 28/04/15
 */
public class CommonCacheAttributes {


    public AddressTemplate LOCKING;
    public AddressTemplate EVICTION;
    public AddressTemplate EXPIRATION;
    public AddressTemplate STORE;
    public AddressTemplate FILE_STORE;
    public AddressTemplate REMOTE_STORE;
    public AddressTemplate STRING_STORE;
    public AddressTemplate MIXED_STORE;
    public AddressTemplate BINARY_STORE;
    public AddressTemplate TRANSACTION;

    private Set<AddressTemplate> FORMS = new HashSet<>();

    private DefaultCellTable<Property> table;
    private final CachesPresenter presenter;
    private String title;
    private final AddressTemplate cacheType;

    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;

    private Map<AddressTemplate, FormContainer> formMapping = new HashMap<>();
    private StoreFormWrapper storeForm;

    public CommonCacheAttributes(CachesPresenter presenter, String title, AddressTemplate cacheType) {
        this.presenter = presenter;
        this.title = title;
        this.cacheType = cacheType;

        initResourceModel(cacheType);
    }

    private void initResourceModel(AddressTemplate cacheType) {
        LOCKING = cacheType.append("component=locking");
        EVICTION = cacheType.append("component=eviction");
        EXPIRATION = cacheType.append("component=expiration");
        STORE = cacheType.append("store=custom");
        FILE_STORE = cacheType.append("store=file");
        REMOTE_STORE = cacheType.append("store=remote");
        STRING_STORE = cacheType.append("store=string-jdbc");
        MIXED_STORE = cacheType.append("store=mixed-jdbc");
        BINARY_STORE = cacheType.append("store=binary-jdbc");
        TRANSACTION = cacheType.append("component=transaction");

        FORMS.add(LOCKING);
        FORMS.add(EVICTION);
        FORMS.add(EXPIRATION);
        FORMS.add(STORE);
        FORMS.add(FILE_STORE);
        FORMS.add(REMOTE_STORE);
        FORMS.add(STRING_STORE);
        FORMS.add(MIXED_STORE);
        FORMS.add(BINARY_STORE);
        FORMS.add(TRANSACTION);

        FORMS.add(cacheType);
    }

    public Widget asWidget() {

        ProvidesKey<Property> providesKey = new ProvidesKey<Property>() {
            @Override
            public Object getKey(Property item) {
                return item.getName();
            }
        };

        selectionModel = new SingleSelectionModel<>(providesKey);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property selection = selectionModel.getSelectedObject();
                if(selection!=null)
                {
                    updateForms(selection);
                }
            }
        });

        table = new DefaultCellTable<Property>(10, providesKey);
        table.setSelectionModel(selectionModel);

        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property modelNode) {
                return modelNode.getName();
            }
        };
        table.addColumn(nameColumn, "Name");

        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);

        ToolStrip tools = new ToolStrip();
        ToolButton addButton = new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                presenter.onLaunchAddWizard(cacheType);
            }
        });
        ToolButton removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                Property selectedObject = selectionModel.getSelectedObject();
                if(selectedObject!=null) {
                    Feedback.confirm(Console.MESSAGES.deleteTitle("Cache Configuration"),
                            Console.MESSAGES.deleteConfirm(selectedObject.getName()),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean isConfirmed) {
                                    if (isConfirmed) {
                                        presenter.onRemoveResource(cacheType, selectedObject.getName());
                                    }
                                }
                            });
                }
            }
        });

        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(removeButton);


        // forms
        final SecurityContext securityContext = presenter.getSecurityFramework()
                .getSecurityContext(presenter.getProxy().getNameToken());

        ResourceDescription localCacheDescription = presenter.getDescriptionRegistry().lookup(cacheType);

        for (AddressTemplate address : FORMS) {

            ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                    .setAddress(address.getTemplate())
                    .setConfigOnly()
                    .setSecurityContext(securityContext)
                    .setResourceDescription(presenter.getDescriptionRegistry().lookup(address))
                    .build();

            formAssets.getForm().setToolsCallback(new AddressableFormCallback(address, formAssets));

            formMapping.put(address,new FormContainer(formAssets));
        }

        storeForm = new StoreFormWrapper();

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(title)
                .setDescription(localCacheDescription.get("description").asString())
                .setMaster("Available Caches", table)
                .setMasterTools(tools.asWidget())
                .addDetail(Console.CONSTANTS.common_label_attributes(), formMapping.get(cacheType).asWidget())
                .addDetail("Locking", formMapping.get(LOCKING).asWidget())
                .addDetail("Eviction", formMapping.get(EVICTION).asWidget())
                .addDetail("Expiration", formMapping.get(EXPIRATION).asWidget())
                .addDetail("Transaction", formMapping.get(TRANSACTION).asWidget())
                .addDetail("Store", storeForm.asWidget());

        return layout.build();

    }

    private void onSave(AddressTemplate address, boolean isTransient, Map<String, Object> changeset) {

        Property selection = selectionModel.getSelectedObject();
        if(selection!=null) {
            if (isTransient) {
                presenter.onCreate(address, selection.getName(),formMapping.get(address).getForm().getUpdatedEntity());
            } else {
                presenter.onSave(address, selection.getName(), changeset);
            }
        }
    }

    private void updateForms(Property selection) {

        resetForms();

        if(selection!=null && selection.getValue().isDefined()) {
            ModelNode payload = selection.getValue();

            formMapping.get(cacheType).getForm().edit(payload);

            // access to singleton subresources
            if (hasDefined(payload, "component", "locking"))
                formMapping.get(LOCKING).getForm().edit(payload.get("component").get("locking"));
            if (hasDefined(payload, "component", "eviction"))
                formMapping.get(EVICTION).getForm().edit(payload.get("component").get("eviction"));
            if (hasDefined(payload, "component", "expiration"))
                formMapping.get(EXPIRATION).getForm().edit(payload.get("component").get("expiration"));
            if (hasDefined(payload, "component", "transaction"))
                formMapping.get(TRANSACTION).getForm().edit(payload.get("component").get("transaction"));

            if (payload.hasDefined("store")) {
                storeForm.edit(payload.get("store"));
            } else {
                storeForm.edit(null);
            }
        }
    }

    private boolean hasDefined(ModelNode payload, String key, String value) {
        return payload.hasDefined(key) && payload.get(key).hasDefined(value);
    }

    private void resetForms() {
        for (AddressTemplate address : FORMS) {
            FormContainer formContainer = formMapping.get(address);
            formContainer.getForm().clearValues();
        }
    }

    public void updateFrom(List<Property> properties) {
        dataProvider.setList(properties);
        if (properties.isEmpty()) {
            selectionModel.clear();
        } else {
            table.selectDefaultEntity();
        }
        updateForms(selectionModel.getSelectedObject());
    }

    class AddressableFormCallback implements FormCallback<ModelNode> {

        private AddressTemplate address;
        private final ModelNodeFormBuilder.FormAssets assets;

        public AddressableFormCallback(AddressTemplate address, ModelNodeFormBuilder.FormAssets assets) {
            this.address = address;
            this.assets = assets;
        }

        @Override
        public void onSave(Map<String, Object> changeset) {
            boolean isTransient = assets.getForm().getEditedEntity()==null ||
                    !assets.getForm().getEditedEntity().isDefined();
            CommonCacheAttributes.this.onSave(address, isTransient, changeset);
        }

        @Override
        public void onCancel(ModelNode entity) {

        }
    }

    class FormContainer {
        ModelNodeFormBuilder.FormAssets assets;

        public FormContainer(ModelNodeFormBuilder.FormAssets assets) {
            this.assets = assets;
        }

        public ModelNodeForm getForm() {
            return assets.getForm();
        }

        Widget asWidget() {
            VerticalPanel p = new VerticalPanel();
            p.setStyleName("fill-layout-width");
            p.add(assets.getHelp().asWidget());
            p.add(assets.getForm().asWidget());
            return p;
        }
    }

    class StoreFormWrapper {
        private VerticalPanel formPanel;
        private HTML heading;
        private ToolButton addButton;
        private ToolButton removeButton;

        private String currentStore = null;

        public StoreFormWrapper() {
        }

        Widget asWidget() {
            VerticalPanel mainPanel = new VerticalPanel();
            mainPanel.setStyleName("fill-layout-width");
            ToolStrip tools = new ToolStrip();
            heading = new HTML("<h3>Store: none</h3>");
            addButton = new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    presenter.onLaunchAddStoreWizard(cacheType, selectionModel.getSelectedObject().getName());
                }
            });
            removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    Property selectedObject = selectionModel.getSelectedObject();
                    String storeName = selectedObject.getValue().get("store").asProperty().getName();

                    if(selectedObject!=null) {
                        Feedback.confirm(Console.MESSAGES.deleteTitle("Cache Store"),
                            Console.MESSAGES.deleteConfirm(storeName + " store"),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean isConfirmed) {
                                    if (isConfirmed) {
                                        presenter.onRemoveResource(cacheType.append("store=" + storeName), selectedObject.getName());
                                        heading.setText("Store: none");
                                    }
                                }
                            });
                    }
                }
            });

            addButton.setEnabled(false);
            removeButton.setEnabled(false);

            tools.addToolButtonRight(addButton);
            tools.addToolButtonRight(removeButton);
            tools.addToolWidget(heading);
            mainPanel.add(tools);
            formPanel = new VerticalPanel();
            formPanel.setStyleName("fill-layout-width");
            mainPanel.add(formPanel);
            return mainPanel;
        }

        public void edit(ModelNode payload) {
            String storeName = payload != null ? payload.asProperty().getName() : "none";
            boolean isNone = storeName.equals("none");

            addButton.setEnabled(isNone);
            removeButton.setEnabled(!isNone);
            AddressTemplate address = null;
            switch (storeName) {
                case "binary-jdbc":
                    address = BINARY_STORE;
                    break;
                case "custom":
                    address = STORE;
                    break;
                case "file":
                    address = FILE_STORE;
                    break;
                case "mixed-jdbc":
                    address = MIXED_STORE;
                    break;
                case "string-jdbc":
                    address = STRING_STORE;
                    break;
                case "remote":
                    address = REMOTE_STORE;
                    break;
            }

            if (!storeName.equals(currentStore)) {
                heading.setHTML("<h3>Store: " + storeName + "</h3>");
                formPanel.clear();
                if (!isNone) {
                    formPanel.add(formMapping.get(address).assets.getHelp().asWidget());
                    formPanel.add(formMapping.get(address).assets.getForm());
                }
                currentStore = storeName;
            }
            if (!isNone)
                formMapping.get(address).getForm().edit(payload.get(storeName));
        }
    }
}
