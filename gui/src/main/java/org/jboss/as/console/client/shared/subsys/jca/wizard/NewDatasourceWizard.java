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

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.jca.DataSourceFinder;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplates;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.as.console.client.v3.widgets.wizard.Wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.console.client.shared.subsys.jca.wizard.State.*;

/**
 * @author Harald Pehl
 */
public class NewDatasourceWizard<T extends DataSource> extends Wizard<Context<T>, State> {

    private final DataSourceFinder presenter;

    public NewDatasourceWizard(final DataSourceFinder presenter,
            final BootstrapContext bootstrapContext,
            final BeanFactory beanFactory,
            final DataSourceTemplates templates,
            final List<DataSource> existingDataSources,
            final List<JDBCDriver> drivers,
            final boolean xa) {
        super(xa ? "new_xa_datasource" : "new_datasource",
                new Context<>(beanFactory, bootstrapContext.isStandalone(), xa));
        this.presenter = presenter;

        addStep(CHOOSE_TEMPLATE, new ChooseTemplateStep<>(this, templates));
        addStep(NAMES, new NamesStep<>(this, existingDataSources, xa ?
                Console.CONSTANTS.subsys_jca_xadataSource_step1() :
                Console.CONSTANTS.subsys_jca_dataSource_step1()));
        addStep(DRIVER, new DriverStep<>(this, drivers, xa ?
                Console.CONSTANTS.subsys_jca_xadataSource_step2() :
                Console.CONSTANTS.subsys_jca_dataSource_step2()));
        addStep(PROPERTIES, new PropertiesStep<>(this, beanFactory));
        addStep(CONNECTION, new ConnectionStep<>(this, xa ?
                Console.CONSTANTS.subsys_jca_xadataSource_step4() :
                Console.CONSTANTS.subsys_jca_dataSource_step3()));
    }

    @Override
    protected State back(final State state) {
        State previous = null;
        switch (state) {
            case CHOOSE_TEMPLATE:
                break;
            case NAMES:
                previous = CHOOSE_TEMPLATE;
                break;
            case DRIVER:
                previous = NAMES;
                break;
            case PROPERTIES:
                previous = DRIVER;
                break;
            case CONNECTION:
                previous = context.xa ? PROPERTIES : DRIVER;
                break;
        }
        return previous;
    }

    @Override
    protected State next(final State state) {
        State next = null;
        switch (state) {
            case CHOOSE_TEMPLATE:
                next = NAMES;
                break;
            case NAMES:
                next = DRIVER;
                break;
            case DRIVER:
                next = context.xa ? PROPERTIES : CONNECTION;
                break;
            case PROPERTIES:
                next = CONNECTION;
                break;
            case CONNECTION:
                break;
        }
        return next;
    }

    void applyNames(final T dataSource) {
        context.dataSource().setName(dataSource.getName());
        context.dataSource().setJndiName(dataSource.getJndiName());
        if (dataSource.getPoolName() == null || dataSource.getPoolName().length() == 0) {
            context.dataSource().setPoolName(dataSource.getName() + "_Pool");
        }
    }

    void applyDriver(JDBCDriver driver) {
        context.dataSource().setDriverName(driver.getName());
        if (context.xa) {
            context.dataSource().setDriverClass(driver.getDriverClass());
        }
        context.dataSource().setDriverClass(driver.getDriverClass());
        context.dataSource().setMajorVersion(driver.getMajorVersion());
        context.dataSource().setMinorVersion(driver.getMinorVersion());
    }

    void applyProperties(final List<PropertyRecord> properties) {
        if (context.asXADataSource().getProperties() == null) {
            context.asXADataSource().setProperties(new ArrayList<>());
        }
        context.asXADataSource().getProperties().clear();
        context.asXADataSource().getProperties().addAll(ensureUniqueProperty(properties));
    }

    void verifyConnection(final T connection, final boolean xa, final boolean existing) {
        mergeAttributes(connection);
        presenter.verifyConnection(context.dataSource(), xa, existing);
    }

    void applyConnection(final T connection) {
        mergeAttributes(connection);
    }

    @Override
    protected void finish() {
        super.finish();
        if (context.xa) {
            presenter.onCreateXADatasource(context.asXADataSource());
        } else {
            presenter.onCreateDatasource(context.dataSource());
        }
    }

    private List<PropertyRecord> ensureUniqueProperty(final List<PropertyRecord> newProperties) {
        Map<String, PropertyRecord> indexedProperties = indexProperties(newProperties);
        if (context.asXADataSource().getProperties() != null) {
            for (PropertyRecord record : context.asXADataSource().getProperties()) {
                if (indexedProperties.containsKey(record.getKey())) {
                    throw new IllegalArgumentException(
                            "Property " + record.getKey() + " has already been defined with value:" + record
                                    .getValue());
                }
            }
        }
        return newProperties;
    }

    private Map<String, PropertyRecord> indexProperties(final List<PropertyRecord> properties) {
        Map<String, PropertyRecord> indexedProperties = new HashMap<>();
        for (PropertyRecord record : properties) {
            if (!indexedProperties.containsKey(record.getKey())) {
                indexedProperties.put(record.getKey(), record);
            } else { throw new IllegalArgumentException("Duplicate key entry:" + record.getKey()); }
        }
        return indexedProperties;
    }

    private void mergeAttributes(final T connection) {
        if (!context.xa) {
            context.dataSource().setConnectionUrl(connection.getConnectionUrl());
        }
        context.dataSource().setUsername(connection.getUsername());
        context.dataSource().setPassword(connection.getPassword());
        context.dataSource().setSecurityDomain(connection.getSecurityDomain());
    }
}

