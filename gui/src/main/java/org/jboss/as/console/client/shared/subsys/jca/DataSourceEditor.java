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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;
import org.jboss.as.console.client.widgets.forms.FormEditor;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
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
public class DataSourceEditor {

    private DataSourcePresenter presenter;
    private DatasourceTable dataSourceTable;
    private DataSourceDetails details;
    private PoolConfigurationView poolConfig;
    private ConnectionProperties connectionProps ;
    private FormEditor<DataSource> securityEditor;
    private DataSourceValidationEditor validationEditor;
    private DataSourceConnectionEditor connectionEditor;
    private DataSourceTimeoutEditor<DataSource> timeoutEditor;
    private ToolButton disableBtn;
    private Widget dataSourceTableWidget;

    public DataSourceEditor(DataSourcePresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {


        ToolStrip topLevelTools = new ToolStrip();
        topLevelTools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                presenter.launchNewDatasourceWizard();
            }
        }));


        ClickHandler clickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                final DataSource currentSelection = details.getCurrentSelection();
                if(currentSelection!=null)
                {
                    Feedback.confirm(
                            Console.MESSAGES.deleteTitle("Datasource"),
                            Console.MESSAGES.deleteConfirm("Datasource "+currentSelection.getName()),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean isConfirmed) {
                                    if (isConfirmed) {
                                        presenter.onDelete(currentSelection);
                                    }
                                }
                            });
                }
            }
        };
        ToolButton deleteBtn = new ToolButton(Console.CONSTANTS.common_label_delete());
        deleteBtn.addClickHandler(clickHandler);
        topLevelTools.addToolButtonRight(deleteBtn);


        // ---

        dataSourceTable = new DatasourceTable();
        dataSourceTableWidget = dataSourceTable.asWidget();

        // -----------
        details = new DataSourceDetails(presenter);
        details.bind(dataSourceTable.getCellTable());

        SingleSelectionModel<DataSource> selectionModel =
                (SingleSelectionModel<DataSource>)dataSourceTable.getCellTable().getSelectionModel();

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler () {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                DataSource selectedObject = ((SingleSelectionModel<DataSource>) dataSourceTable.getCellTable().getSelectionModel()).getSelectedObject();
                if(selectedObject!=null) {
                    presenter.loadPoolConfig(false, selectedObject.getName());
                    presenter.onLoadConnectionProperties(selectedObject.getName());
                }

            }
        });

        // -----------------



        // -----------------

        final FormToolStrip.FormCallback<DataSource> formCallback = new FormToolStrip.FormCallback<DataSource>() {
            @Override
            public void onSave(Map<String, Object> changeset) {
                DataSource ds = getCurrentSelection();
                presenter.onSaveDSDetails(ds.getName(), changeset);
            }

            @Override
            public void onDelete(DataSource entity) {
                // n/a
            }
        };

        connectionEditor = new DataSourceConnectionEditor(presenter, formCallback);

        securityEditor = new DataSourceSecurityEditor(formCallback);

        // -----------------

        connectionProps = new ConnectionProperties(presenter);

        // -----------------

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
            public void onDoFlush(String editedName) {
                if(getCurrentSelection().isEnabled())
                    presenter.onDoFlush(false, editedName);
                else
                    Console.error(Console.CONSTANTS.subsys_jca_error_datasource_notenabled());
            }
        });



        // ----

        validationEditor = new DataSourceValidationEditor(formCallback);

        // ----

        timeoutEditor = new DataSourceTimeoutEditor<DataSource>(formCallback, false);

        // --
        ClickHandler disableHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                final DataSource selection = getCurrentSelection();
                final boolean nextState = !selection.isEnabled();
                String title = nextState ? Console.MESSAGES.enableConfirm("datasource") : Console.MESSAGES.disableConfirm("datasource");
                String text = nextState ? Console.MESSAGES.enableConfirm("datasource "+selection.getName()) : Console.MESSAGES.disableConfirm("datasource "+selection.getName()) ;
                Feedback.confirm(title, text,
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onDisable(selection, nextState);
                                }
                            }
                        });
            }
        };

        disableBtn = new ToolButton(Console.CONSTANTS.common_label_enOrDisable(), disableHandler);
        disableBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_enOrDisable_dataSourceDetails());


        // -----
        // handle modifications to the model

        dataSourceTable.getCellTable().getSelectionModel().addSelectionChangeHandler(
                new SelectionChangeEvent.Handler() {
                    @Override
                    public void onSelectionChange(SelectionChangeEvent event) {
                        DataSource selection = getCurrentSelection();
                        if(selection!=null) {
                            String nextState = selection.isEnabled() ? Console.CONSTANTS.common_label_disable() : Console.CONSTANTS.common_label_enable();
                            disableBtn.setText(nextState);
                        }
                    }
                }) ;


        topLevelTools.addToolButtonRight(disableBtn);


        // --


        MultipleToOneLayout builder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("JDBC Datasources")
                .setDescription(Console.CONSTANTS.subsys_jca_dataSources_desc())
                .setMasterTools(topLevelTools.asWidget())
                .setMaster("Available Datasources", dataSourceTable.getCellTable())
                .addDetail("Attributes", details.asWidget())
                .addDetail("Connection", connectionEditor.asWidget())
                .addDetail("Pool", poolConfig.asWidget())
                .addDetail("Security", securityEditor.asWidget())
                .addDetail("Properties", connectionProps.asWidget())
                .addDetail("Validation", validationEditor.asWidget())
                .addDetail("Timeouts", timeoutEditor.asWidget());

        connectionEditor.getForm().bind(dataSourceTable.getCellTable());
        securityEditor.getForm().bind(dataSourceTable.getCellTable());
        poolConfig.getForm().bind(dataSourceTable.getCellTable());
        validationEditor.getForm().bind(dataSourceTable.getCellTable());
        timeoutEditor.getForm().bind(dataSourceTable.getCellTable());

        return builder.build();
    }


    private DataSource getCurrentSelection() {
        DataSource ds = ((SingleSelectionModel<DataSource>) dataSourceTable.getCellTable().getSelectionModel()).getSelectedObject();
        return ds;
    }

    public void updateDataSources(List<DataSource> datasources) {

        // some cleanup has to be done manually
        connectionProps.clearProperties();


        dataSourceTable.getDataProvider().setList(datasources);

        final DefaultCellTable<DataSource> cellTable = dataSourceTable.getCellTable();
        cellTable.selectDefaultEntity();
    }

    public void setEnabled(boolean isEnabled) {


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
