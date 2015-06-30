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

package org.jboss.as.console.client.shared.subsys.jca.wizard;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.subsys.jca.DataSourceFinder;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplate;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplates;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 4/15/11
 */
public class NewDatasourceWizard {

    private final DataSourceFinder presenter;
    private final List<DataSource> existingDataSources;
    private final List<JDBCDriver> drivers;
    private final ApplicationProperties bootstrap;
    private final DataSourceTemplates templates;
    private final BeanFactory beanFactory;

    private DataSource dataSource;

    private DeckPanel deck;
    private ChooseTemplateStep<DataSource> chooseTemplateStep;
    private DatasourceStep1 step1;
    private DatasourceStep2 step2;
    private DataSourceStep3 step3;
    private TrappedFocusPanel trap;

    public NewDatasourceWizard(DataSourceFinder presenter, List<JDBCDriver> drivers,
                               List<DataSource> existingDataSources, ApplicationProperties bootstrap,
                               DataSourceTemplates templates, BeanFactory beanFactory) {
        this.presenter = presenter;
        this.drivers = drivers;
        this.existingDataSources = existingDataSources;
        this.bootstrap = bootstrap;
        this.templates = templates;
        this.beanFactory = beanFactory;
    }

    public DataSourceFinder getPresenter() {
        return presenter;
    }

    public List<JDBCDriver> getDrivers() {
        return drivers;
    }

    public List<DataSource> getExistingDataSources() {
        return existingDataSources;
    }

    ApplicationProperties getBootstrap() {
        return bootstrap;
    }

    public Widget asWidget() {
        deck = new DeckPanel() {
            @Override
            public void showWidget(int index) {
                super.showWidget(index);
                trap.getFocus().reset(getWidget(index).getElement());
                trap.getFocus().onFirstInput();
            }
        };

        chooseTemplateStep = new ChooseTemplateStep<DataSource>(getPresenter(), templates, false, new Command() {
            @Override
            public void execute() {
                onStart();
            }
        });
        deck.add(chooseTemplateStep);

        step1 = new DatasourceStep1(this);
        deck.add(step1.asWidget());

        step2 = new DatasourceStep2(this);
        deck.add(step2.asWidget());

        step3 = new DataSourceStep3(this);
        deck.add(step3.asWidget());

        trap = new TrappedFocusPanel(deck);
        deck.showWidget(0);
        return trap;
    }

    public void onStart() {
        DataSourceTemplate<DataSource> dataSourceTemplate = chooseTemplateStep.getSelectedTemplate();
        if (dataSourceTemplate != null) {
            dataSource = dataSourceTemplate.getDataSource();
            JDBCDriver driver = dataSourceTemplate.getDriver();
            step1.edit(dataSource);
            step2.edit(driver);
            step3.edit(dataSource);
        } else {
            dataSource = beanFactory.dataSource().as();
        }
        deck.showWidget(1);
    }

    public void onConfigureBaseAttributes(DataSource baseAttributes) {
        dataSource.setName(baseAttributes.getName());
        dataSource.setJndiName(baseAttributes.getJndiName());
        if (dataSource.getPoolName() == null || dataSource.getPoolName().length() == 0) {
            dataSource.setPoolName(baseAttributes.getName() + "_Pool");
        }
        deck.showWidget(2);
    }

    public void onConfigureDriver(JDBCDriver driver) {
        dataSource.setDriverName(driver.getName());
        dataSource.setDriverClass(driver.getDriverClass());
        dataSource.setMajorVersion(driver.getMajorVersion());
        dataSource.setMinorVersion(driver.getMinorVersion());
        deck.showWidget(3);
    }

    public void onFinish(DataSource updatedEntity) {
        mergeAttributes(updatedEntity);
        presenter.onCreateDatasource(dataSource);
    }

    public void onVerifyConnection(final DataSource updatedEntity, final boolean xa, final boolean existing) {
        mergeAttributes(updatedEntity);
        presenter.verifyConnection(dataSource, xa, existing);
    }

    private void mergeAttributes(final DataSource connection) {
        dataSource.setConnectionUrl(connection.getConnectionUrl());
        dataSource.setUsername(connection.getUsername());
        dataSource.setPassword(connection.getPassword());
        dataSource.setSecurityDomain(connection.getSecurityDomain());
    }
}
