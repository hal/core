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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.forms.FormEditor;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.shared.subsys.jca.DataSourcePresenter.DATASOURCE_TEMPLATE;
import static org.jboss.dmr.client.ModelDescriptionConstants.CREDENTIAL_REFERENCE;

/**
 * @author Heiko Braun
 * @date 3/29/11
 */
public class DataSourceEditor {

    private DataSourcePresenter presenter;
    private DataSourceDetails details;
    private PoolConfigurationView poolConfig;
    private ConnectionProperties connectionProps;
    private FormEditor<DataSource> securityEditor;
    private DataSourceValidationEditor validationEditor;
    private DataSourceConnectionEditor connectionEditor;
    private DataSourceTimeoutEditor<DataSource> timeoutEditor;
    private DataSourceStatementEditor<DataSource> statementEditor;
    private ModelNodeFormBuilder.FormAssets credentialReferenceFormAsset;
    private ToolButton disableBtn;
    private DataSource selectedEntity = null;
    private HTML title;

    public DataSourceEditor(DataSourcePresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {


        ToolStrip topLevelTools = new ToolStrip();

        details = new DataSourceDetails(presenter);

        ClickHandler disableHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {


                final boolean nextState = !selectedEntity.isEnabled();
                String title = nextState ? Console.MESSAGES.enableConfirm("datasource") : Console.MESSAGES
                        .disableConfirm("datasource");
                String text = nextState ? Console.MESSAGES
                        .enableConfirm("datasource " + selectedEntity.getName()) : Console.MESSAGES
                        .disableConfirm("datasource " + selectedEntity.getName());
                Feedback.confirm(title, text,
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onDisable(selectedEntity, nextState);
                                }
                            }
                        });
            }
        };

        disableBtn = new ToolButton(Console.CONSTANTS.common_label_enOrDisable(), disableHandler);
        disableBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_enOrDisable_dataSourceDetails());

        topLevelTools.addToolButtonRight(disableBtn);


        // -----------------

        final FormToolStrip.FormCallback<DataSource> formCallback = new FormToolStrip.FormCallback<DataSource>() {
            @Override
            public void onSave(Map<String, Object> changeset) {

                presenter.onSaveDSDetails(selectedEntity.getName(), changeset);
            }

            @Override
            public void onDelete(DataSource entity) {
                // n/a
            }
        };

        connectionEditor = new DataSourceConnectionEditor(presenter, formCallback);

        securityEditor = new DataSourceSecurityEditor(formCallback);

        // credential-reference attribute
        SecurityFramework securityFramework = Console.MODULES.getSecurityFramework();
        ResourceDescription resourceDescription = presenter.getResourceDescriptionRegistry()
                .lookup(DATASOURCE_TEMPLATE);
        credentialReferenceFormAsset = new ComplexAttributeForm(CREDENTIAL_REFERENCE,
                securityFramework.getSecurityContext(presenter.getProxy().getNameToken()), resourceDescription).build();
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


        connectionProps = new ConnectionProperties(presenter);

        poolConfig = new PoolConfigurationView(new PoolManagement() {
            @Override
            public void onSavePoolConfig(String parentName, Map<String, Object> changeset) {
                presenter.onSavePoolConfig(parentName, changeset, false);
            }

            @Override
            public void onResetPoolConfig(String parentName, PoolConfig entity) {
                presenter.onDeletePoolConfig(parentName, entity, false);
            }

            @Override
            public void onDoFlush(String editedName, String flushOp) {
                if (selectedEntity.isEnabled()) {
                    presenter.onDoFlush(false, editedName, flushOp);
                } else {
                    Console.error(Console.CONSTANTS.subsys_jca_error_datasource_notenabled());
                }
            }
        });


        // ----

        validationEditor = new DataSourceValidationEditor(formCallback);

        // ----

        timeoutEditor = new DataSourceTimeoutEditor<DataSource>(formCallback, false);
        statementEditor = new DataSourceStatementEditor<>(formCallback, false);


        title = new HTML();
        title.setStyleName("content-header-label");

        OneToOneLayout builder = new OneToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(title)
                .setDescription(Console.CONSTANTS.subsys_jca_dataSources_desc())
                .setMaster("", topLevelTools.asWidget())
                .addDetail(Console.CONSTANTS.common_label_attributes(), details.asWidget())
                .addDetail("Connection", connectionEditor.asWidget())
                .addDetail("Pool", poolConfig.asWidget())
                .addDetail("Security", securityEditor.asWidget())
                .addDetail("Credential Reference", credentialReferenceFormAsset.asWidget())
                .addDetail(Console.CONSTANTS.common_label_properties(), connectionProps.asWidget())
                .addDetail("Validation", validationEditor.asWidget())
                .addDetail("Timeouts", timeoutEditor.asWidget())
                .addDetail("Statements", statementEditor.asWidget());

        return builder.build();
    }

    public void updateDataSource(DataSource ds) {

        this.selectedEntity = ds;
        details.updateFrom(ds);

        String suffix = ds.isEnabled() ? " (enabled)" : " (disabled)";
        title.setHTML("JDBC datasource '" + ds.getName() + "'" + suffix);

        String nextState = ds.isEnabled() ? Console.CONSTANTS.common_label_disable() : Console.CONSTANTS
                .common_label_enable();
        disableBtn.setText(nextState);

        // some cleanup has to be done manually
        connectionProps.clearProperties();

        connectionEditor.getForm().edit(ds);
        securityEditor.getForm().edit(ds);
        if (ds.getCredentialReference() != null) {
            ModelNode bean = presenter.getCredentialReferenceAdapter().fromEntity(ds.getCredentialReference());
            credentialReferenceFormAsset.getForm().edit(bean);
        } else {
            // if there is no credential-reference in the model, an empty one allows for edit operation.
            credentialReferenceFormAsset.getForm().edit(new ModelNode());
        }

        validationEditor.getForm().edit(ds);
        timeoutEditor.getForm().edit(ds);
        statementEditor.getForm().edit(ds);

        // used to be selection model callbacks
        presenter.loadPoolConfig(false, ds.getName());
        presenter.onLoadConnectionProperties(ds.getName());
    }

    public void enableDetails(boolean b) {
        details.setEnabled(b);
    }

    public void setPoolConfig(String name, PoolConfig poolConfig) {
        this.poolConfig.updateFrom(name, poolConfig);
    }

    public void setConnectionProperties(String reference, List<PropertyRecord> properties) {
        connectionProps.setProperties(reference, properties);
    }
}
