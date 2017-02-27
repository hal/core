/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.shared.subsys.activemq;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceAlternativesFormValidation;
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceFormValidation;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.MapAttributeAddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.MapAttributePropertyManager;
import org.jboss.as.console.client.v3.widgets.PropertyEditor;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.shared.subsys.activemq.MessagingAddress.JMSBRIDGE_TEMPLATE;
import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;

/**
 * @author Claudio Miranda
 */
class JMSBridgeList {

    private JMSBridgePresenter presenter;

    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;
    private final SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets formAssets;

    private PropertyEditor sourceContextEditor;
    private PropertyEditor targetContextEditor;
    private ModelNodeFormBuilder.FormAssets sourceCredentialRefForm;
    private ModelNodeFormBuilder.FormAssets targetCredentialRefForm;

    JMSBridgeList(JMSBridgePresenter presenter) {
        this.presenter = presenter;
        ProvidesKey<Property> providesKey = Property::getName;
        this.table = new DefaultCellTable<>(8, providesKey);
        this.dataProvider = new ListDataProvider<>(providesKey);
        this.dataProvider.addDataDisplay(table);
        this.selectionModel = new SingleSelectionModel<>(providesKey);
        this.table.setSelectionModel(selectionModel);
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, "Name");

        SecurityContext securityContext = Console.MODULES.getSecurityFramework()
                .getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(JMSBRIDGE_TEMPLATE);

        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                // they are complex attributes
                .exclude("source-context", "target-context", "source-credential-reference", "target-credential-reference")
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .addFactory("source-password",
                        attributeDescription -> new PasswordBoxItem("source-password", "Source Password", false))
                .addFactory("target-password",
                        attributeDescription -> new PasswordBoxItem("target-password", "Target Password", false))
                .build();

        // this is the save operation when the user click at the "edit" link
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveJmsBridge(getSelectedEntity().getName(), changeset);
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        // this filter is called at a later time, to resolve the selected item name
        // from the table list.
        JMSBridgeSelectionAwareContext jmsBridgeSelectionContext = new JMSBridgeSelectionAwareContext(
                presenter.getStatementContext(), this);

        AddressTemplate jmsBridgeTemplate = JMSBRIDGE_TEMPLATE
                .replaceWildcards(JMSBridgeSelectionAwareContext.SELECTED_ENTITY);

        sourceCredentialRefForm = createComplexForm("source-credential-reference");
        targetCredentialRefForm = createComplexForm("target-credential-reference");

        // cross validate the forms, as there are "alternatives" metadata for the password.
        formAssets.getForm().addFormValidator(new CredentialReferenceAlternativesFormValidation("source-password", sourceCredentialRefForm.getForm(), "Source Credential Reference", true));
        sourceCredentialRefForm.getForm().addFormValidator(new CredentialReferenceAlternativesFormValidation("source-password", formAssets.getForm(), "Attributes", false));

        formAssets.getForm().addFormValidator(new CredentialReferenceAlternativesFormValidation("target-password", targetCredentialRefForm.getForm(), "Target Credential Reference", true));
        sourceCredentialRefForm.getForm().addFormValidator(new CredentialReferenceAlternativesFormValidation("target-password", formAssets.getForm(), "Attributes", false));


        // there are two attributes source-context,target-context, they are of type=OBJECT and value-type=STRING
        // they store key=value properties
        // then, they are added as PropertyEditor
        sourceContextEditor = buildProperties("source-context", jmsBridgeSelectionContext, jmsBridgeTemplate);
        targetContextEditor = buildProperties("target-context", jmsBridgeSelectionContext, jmsBridgeTemplate);

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("JMS Bridges")
                .setDescription(SafeHtmlUtils.fromString(definition.get(DESCRIPTION).asString()))
                .setMaster("", table)
                .setMasterTools(tableButtons())
                .addDetail(Console.CONSTANTS.common_label_attributes(), formAssets.asWidget())
                .addDetail("Source Context", sourceContextEditor.asWidget())
                .addDetail("Target Context", targetContextEditor.asWidget())
                .addDetail("Source Credential Reference", sourceCredentialRefForm.asWidget())
                .addDetail("Target Credential Reference", targetCredentialRefForm.asWidget());

        selectionModel.addSelectionChangeHandler(event -> updatePropertiesData(null));

        return layout.build();
    }

    private ModelNodeFormBuilder.FormAssets createComplexForm(final String complexAttributeName) {

        SecurityContext securityContext = Console.MODULES.getSecurityFramework()
                .getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(JMSBRIDGE_TEMPLATE);

        ModelNodeFormBuilder.FormAssets formAsset = new ComplexAttributeForm(complexAttributeName, securityContext,
                definition).build();

        formAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                presenter.saveAttribute(complexAttributeName, getSelectedEntity().getName(),
                        formAsset.getForm().getUpdatedEntity());
            }

            @Override
            public void onCancel(final Object entity) {
                formAsset.getForm().cancel();
            }
        });
        formAsset.getForm().addFormValidator(new CredentialReferenceFormValidation());
        return formAsset;
    }

    private ToolStrip tableButtons() {
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(),
                        clickEvent -> presenter.onLaunchAddJMSBridgeDialog()));


        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("JMS Bridge"),
                        Console.MESSAGES.deleteConfirm("JMS Bridge " + getSelectedEntity().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteJmsBridge(getSelectedEntity().getName());
                            }
                        })));
        return tools;
    }

    //
    private PropertyEditor buildProperties(String attributeName, JMSBridgeSelectionAwareContext statementContext,
            AddressTemplate addressTemplate) {
        MapAttributePropertyManager propertyManager = new MapAttributePropertyManager(
                addressTemplate,
                attributeName, statementContext, presenter.getDispatcher());

        MapAttributeAddPropertyDialog addDialog = new MapAttributeAddPropertyDialog(propertyManager);

        PropertyEditor propertyEditor = new PropertyEditor.Builder(propertyManager)
                .addDialog(addDialog)
                .build();

        return propertyEditor;
    }

    /*
        This update the table itens for the source-context and target-context attributes
        The source item may be the user selectable item or the default (the first).
     */
    private void updatePropertiesData(Property bridge) {
        Property selectedItem = getSelectedEntity();
        if (bridge != null) { selectedItem = bridge; }
        if (selectedItem != null) {
            ModelNode sourceContextNode = selectedItem.getValue().get("source-context");
            ModelNode targetContextNode = selectedItem.getValue().get("target-context");
            sourceContextEditor.update(sourceContextNode.asPropertyList());
            targetContextEditor.update(targetContextNode.asPropertyList());
            formAssets.getForm().edit(selectedItem.getValue());
            sourceCredentialRefForm.getForm().edit(selectedItem.getValue().get("source-credential-reference"));
            targetCredentialRefForm.getForm().edit(selectedItem.getValue().get("target-credential-reference"));
        } else {
            formAssets.getForm().clearValues();
            sourceContextEditor.clearValues();
            targetContextEditor.clearValues();
            sourceCredentialRefForm.getForm().clearValues();
            targetCredentialRefForm.getForm().clearValues();
        }
        sourceContextEditor.enableToolButtons(selectedItem != null);
        targetContextEditor.enableToolButtons(selectedItem != null);
    }

    public void setBridges(List<Property> bridges) {
        Collections.sort(bridges, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));
        dataProvider.setList(bridges);
        if (bridges.isEmpty()) {
            selectionModel.clear();
        }
        // when add/remove operation is called for the source/target context attribute
        // the bridge list is updated and the current selected item must be updated.
        if (getSelectedEntity() != null) {
            String selectedBridge = getSelectedEntity().getName();
            for (Property bridge : bridges) {
                if (selectedBridge != null && selectedBridge.equals(bridge.getName())) {
                    updatePropertiesData(bridge);
                }
            }
        }
        table.selectDefaultEntity();
    }

    @SuppressWarnings("unchecked")
    public Property getSelectedEntity() {
        return selectionModel.getSelectedObject();
    }

}
