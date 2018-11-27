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

package org.jboss.as.console.client.shared.subsys.jca;

import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceAlternativesFormValidation;
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceFormValidation;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.SECURITY_DOMAIN;
import static org.jboss.as.console.client.shared.subsys.jca.XADataSourcePresenter.XADATASOURCE_TEMPLATE;
import static org.jboss.dmr.client.ModelDescriptionConstants.CREDENTIAL_REFERENCE;

/**
 * @author Heiko Braun
 * @date 3/29/11
 */
public class XADataSourceEditor implements PropertyManagement {

    private XADataSourcePresenter presenter;
    private XADataSourceDetails details;
    private PropertyEditor propertyEditor;
    private PoolConfigurationView poolConfig;
    private XADataSourceConnection connectionEditor;
    private DataSourceValidationEditor validationEditor;
    private DataSourceTimeoutEditor<XADataSource> timeoutEditor;
    private DataSourceStatementEditor<XADataSource> statementEditor;
    private ModelNodeFormBuilder.FormAssets securityFormAsset;
    private ModelNodeFormBuilder.FormAssets recoveryFormAsset;
    private ModelNodeFormBuilder.FormAssets credentialReferenceFormAsset;
    private ToolButton disableBtn;
    private HTML title;
    private XADataSource selectedEntity = null;


    public XADataSourceEditor(XADataSourcePresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {


        ToolStrip topLevelTools = new ToolStrip();

        details = new XADataSourceDetails(presenter);

        propertyEditor = new PropertyEditor(this, true);
        propertyEditor.setHelpText(Console.CONSTANTS.subsys_jca_dataSource_xaprop_help());

        ClickHandler disableHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                final boolean doEnable = !selectedEntity.isEnabled();
                String title = doEnable ? Console.MESSAGES.enableConfirm("XA datasource") : Console.MESSAGES
                        .disableConfirm("XA datasource");
                String text = doEnable ? Console.MESSAGES
                        .enableConfirm("XA datasource " + selectedEntity.getName()) : Console.MESSAGES
                        .disableConfirm("XA datasource " + selectedEntity.getName());
                Feedback.confirm(title, text,
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onDisableXA(selectedEntity, doEnable);
                                }
                            }
                        });
            }
        };

        disableBtn = new ToolButton(Console.CONSTANTS.common_label_enOrDisable());
        disableBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_enOrDisable_xADataSourceDetails());
        disableBtn.addClickHandler(disableHandler);
        topLevelTools.addToolButtonRight(disableBtn);


        // -----

        final FormToolStrip.FormCallback<XADataSource> xaCallback = new FormToolStrip.FormCallback<XADataSource>() {
            @Override
            public void onSave(Map<String, Object> changeset) {
                presenter.onSaveXADetails(selectedEntity.getName(), changeset);
            }

            @Override
            public void onDelete(XADataSource entity) {
                // n/a
            }
        };

        final FormToolStrip.FormCallback<DataSource> dsCallback = new FormToolStrip.FormCallback<DataSource>() {
            @Override
            public void onSave(Map<String, Object> changeset) {
                presenter.onSaveXADetails(selectedEntity.getName(), changeset);
            }

            @Override
            public void onDelete(DataSource entity) {
                // n/a
            }
        };

        connectionEditor = new XADataSourceConnection(presenter, xaCallback);

        SecurityFramework securityFramework = Console.MODULES.getSecurityFramework();
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription resourceDescription = presenter.getResourceDescriptionRegistry()
                .lookup(XADATASOURCE_TEMPLATE);

        securityFormAsset = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext)
                .createValidators(true)
                .include("user-name", "password", "allow-multiple-users", "authentication-context", "elytron-enabled", "security-domain")
                .addFactory("security-domain", attributeDescription -> new SuggestionResource("security-domain", "Security Domain", false,
                        Console.MODULES.getCapabilities().lookup(SECURITY_DOMAIN)).buildFormItem())
                .addFactory("password", attributeDescription -> new PasswordBoxItem("password", "Password", false))
                .build();

        securityFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                presenter.onSaveDatasource(XADATASOURCE_TEMPLATE, selectedEntity.getName(), changeset);
            }

            @Override
            public void onCancel(final Object entity) {
                securityFormAsset.getForm().cancel();
            }
        });

        // credential-reference attribute
        credentialReferenceFormAsset = new ComplexAttributeForm(CREDENTIAL_REFERENCE,
                securityContext, resourceDescription).build();
        credentialReferenceFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                ModelNode updatedEntity = credentialReferenceFormAsset.getForm().getUpdatedEntity();
                for (Property prop : updatedEntity.asPropertyList()) {
                    if (!prop.getValue().isDefined()) {
                        updatedEntity.remove(prop.getName());
                    }
                }
                presenter.onSaveComplexAttribute(selectedEntity.getName(), CREDENTIAL_REFERENCE, updatedEntity);
            }

            @Override
            public void onCancel(final Object entity) {
                credentialReferenceFormAsset.getForm().cancel();
            }
        });
        credentialReferenceFormAsset.getForm().addFormValidator(new CredentialReferenceFormValidation());

        credentialReferenceFormAsset.getForm().setResetCallback(() -> presenter
                .onSaveComplexAttribute(selectedEntity.getName(), CREDENTIAL_REFERENCE,
                        new ModelNode().setEmptyList()));

        // cross validate the forms, as there are "alternatives" metadata for the password.
        securityFormAsset.getForm().addFormValidator(new CredentialReferenceAlternativesFormValidation("password", credentialReferenceFormAsset.getForm(), "Credential Reference", true));
        credentialReferenceFormAsset.getForm().addFormValidator(new CredentialReferenceAlternativesFormValidation("password", securityFormAsset.getForm(), "Security", false));


        recoveryFormAsset = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext)
                .createValidators(true)
                .include("recovery-authentication-context", "recovery-elytron-enabled", "recovery-security-domain",
                        "recovery-username", "recovery-password", "recovery-plugin-class-name")
                .addFactory("recovery-security-domain", attributeDescription -> new SuggestionResource("recovery-security-domain", "Recovery Security Domain", false,
                        Console.MODULES.getCapabilities().lookup(SECURITY_DOMAIN)).buildFormItem())
                .addFactory("recovery-password", attributeDescription -> new PasswordBoxItem("recovery-password", "Recovery Password", false))
                .build();
        recoveryFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                presenter.onSaveXARecovery(selectedEntity.getName(), changeset);
            }

            @Override
            public void onCancel(final Object entity) {
                recoveryFormAsset.getForm().cancel();
            }
        });

        poolConfig = new PoolConfigurationView(new PoolManagement() {
            @Override
            public void onSavePoolConfig(String parentName, Map<String, Object> changeset) {
                presenter.onSavePoolConfig(parentName, changeset, true);
            }

            @Override
            public void onResetPoolConfig(String parentName, PoolConfig entity) {
                presenter.onDeletePoolConfig(parentName, entity, true);
            }

            @Override
            public void onDoFlush(String editedName, String flushOp) {
                presenter.onDoFlush(true, editedName, flushOp);
            }
        });
        validationEditor = new DataSourceValidationEditor(dsCallback);
        timeoutEditor = new DataSourceTimeoutEditor<>(xaCallback, true);
        statementEditor = new DataSourceStatementEditor<>(xaCallback, true);

        title = new HTML();
        title.setStyleName("content-header-label");


        OneToOneLayout builder = new OneToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(title)
                .setDescription(Console.CONSTANTS.subsys_jca_xadataSources_desc())
                .setMaster("", topLevelTools.asWidget())
                .addDetail(Console.CONSTANTS.common_label_attributes(), details.asWidget())
                .addDetail("Connection", connectionEditor.asWidget())
                .addDetail("Pool", poolConfig.asWidget())
                .addDetail("Security", securityFormAsset.asWidget())
                .addDetail("Credential Reference", credentialReferenceFormAsset.asWidget())
                .addDetail("Properties", propertyEditor.asWidget())
                .addDetail("Validation", validationEditor.asWidget())
                .addDetail("Timeouts", timeoutEditor.asWidget())
                .addDetail("Statements", statementEditor.asWidget())
                .addDetail("Recovery", recoveryFormAsset.asWidget());

        // build the overall layout
        Widget widget = builder.build();

        return widget;
    }


    public void updateDataSource(XADataSource ds) {

        this.selectedEntity = ds;

        // requires manual cleanup
        propertyEditor.clearValues();

        details.updateFrom(ds);

        String suffix = ds.isEnabled() ? " (enabled)" : " (disabled)";
        title.setHTML(SafeHtmlUtils.fromString("JDBC datasource '" + ds.getName() + "'" + suffix));

        String nextState = ds.isEnabled() ? Console.CONSTANTS.common_label_disable() : Console.CONSTANTS
                .common_label_enable();
        disableBtn.setText(nextState);

        details.getForm().edit(ds);
        connectionEditor.getForm().edit(ds);
        ModelNode datasourceBean = presenter.getXaDataSourceAdapter().fromEntity(ds);
        securityFormAsset.getForm().edit(datasourceBean);

        if (ds.getCredentialReference() != null) {
            ModelNode bean = presenter.getCredentialReferenceAdapter().fromEntity(ds.getCredentialReference());
            credentialReferenceFormAsset.getForm().edit(bean);
        } else {
            // if there is no credential-reference in the model, an empty one allows for edit operation.
            credentialReferenceFormAsset.getForm().editTransient(new ModelNode());
        }

        validationEditor.getForm().edit(ds);
        timeoutEditor.getForm().edit(ds);
        recoveryFormAsset.getForm().edit(datasourceBean);

        presenter.loadXAProperties(ds.getName());
        presenter.loadPoolConfig(true, ds.getName());
    }

    public void setEnabled(boolean isEnabled) {
        details.setEnabled(isEnabled);
    }

    // property management below

    @Override
    public void onCreateProperty(String reference, PropertyRecord prop) {
        presenter.onCreateXAProperty(reference, prop);
    }

    @Override
    public void onDeleteProperty(String reference, PropertyRecord prop) {
        presenter.onDeleteXAProperty(reference, prop);
    }

    @Override
    public void onChangeProperty(String reference, PropertyRecord prop) {

    }

    @Override
    public void launchNewPropertyDialoge(String reference) {
        presenter.launchNewXAPropertyDialoge(reference);
    }

    @Override
    public void closePropertyDialoge() {
        presenter.closeXAPropertyDialoge();
    }

    public void enableDetails(boolean b) {
        details.setEnabled(b);
    }

    public void setPoolConfig(String name, PoolConfig poolConfig) {
        this.poolConfig.updateFrom(name, poolConfig);
    }

    public void setXaProperties(String dataSourceName, List<PropertyRecord> result) {
        propertyEditor.setProperties(dataSourceName, result);
    }
}
