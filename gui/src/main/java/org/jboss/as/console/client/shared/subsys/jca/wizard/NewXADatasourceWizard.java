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
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.jca.DataSourceFinder;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplate;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplates;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 4/15/11
 */
public class NewXADatasourceWizard {

    private final DataSourceFinder presenter;
    private final List<JDBCDriver> drivers;
    private final List<XADataSource> existingXaDataSources;
    private final ApplicationProperties bootstrap;
    private final DataSourceTemplates templates;
    private final BeanFactory beanFactory;

    private XADataSource xaDataSource;

    private DeckPanel deck;
    private ChooseTemplateStep<XADataSource> chooseTemplateStep;
    private XADatasourceStep1 step1;
    private XADatasourceStep2 step2;
    private XADatasourceStep3 step3;
    private XADatasourceStep4 step4;
    private TrappedFocusPanel trap;

    public NewXADatasourceWizard(DataSourceFinder presenter, List<JDBCDriver> drivers,
                                 List<XADataSource> existingXaDataSources, ApplicationProperties bootstrap,
                                 DataSourceTemplates templates, BeanFactory beanFactory) {
        this.presenter = presenter;
        this.drivers = drivers;
        this.existingXaDataSources = existingXaDataSources;
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

    public List<XADataSource> getExistingXaDataSources() {
        return existingXaDataSources;
    }

    ApplicationProperties getBootstrap() {
        return this.bootstrap;
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

        chooseTemplateStep = new ChooseTemplateStep<>(getPresenter(), templates, true, new Command() {
            @Override
            public void execute() {
                onStart();
            }
        });
        deck.add(chooseTemplateStep);

        step1 = new XADatasourceStep1(this);
        deck.add(step1.asWidget());

        step2 = new XADatasourceStep2(this);
        deck.add(step2.asWidget());

        step3 = new XADatasourceStep3(this);
        deck.add(step3.asWidget());

        step4 = new XADatasourceStep4(this);
        deck.add(step4.asWidget());

        trap = new TrappedFocusPanel(deck);
        deck.showWidget(0);
        return trap;
    }

    public void onStart() {
        DataSourceTemplate<XADataSource> dataSourceTemplate = chooseTemplateStep.getSelectedTemplate();
        if (dataSourceTemplate != null) {
            xaDataSource = dataSourceTemplate.getDataSource();
            JDBCDriver driver = dataSourceTemplate.getDriver();
            step1.edit(xaDataSource);
            step2.edit(driver);
            step3.edit(xaDataSource);
            step4.edit(xaDataSource);
        } else {
            xaDataSource = beanFactory.xaDataSource().as();
        }
        deck.showWidget(1);
    }

    public void onConfigureBaseAttributes(XADataSource baseAttributes) {
        xaDataSource.setName(baseAttributes.getName());
        xaDataSource.setJndiName(baseAttributes.getJndiName());
        if (xaDataSource.getPoolName() == null || xaDataSource.getPoolName().length() == 0) {
            xaDataSource.setPoolName(baseAttributes.getName() + "_Pool");
        }
        deck.showWidget(2);
    }

    public void onConfigureDriver(JDBCDriver driver) {
        xaDataSource.setDriverName(driver.getName());
        xaDataSource.setDriverClass(driver.getDriverClass());
        xaDataSource.setDataSourceClass(driver.getXaDataSourceClass());
        xaDataSource.setMajorVersion(driver.getMajorVersion());
        xaDataSource.setMinorVersion(driver.getMinorVersion());
        deck.showWidget(3);
    }

    private static Map<String, PropertyRecord> indexProperties(List<PropertyRecord> properties) {
        Map<String, PropertyRecord> indexedProperties = new HashMap<>();
        for (PropertyRecord record : properties) {
            if (!indexedProperties.containsKey(record.getKey()))
                indexedProperties.put(record.getKey(), record);
            else
                throw new IllegalArgumentException("Duplicate key entry:" + record.getKey());
        }   
        return indexedProperties;
    }   

    private List<PropertyRecord> ensureUniqueProperty(List<PropertyRecord> newProperties) {
        Map<String, PropertyRecord> indexedProperties = indexProperties(newProperties);
        if (xaDataSource.getProperties() != null)
            for (PropertyRecord record : xaDataSource.getProperties() )
                if (indexedProperties.containsKey(record.getKey()))
                    throw new IllegalArgumentException("Property " + record.getKey() + " has already been defined with value:"
                            + record.getValue());
        return newProperties;
    }
    
    public void onConfigureProperties(List<PropertyRecord> properties) {
        if (xaDataSource.getProperties() == null) {
            xaDataSource.setProperties(new ArrayList<>());
        }
        xaDataSource.getProperties().clear();
        xaDataSource.getProperties().addAll(ensureUniqueProperty(properties));
        deck.showWidget(4);
    }

    public void onVerifyConnection(final XADataSource updatedEntity, final boolean xa, final boolean existing) {
        mergeAttributes(updatedEntity);
        presenter.verifyConnection(xaDataSource, xa, existing);
    }

    public void onFinish(XADataSource updatedEntity) {
        mergeAttributes(updatedEntity);
        presenter.onCreateXADatasource(xaDataSource);
    }

    private void mergeAttributes(final XADataSource connection) {
        xaDataSource.setUsername(connection.getUsername());
        xaDataSource.setPassword(connection.getPassword());
        xaDataSource.setSecurityDomain(connection.getSecurityDomain());
    }
}
