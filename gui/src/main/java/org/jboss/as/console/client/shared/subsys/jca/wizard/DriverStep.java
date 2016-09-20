/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.subsys.jca.wizard;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

import java.util.List;

/**
 * @author Harald Pehl
 */
class DriverStep<T extends DataSource> extends WizardStep<Context<T>, State> {

    private final NewDatasourceWizard<T> wizard;
    private final List<JDBCDriver> drivers;
    private int selectedTab;
    private Form<JDBCDriver> form;
    private DefaultCellTable<JDBCDriver> table;
    private SingleSelectionModel<JDBCDriver> selectionModel;

    DriverStep(final NewDatasourceWizard<T> wizard, final List<JDBCDriver> drivers, final String title) {
        super(wizard, title);
        this.wizard = wizard;
        this.drivers = drivers;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Widget asWidget(final Context<T> context) {
        // first tab: driver form
        form = new Form<>(JDBCDriver.class);
        TextBoxItem driverName = new TextBoxItem("name", "Name", true);
        TextBoxItem moduleName = new TextBoxItem("driverModuleName", "Module Name", true);
        TextBoxItem driverClass = new TextBoxItem("driverClass", "Driver Class", false);
        TextBoxItem xaDataSource = new TextBoxItem("xaDataSourceClass", "XA DataSource Class", false);
        NumberBoxItem major = new NumberBoxItem("majorVersion", "Major Version") {{
            setRequired(false);
        }};
        NumberBoxItem minor = new NumberBoxItem("minorVersion", "Minor Version") {{
            setRequired(false);
        }};
        if (context.xa) {
            form.setFields(driverName, moduleName, driverClass, xaDataSource, major, minor);
        } else {
            form.setFields(driverName, moduleName, driverClass, major, minor);
        }

        FlowPanel formPanel = new FlowPanel();
        formPanel.add(new FormHelpPanel(context.jdbcDriverHelp, form).asWidget());
        formPanel.add(form);

        // second tab: existing drivers
        table = new DefaultCellTable<>(5);
        TextColumn<JDBCDriver> nameColumn = new TextColumn<JDBCDriver>() {
            @Override
            public String getValue(JDBCDriver record) {
                return record.getName();
            }
        };
        TextColumn<JDBCDriver> moduleColumn = new TextColumn<JDBCDriver>() {
            @Override
            public String getValue(JDBCDriver record) {
                return record.getDriverModuleName();
            }
        };
        table.addColumn(nameColumn, "Name");
        if (!context.standalone) {
            table.addColumn(moduleColumn, "Module");
        }
        selectionModel = new SingleSelectionModel<>();
        table.setSelectionModel(selectionModel);
        provisionTable(table);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        FlowPanel driverPanel = new FlowPanel();
        driverPanel.add(table);
        driverPanel.add(pager);

        // tabs
        TabPanel tabs = new TabPanel();
        tabs.setStyleName("default-tabpanel");
        tabs.addSelectionHandler(event -> selectedTab = event.getSelectedItem());
        tabs.add(formPanel, "Specify Driver");
        tabs.add(driverPanel, "Detected Driver");

        // body
        FlowPanel body = new FlowPanel();
        body.add(new ContentDescription(
               "Select one of the installed JDBC drivers. If you do not see your driver, make sure that it is deployed as a module and properly registered."));
        body.add(tabs);
        tabs.selectTab(0);

        return body;
    }

    private void provisionTable(final CellTable<JDBCDriver> table) {
        table.setRowCount(drivers.size(), true);
        table.setRowData(drivers);

        // clear selection
        JDBCDriver selectedDriver = selectionModel.getSelectedObject();
        if (selectedDriver != null) {
            selectionModel.setSelected(selectedDriver, false);
        }

        // new default selection
        if (drivers.size() > 0) {
            selectionModel.setSelected(drivers.get(0), true);
        }
    }

    @Override
    public void reset(final Context<T> context) {
        form.clearValues();
        selectionModel.clear();
    }

    @Override
    protected void onShow(final Context<T> context) {
        if (context.driver != null) {
            form.edit(context.driver);
        }
    }

    @Override
    protected boolean onNext(final Context<T> context) {
        JDBCDriver driver = null;
        if (selectedTab == 0) {
            FormValidation formValidation = form.validate();
            if (!formValidation.hasErrors()) {
                driver = form.getUpdatedEntity();
            }
        } else {
            form.clearValues();
            SingleSelectionModel<JDBCDriver> selection =
                    (SingleSelectionModel<JDBCDriver>) table.getSelectionModel();
            driver = selection.getSelectedObject();
        }

        if (driver != null) {
            wizard.applyDriver(driver);
            return true;
        } else {
            Console.warning(Console.CONSTANTS.noDriverSpecified());
            return false;
        }
    }
}
