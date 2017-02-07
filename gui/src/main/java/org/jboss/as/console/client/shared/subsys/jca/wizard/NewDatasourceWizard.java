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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.model.ResponseWrapper;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.jca.DataSourceFinder;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceStore;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplates;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.as.console.client.v3.widgets.wizard.Wizard;

import static org.jboss.as.console.client.shared.subsys.jca.wizard.State.*;
import static org.jboss.ballroom.client.widgets.forms.FormItem.VALUE_SEMANTICS.UNDEFINED;

/**
 * @author Harald Pehl
 */
public class NewDatasourceWizard<T extends DataSource> extends Wizard<Context<T>, State> {

    static class ModifyAfterTestCallback implements AsyncCallback<ResponseWrapper<Boolean>> {

        private final String dataSource;

        ModifyAfterTestCallback(final String dataSource) {this.dataSource = dataSource;}

        @Override
        public void onFailure(final Throwable throwable) {
            Log.error(
                    "Unable to modify datasource " + dataSource + " after it was created by 'Test connection' button: " + throwable
                            .getMessage());
        }

        @Override
        public void onSuccess(final ResponseWrapper<Boolean> booleanResponseWrapper) {
            if (!booleanResponseWrapper.getUnderlying()) {
                Log.error(
                        "Unable to modify datasource " + dataSource + " after it was created by 'Test connection' button");
            }
        }
    }


    private final DataSourceFinder presenter;
    private final DataSourceStore dataSourceStore;
    // the Wizard.open method calls addCloseHandler that calls onClose()
    // the close handler is called when the modal window is closed,
    // either when the user clicked at "finish" button or cancels the operation
    // if the modal window is closed or user press "esc" or click at x to close the window

    // then, if the user tested the datasource, it is added to the profile
    // and as the cancel() is called, we need to ensure to not remove the datasource.
    private boolean saveDatasource;

    public NewDatasourceWizard(final DataSourceFinder presenter,
            final DataSourceStore dataSourceStore,
            final BootstrapContext bootstrapContext,
            final BeanFactory beanFactory,
            final DataSourceTemplates templates,
            final List<DataSource> existingDataSources,
            final List<JDBCDriver> drivers,
            final boolean xa) {
        super(xa ? "new_xa_datasource" : "new_datasource",
                new Context<>(beanFactory, bootstrapContext.isStandalone(), xa));
        this.presenter = presenter;
        this.dataSourceStore = dataSourceStore;

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
        addStep(TEST, new TestConnectionStep<>(this));
        addStep(SUMMARY, new SummaryStep<>(this));
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
           case TEST:
               previous = CONNECTION;
               break;
            case SUMMARY:
                previous = TEST;
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
                next = TEST;
                break;
           case TEST:
               next = SUMMARY;
               break;
            case SUMMARY:
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

        // Modifying basic attributes like name and/or JNDI bindings after the DS was created by 'Test connection' is
        // not supported!
    }

    void applyDriver(JDBCDriver driver) {
        context.dataSource().setDriverName(driver.getName());
        if (context.xa) {
            context.dataSource().setDriverClass(driver.getDriverClass());
        }
        context.dataSource().setDriverClass(driver.getDriverClass());
        context.dataSource().setMajorVersion(driver.getMajorVersion());
        context.dataSource().setMinorVersion(driver.getMinorVersion());

        if (context.dataSourceCreatedByTest) {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder();
            recordChange(builder, "driverName", driver.getName());
            recordChange(builder, "majorVersion", driver.getMajorVersion());
            recordChange(builder, "minorVersion", driver.getMinorVersion());
            modifyAfterTest(context.dataSource().getName(), builder.build());
        }
    }

    void applyProperties(final List<PropertyRecord> properties) {
        if (context.asXADataSource().getProperties() == null) {
            context.asXADataSource().setProperties(new ArrayList<>());
        }
        context.asXADataSource().getProperties().clear();
        context.asXADataSource().getProperties().addAll(ensureUniqueProperty(properties));

        // Modifying properties after the DS was created by 'Test connection' is not supported!
        // It's considered as an edge case which does not justify the effort to calculate new, modified or removed
        // properties and build the related DMR operations.
    }

    void applyConnection(final T connection) {
        mergeAttributes(connection);

        if (context.dataSourceCreatedByTest) {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder();
            if (!context.xa) {
                recordChange(builder, "connectionUrl", connection.getConnectionUrl());
            }
            recordChange(builder, "username", connection.getUsername());
            recordChange(builder, "password", connection.getPassword());
            if (connection.getSecurityDomain() != null) {
                recordChange(builder, "securityDomain", connection.getSecurityDomain());
            }
            modifyAfterTest(context.dataSource().getName(), builder.build());
        }
    }

    void verifyConnection(Scheduler.ScheduledCommand onCreated) {
        presenter.verifyConnection(context.dataSource(), context.xa, context.dataSourceCreatedByTest, onCreated);
    }

    @Override
    protected void finish() {
        // it is important to set it before the super.finish() call as it will call cancel() before
        // finishing super.finish() call.
        // this way the variable controls the rule to not remove the datasource in the cancel()
        saveDatasource = true;
        super.finish();
        if (context.xa) {
            presenter.onCreateXADatasource(context.asXADataSource(), context.dataSourceCreatedByTest);
        } else {
            presenter.onCreateDatasource(context.dataSource(), context.dataSourceCreatedByTest);
        }
    }

    @Override
    protected void cancel() {
        super.cancel();
        if (!saveDatasource && context.dataSourceCreatedByTest) {
            saveDatasource = false;
            // cleanup
            AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(final Throwable throwable) {
                   Console.error(Console.CONSTANTS.common_error_unknownError(), throwable.getMessage());
                }

                @Override
                public void onSuccess(final Boolean aBoolean) {

                }
            };
            if (context.xa) {
                dataSourceStore.deleteXADataSource(context.asXADataSource(), callback);
            } else {
                dataSourceStore.deleteDataSource(context.dataSource(), callback);
            }
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

    private void recordChange(final ImmutableMap.Builder<String, Object> builder, final String name,
            final Object value) {
        if (value instanceof String) {
            if (Strings.emptyToNull((String) value) == null) {
                builder.put(name, UNDEFINED);
            } else {
                builder.put(name, value);
            }
        } else {
            if (value == null) {
                builder.put(name, UNDEFINED);
            } else {
                builder.put(name, value);
            }
        }
    }

    private void modifyAfterTest(final String dataSource, final Map<String, Object> changedValues) {
        if (!changedValues.isEmpty()) {
            ModifyAfterTestCallback callback = new ModifyAfterTestCallback(dataSource);
            if (context.xa) {
                dataSourceStore.updateDataSource(dataSource, changedValues, callback);
            } else {
                dataSourceStore.updateDataSource(dataSource, changedValues, callback);
            }
        }
    }
}

