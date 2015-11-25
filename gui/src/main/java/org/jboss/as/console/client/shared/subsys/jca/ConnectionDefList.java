package org.jboss.as.console.client.shared.subsys.jca;

import com.google.common.collect.Collections2;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.jca.model.CapacityPolicy;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.PropertyEditor;
import org.jboss.as.console.client.v3.widgets.SubResourceAddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.SubResourcePropertyManager;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Heiko Braun
 */
public class ConnectionDefList {


    private final ResourceAdapterPresenter presenter;
    private final static AddressTemplate ADDRESS_TEMPLATE = AddressTemplate.of("{selected.profile}/subsystem=resource-adapters/resource-adapter=*/connection-definitions=*");
    private final static AddressTemplate PROPS_ADDRESS = ADDRESS_TEMPLATE.append("config-properties=*");

    private final DefaultCellTable table;
    private final ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private SubResourceAddPropertyDialog addDialog;
    private PropertyEditor configProperties;
    private ModelNodeFormBuilder.FormAssets formAssets;
    private ModelNodeFormBuilder.FormAssets poolAssets;
    private ModelNodeFormBuilder.FormAssets secAssets;
    private ModelNodeFormBuilder.FormAssets validationAssets;
    private ModelNodeFormBuilder.FormAssets recoveryAssets;

    public ConnectionDefList(ResourceAdapterPresenter presenter) {
        this.presenter = presenter;

        this.table = new DefaultCellTable(5);
        this.dataProvider = new ListDataProvider<Property>();
        this.dataProvider.addDataDisplay(table);
    }

    public Widget asWidget() {
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        table.addColumn(nameColumn, "Name");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onLaunchAddWizard(ADDRESS_TEMPLATE);
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle("Connection Definition"),
                        Console.MESSAGES.deleteConfirm("Connection Definition '" + getCurrentSelection().getName() + "'"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onRemoveChildResource(ADDRESS_TEMPLATE, getCurrentSelection());
                                }
                            }
                        });
            }
        }));

        SecurityContext securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(ADDRESS_TEMPLATE);

        String[] poolAttributes = new String[] {
                "min-pool-size",
                "max-pool-size",
                "initial-pool-size",
                "pool-prefill",
                "pool-use-strict-min",
                "flush-strategy",
                "use-fast-fail",
                "capacity-decrementer-class",
                "capacity-decrementer-properties",
                "capacity-incrementer-class",
                "capacity-incrementer-properties"
        };

        String[] secAttributes = new String[] {
                "security-application",
                "security-domain",
                "security-domain-and-application"
        };


        String[] validationAttributes = new String[] {
                "background-validation",
                "background-validation-millis",
                "validate-on-match"
        };

        String[] recoveryAttributes = new String[] {
                "no-recovery",
                "recovery-user",
                "recovery-password",
                "recovery-security-domain",
                "recovery-plugin-class-name",
                "recovery-plugin-properties"
        };

        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .exclude(poolAttributes, secAttributes, validationAttributes, recoveryAttributes);

        final FormCallback callback = new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveChildResource(ADDRESS_TEMPLATE, getCurrentSelection().getName(), changeset);
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
                poolAssets.getForm().cancel();
                validationAssets.getForm().cancel();
                secAssets.getForm().cancel();
                recoveryAssets.getForm().cancel();
            }
        };

        formAssets = builder.build();
        formAssets.getForm().setToolsCallback(callback);

        // ---

        ModelNodeFormBuilder poolBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .include(poolAttributes);

         // decrementer
        Set<CapacityPolicy> decs = Sets.filter(EnumSet.allOf(CapacityPolicy.class),
                (capacityPolicy) -> !capacityPolicy.isIncrement());
        Collection<String> decNames = Collections2.transform(decs, CapacityPolicy::className);
        ComboBoxItem decrementerClass = new ComboBoxItem("capacity-decrementer-class", "Decrementer Class");
        decrementerClass.setRequired(false);
        decrementerClass.setValueMap(Ordering.natural().immutableSortedCopy(decNames));

        // incrementer
        Set<CapacityPolicy> incs = Sets.filter(EnumSet.allOf(CapacityPolicy.class), CapacityPolicy::isIncrement);
        Collection<String> incNames = Collections2.transform(incs, CapacityPolicy::className);
        ComboBoxItem incrementerClass = new ComboBoxItem("capacity-incrementer-class", "Incrementer Class");
        incrementerClass.setRequired(false);
        incrementerClass.setValueMap(Ordering.natural().immutableSortedCopy(incNames));

        poolBuilder.addFactory("capacity-incrementer-class", new ModelNodeFormBuilder.FormItemFactory() {
            @Override
            public FormItem create(Property attributeDescription) {
                return incrementerClass;
            }
        });

        poolBuilder.addFactory("capacity-decrementer-class", new ModelNodeFormBuilder.FormItemFactory() {
            @Override
            public FormItem create(Property attributeDescription) {
                return decrementerClass;
            }
        });

        poolAssets = poolBuilder.build();
        poolAssets.getForm().setToolsCallback(callback);

        // ----


        ModelNodeFormBuilder secBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .include(secAttributes);

        secAssets = secBuilder.build();
        secAssets.getForm().setToolsCallback(callback);

        // ----

        ModelNodeFormBuilder validationBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .include(validationAttributes);

        validationAssets = validationBuilder.build();
        validationAssets.getForm().setToolsCallback(callback);

        // ----

        ModelNodeFormBuilder recoveryBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .include(recoveryAttributes)
                .addFactory("recovery-password", new ModelNodeFormBuilder.FormItemFactory() {
                    @Override
                    public FormItem create(Property attributeDescription) {

                        PasswordBoxItem item = new PasswordBoxItem("recovery-password", "Recovery Password", false);
                        return item;
                    }
                });

        recoveryAssets = recoveryBuilder.build();
        recoveryAssets.getForm().setToolsCallback(callback);

        // ----

        SubResourcePropertyManager propertyManager = new SubResourcePropertyManager(PROPS_ADDRESS, presenter.getStatementContext(), presenter.getDispatcher())
        {
            @Override
            public void onAdd(Property property, AddPropertyDialog addDialog) {
                presenter.onCreateProperty(PROPS_ADDRESS, property.getValue(), getCurrentSelection().getName(), property.getName());
                ConnectionDefList.this.addDialog.hide();
            }

            @Override
            public void onRemove(Property property) {
                presenter.onRemoveProperty(PROPS_ADDRESS, getCurrentSelection().getName(), property.getName());
                ConnectionDefList.this.addDialog.hide();
            }
        };

        addDialog =
                new SubResourceAddPropertyDialog(
                        propertyManager,
                        securityContext,
                        definition.getChildDescription("config-properties")
                );

        configProperties = new PropertyEditor.Builder(propertyManager)
                .addDialog(addDialog)
                .operationAddress(PROPS_ADDRESS) // this address is used in the security context
                .build();



        // ----
        final MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Connection Definitions")
                .setDescription(definition.get("description").asString())
                .setMasterTools(tools)
                .setMaster("", table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), formAssets.asWidget())
                .addDetail(Console.CONSTANTS.common_label_properties(), configProperties.asWidget())
                .addDetail("Pool", poolAssets.asWidget())
                .addDetail("Security", secAssets.asWidget())
                .addDetail("Validation", validationAssets.asWidget())
                .addDetail("Recovery", recoveryAssets.asWidget())
                ;

        selectionModel = new SingleSelectionModel<Property>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property adminObject = selectionModel.getSelectedObject();
                if(adminObject!=null)
                {
                    formAssets.getForm().edit(adminObject.getValue());
                    poolAssets.getForm().edit(adminObject.getValue());
                    secAssets.getForm().edit(adminObject.getValue());
                    validationAssets.getForm().edit(adminObject.getValue());
                    recoveryAssets.getForm().edit(adminObject.getValue());

                    List<Property> props = adminObject.getValue().hasDefined("config-properties") ?
                            adminObject.getValue().get("config-properties").asPropertyList() : Collections.EMPTY_LIST;

                    configProperties.update(props);
                }
                else
                {
                    formAssets.getForm().clearValues();
                }
            }
        });
        table.setSelectionModel(selectionModel);
        return layoutBuilder.build();
    }

    private Property getCurrentSelection() {
        return ((SingleSelectionModel<Property>)table.getSelectionModel()).getSelectedObject();
    }

    public void setData(List<Property> data) {
        selectionModel.clear();
        dataProvider.setList(data);
        table.selectDefaultEntity();

    }
}

