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

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.icons.Icons;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 3/29/11
 */
public class XADataSourceEditor implements PropertyManagement {

    private DataSourcePresenter presenter;
    private DefaultCellTable<XADataSource> dataSourceTable;
    private ListDataProvider<XADataSource> dataSourceProvider;
    private XADataSourceDetails details;
    private PropertyEditor propertyEditor;
    private PoolConfigurationView poolConfig;
    private XADataSourceConnection connectionEditor;
    private DataSourceSecurityEditor securityEditor;
    private DataSourceValidationEditor validationEditor;
    private DataSourceTimeoutEditor<XADataSource> timeoutEditor;
    private DataSourceStatementEditor<XADataSource> statementEditor;
    private ToolButton disableBtn;

    public XADataSourceEditor(DataSourcePresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {


        ToolStrip topLevelTools = new ToolStrip();
        ToolButton commonLabelAddBtn = new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                presenter.launchNewXADatasourceWizard();
            }
        });
        commonLabelAddBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_xADataSourceEditor());
        topLevelTools.addToolButtonRight(commonLabelAddBtn);


        ClickHandler clickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                final XADataSource currentSelection = details.getCurrentSelection();
                if(currentSelection!=null)
                {
                    Feedback.confirm(
                            Console.MESSAGES.deleteTitle("XA Datasource"),
                            Console.MESSAGES.deleteConfirm("XA Datasource "+currentSelection.getName()),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean isConfirmed) {
                                    if (isConfirmed) {
                                        presenter.onDeleteXA(currentSelection);
                                    }
                                }
                            });
                }
            }
        };
        ToolButton deleteBtn = new ToolButton(Console.CONSTANTS.common_label_delete());
        deleteBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_delete_xADataSourceEditor());
        deleteBtn.addClickHandler(clickHandler);
        topLevelTools.addToolButtonRight(deleteBtn);

        // ---

        dataSourceTable = new DefaultCellTable<XADataSource>(8,
                new ProvidesKey<XADataSource>() {
                    @Override
                    public Object getKey(XADataSource item) {
                        return item.getJndiName();
                    }
                });

        dataSourceProvider = new ListDataProvider<XADataSource>();
        dataSourceProvider.addDataDisplay(dataSourceTable);


        TextColumn<DataSource> nameColumn = new TextColumn<DataSource>() {
            @Override
            public String getValue(DataSource record) {
                return record.getName();
            }
        };

        TextColumn<DataSource> jndiNameColumn = new TextColumn<DataSource>() {
            @Override
            public String getValue(DataSource record) {
                return record.getJndiName();
            }
        };

        Column<DataSource, ImageResource> statusColumn =
                new Column<DataSource, ImageResource>(new ImageResourceCell()) {
                    @Override
                    public ImageResource getValue(DataSource dataSource) {

                        ImageResource res = null;

                        if(dataSource.isEnabled())
                            res = Icons.INSTANCE.status_good();
                        else
                            res = Icons.INSTANCE.status_bad();

                        return res;
                    }
                };


        dataSourceTable.addColumn(nameColumn, "Name");
        dataSourceTable.addColumn(jndiNameColumn, "JNDI");
        dataSourceTable.addColumn(statusColumn, "Enabled?");

        // -----------
        details = new XADataSourceDetails(presenter);


        propertyEditor = new PropertyEditor(this, true);
        propertyEditor.setHelpText(Console.CONSTANTS.subsys_jca_dataSource_xaprop_help());

        final SingleSelectionModel<XADataSource> selectionModel = new SingleSelectionModel<XADataSource>();
        selectionModel.addSelectionChangeHandler(
                new SelectionChangeEvent.Handler() {
                    @Override
                    public void onSelectionChange(SelectionChangeEvent event) {
                        XADataSource dataSource = selectionModel.getSelectedObject();
                        String nextState = dataSource.isEnabled() ? Console.CONSTANTS.common_label_disable() : Console.CONSTANTS.common_label_enable();
                        disableBtn.setText(nextState);

                        presenter.loadXAProperties(dataSource.getName());
                        presenter.loadPoolConfig(true, dataSource.getName());
                    }
                });
        dataSourceTable.setSelectionModel(selectionModel);


        ClickHandler disableHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                final XADataSource selection = getCurrentSelection();
                final boolean doEnable = !selection.isEnabled();
                String title = doEnable ? Console.MESSAGES.enableConfirm("XA datasource") : Console.MESSAGES.disableConfirm("XA datasource");
                String text = doEnable ? Console.MESSAGES.enableConfirm("XA datasource "+selection.getName()) : Console.MESSAGES.disableConfirm("XA datasource "+selection.getName()) ;
                Feedback.confirm(title, text,
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onDisableXA(selection, doEnable);
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
                DataSource ds = getCurrentSelection();
                presenter.onSaveXADetails(ds.getName(), changeset);
            }

            @Override
            public void onDelete(XADataSource entity) {
                // n/a
            }
        };

        final FormToolStrip.FormCallback<DataSource> dsCallback = new FormToolStrip.FormCallback<DataSource>() {
            @Override
            public void onSave(Map<String, Object> changeset) {
                DataSource ds = getCurrentSelection();
                presenter.onSaveXADetails(ds.getName(), changeset);
            }

            @Override
            public void onDelete(DataSource entity) {
                // n/a
            }
        };

        connectionEditor = new XADataSourceConnection(presenter, xaCallback);
        securityEditor = new DataSourceSecurityEditor(dsCallback);
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
        timeoutEditor = new DataSourceTimeoutEditor<XADataSource>(xaCallback, true);
        statementEditor = new DataSourceStatementEditor<>(xaCallback, true);

        MultipleToOneLayout builder = new MultipleToOneLayout()
                     .setPlain(true)
                     .setHeadline("JDBC XA Datasources")
                     .setDescription(Console.CONSTANTS.subsys_jca_xadataSources_desc())
                     .setMasterTools(topLevelTools.asWidget())
                     .setMaster("Available Datasources", dataSourceTable)
                     .addDetail("Attributes", details.asWidget())
                     .addDetail("Connection", connectionEditor.asWidget())
                     .addDetail("Pool", poolConfig.asWidget())
                     .addDetail("Security", securityEditor.asWidget())
                     .addDetail("Properties", propertyEditor.asWidget())
                     .addDetail("Validation", validationEditor.asWidget())
                     .addDetail("Timeouts", timeoutEditor.asWidget())
                     .addDetail("Statements", statementEditor.asWidget());

        // build the overall layout
        Widget widget = builder.build();
        // now it's safe to bind the forms
        details.getForm().bind(dataSourceTable);
        connectionEditor.getForm().bind(dataSourceTable);
        poolConfig.getForm().bind(dataSourceTable);
        securityEditor.getForm().bind(dataSourceTable);
        validationEditor.getForm().bind(dataSourceTable);
        timeoutEditor.getForm().bind(dataSourceTable);
        return widget;
    }


    public void updateDataSources(List<XADataSource> datasources) {

        // requires manual cleanup
        propertyEditor.clearValues();

        dataSourceProvider.setList(datasources);

        dataSourceTable.selectDefaultEntity();

    }

    public void setEnabled(boolean isEnabled) {
        details.setEnabled(isEnabled);
    }

    private XADataSource getCurrentSelection() {
        return ((SingleSelectionModel<XADataSource>)dataSourceTable.getSelectionModel()).getSelectedObject();
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
