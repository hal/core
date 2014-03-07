/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.topology.HostInfo;
import org.jboss.as.console.client.domain.topology.TopologyFunctions;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.model.ResponseWrapper;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceStore;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

/**
 * @author Harald Pehl
 */
public class VerifyConnectionOp {

    public static class VerifyResult {

        private final boolean successful;
        private final String message;
        private final String details;

        public VerifyResult(Throwable failure) {
            this(false, Console.CONSTANTS.verify_datasource_internal_error(), failure.getMessage());
        }

        public VerifyResult(final boolean successful, final String message) {
            this(successful, message, null);
        }

        public VerifyResult(final boolean successful, final String message, final String details) {
            this.successful = successful;
            this.message = message;
            this.details = details;
        }

        public boolean wasSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }

        public boolean hasDetails() {
            return details != null;
        }

        public String getDetails() {
            return details;
        }
    }


    private class VerifyStandaloneFunction implements Function<FunctionContext> {

        private final DataSource dataSource;
        private final boolean xa;
        private final boolean existing;

        VerifyStandaloneFunction(final DataSource dataSource, final boolean xa, final boolean existing) {
            this.dataSource = dataSource;
            this.xa = xa;
            this.existing = existing;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            dataSourceStore.verifyConnection(dataSource.getName(), xa, new AsyncCallback<ResponseWrapper<Boolean>>() {
                @Override
                public void onFailure(final Throwable caught) {
                    control.getContext().push(new VerifyResult(caught));
                    if (existing) {
                        control.abort();
                    } else {
                        control.proceed();
                    }
                }

                @Override
                public void onSuccess(final ResponseWrapper<Boolean> result) {
                    if (result.getUnderlying()) {
                        control.getContext()
                                .push(new VerifyResult(true, Console.MESSAGES.verify_datasource_successful_message(
                                        dataSource.getName())));
                    } else {
                        control.getContext()
                                .push(new VerifyResult(false, Console.MESSAGES.verify_datasource_failed_message(
                                        dataSource.getName()), result.getResponse().toString()));
                    }
                    control.proceed();
                }
            });
        }
    }


    private class VerifyDomainFunction implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final DataSource dataSource;
        private final String dsAddress;
        private final boolean existing;

        public VerifyDomainFunction(final DispatchAsync dispatcher, final DataSource dataSource, final boolean xa,
                final boolean existing) {
            this.dispatcher = dispatcher;
            this.dataSource = dataSource;
            this.dsAddress = xa ? "xa-data-source" : "data-source";
            this.existing = existing;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            final ModelNode node = new ModelNode();
            node.get(ADDRESS).setEmptyList();
            node.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            final List<HostInfo> hosts = control.getContext().get(TopologyFunctions.HOSTS_KEY);
            for (HostInfo hostInfo : hosts) {
                for (ServerInstance serverInstance : hostInfo.getServerInstances()) {
                    if (serverInstance.isRunning()) {
                        ModelNode verifyOp = new ModelNode();
                        verifyOp.get(OP).set("test-connection-in-pool");
                        verifyOp.get(ADDRESS).add("host", hostInfo.getName());
                        verifyOp.get(ADDRESS).add("server", serverInstance.getName());
                        verifyOp.get(ADDRESS).add("subsystem", "datasources");
                        verifyOp.get(ADDRESS).add(dsAddress, dataSource.getName());
                        steps.add(verifyOp);
                    }
                }
            }

            node.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control) {
                @Override
                public void onFailure(final Throwable caught) {
                    control.getContext().push(new VerifyResult(caught));
                    if (existing) { control.abort(); } else { control.proceed(); }
                }

                @Override
                public void onSuccess(final DMRResponse response) {
                    ModelNode result = response.get();
                    if (!result.hasDefined(OUTCOME) || result.isFailure()) {
                        control.getContext()
                                .push(new VerifyResult(false, Console.MESSAGES.verify_datasource_failed_message(
                                        dataSource.getName()), result.toString()));
                        if (existing) {
                            control.abort();
                        } else {
                            control.proceed();
                        }
                    } else {
                        control.getContext()
                                .push(new VerifyResult(true, Console.MESSAGES.verify_datasource_successful_message(
                                        dataSource.getName())));
                        control.proceed();
                    }
                }
            });
        }
    }


    /**
     * Remove server instances and hosts which do not belong to the selected profile. Those server instances / hosts
     * must not be part of the verify operation.
     */
    private class FilterCurrentProfile implements Function<FunctionContext> {

        @Override
        public void execute(final Control<FunctionContext> control) {
            List<HostInfo> hosts = control.getContext().get(TopologyFunctions.HOSTS_KEY);
            for (HostInfo host : hosts) {
                for (Iterator<ServerInstance> iterator = host.getServerInstances().iterator(); iterator.hasNext(); ) {
                    ServerInstance serverInstance = iterator.next();
                    if (!profile.equals(serverInstance.getProfile())) {
                        iterator.remove();
                    }
                }
            }
            for (Iterator<HostInfo> iterator = hosts.iterator(); iterator.hasNext(); ) {
                HostInfo host = iterator.next();
                if (host.getServerInstances().isEmpty()) {
                    iterator.remove();
                }
            }
            control.proceed();
        }
    }


    private class VerifyRunningServer implements Function<FunctionContext> {

        @Override
        public void execute(final Control<FunctionContext> control) {
            boolean runningServer = false;
            final List<HostInfo> hosts = control.getContext().get(TopologyFunctions.HOSTS_KEY);
            for (Iterator<HostInfo> hIterator = hosts.iterator(); hIterator.hasNext() && !runningServer; ) {
                HostInfo host = hIterator.next();
                List<ServerInstance> serverInstances = host.getServerInstances();
                for (Iterator<ServerInstance> siIterator = serverInstances.iterator();
                        siIterator.hasNext() && !runningServer; ) {
                    ServerInstance serverInstance = siIterator.next();
                    runningServer = serverInstance.isRunning();
                }
            }
            if (runningServer) {
                control.proceed();
            } else {
                control.getContext()
                        .push(new VerifyResult(false, Console.CONSTANTS.verify_datasource_no_running_servers()));
                control.abort();
            }
        }
    }


    private class CreateFunction implements Function<FunctionContext> {

        private final DataSource dataSource;
        private final boolean xa;

        public CreateFunction(final DataSource dataSource, final boolean xa) {
            this.dataSource = dataSource;
            this.xa = xa;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            if ("".equals(dataSource.getUsername())) { dataSource.setUsername(null); }
            if ("".equals(dataSource.getPassword())) { dataSource.setPassword(null); }
            if ("".equals(dataSource.getSecurityDomain())) { dataSource.setSecurityDomain(null); }
            dataSource.setEnabled(false); // will be enabled in next function

            final AsyncCallback<ResponseWrapper<Boolean>> callback = new AsyncCallback<ResponseWrapper<Boolean>>() {
                @Override
                public void onFailure(final Throwable caught) {
                    control.getContext().push(new VerifyResult(caught));
                    control.abort();
                }

                @Override
                public void onSuccess(ResponseWrapper<Boolean> result) {
                    if (result.getUnderlying()) {
                        control.proceed();
                    } else {
                        control.getContext()
                                .push(new VerifyResult(false, Console.CONSTANTS.verify_datasource_dependent_error(),
                                        Console.MESSAGES.addingFailed("Datasource " + dataSource.getName())));
                        control.abort();
                    }
                }
            };
            if (xa) {
                dataSourceStore.createXADataSource((XADataSource) dataSource, callback);
            } else {
                dataSourceStore.createDataSource(dataSource, callback);
            }
        }
    }


    private class EnableFunction implements Function<FunctionContext> {

        private final DataSource dataSource;
        private final boolean xa;
        private final boolean existing;

        private EnableFunction(final DataSource dataSource, boolean xa, boolean existing) {
            this.dataSource = dataSource;
            this.xa = xa;
            this.existing = existing;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            final AsyncCallback<ResponseWrapper<Boolean>> callback = new AsyncCallback<ResponseWrapper<Boolean>>() {
                @Override
                public void onFailure(final Throwable caught) {
                    control.getContext().push(new VerifyResult(caught));
                    if (existing) {
                        control.abort();
                    } else {
                        control.proceed();
                    }
                }

                @Override
                public void onSuccess(ResponseWrapper<Boolean> result) {
                    if (result.getUnderlying()) {
                        control.proceed();
                    } else {
                        control.getContext()
                                .push(new VerifyResult(false, Console.CONSTANTS.verify_datasource_dependent_error(),
                                        Console.MESSAGES.modificationFailed("Datasource " + dataSource.getName())));
                        if (existing) {
                            control.abort();
                        } else {
                            control.proceed();
                        }
                    }
                }
            };
            if (xa) {
                dataSourceStore.enableXADataSource((XADataSource) dataSource, true, callback);
            } else {
                dataSourceStore.enableDataSource(dataSource, true, callback);
            }
        }
    }


    private class RemoveFunction implements Function<FunctionContext> {

        private final DataSource dataSource;
        private final boolean xa;

        public RemoveFunction(final DataSource dataSource, final boolean xa) {
            this.dataSource = dataSource;
            this.xa = xa;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(final Throwable caught) {
                    control.getContext().push(new VerifyResult(caught));
                    control.abort();
                }

                @Override
                public void onSuccess(final Boolean result) {
                    if (result) {
                        control.proceed();
                    } else {
                        control.getContext()
                                .push(new VerifyResult(false, Console.CONSTANTS.verify_datasource_dependent_error(),
                                        Console.MESSAGES.deletionFailed("Datasource " + dataSource.getName())));
                    }
                }
            };
            if (xa) {
                dataSourceStore.deleteXADataSource((XADataSource) dataSource, callback);
            } else {
                dataSourceStore.deleteDataSource(dataSource, callback);
            }
        }
    }


    private final DataSourceStore dataSourceStore;
    private final DispatchAsync dispatcher;
    private final BeanFactory beanFactory;
    private final String profile;
    private final boolean standalone;

    public VerifyConnectionOp(final DataSourceStore dataSourceStore, final DispatchAsync dispatcher,
            final BeanFactory beanFactory, final String profile) {
        this.dataSourceStore = dataSourceStore;
        this.dispatcher = dispatcher;
        this.beanFactory = beanFactory;
        this.profile = profile;
        this.standalone = Console.getBootstrapContext().isStandalone();
    }

    @SuppressWarnings("unchecked")
    public void execute(final DataSource dataSource, final boolean xa, final boolean existing,
            final AsyncCallback<VerifyResult> callback) {

        if (existing && !dataSource.isEnabled()) {
            // Verifying makes only sense for enabled datasources!
            callback.onSuccess(new VerifyResult(false, Console.CONSTANTS.verify_datasource_disabled()));
        } else {
            // Setup a list of functions depending on the operation mode and existence of the datasource
            List<Function<FunctionContext>> functions = new LinkedList<Function<FunctionContext>>();
            if (standalone) {
                if (existing) {
                    // that's the easiest case - just verify the existing datasource
                    functions.add(new VerifyStandaloneFunction(dataSource, xa, true));
                } else {
                    // create - verify - remove
                    functions.add(new CreateFunction(dataSource, xa));
                    functions.add(new EnableFunction(dataSource, xa, false));
                    functions.add(new VerifyStandaloneFunction(dataSource, xa, false));
                    functions.add(new RemoveFunction(dataSource, xa));
                }
            } else {
                // in domain mode verifying a datasource requires a running server
                functions.add(new TopologyFunctions.HostsAndGroups(dispatcher));
                functions.add(new TopologyFunctions.ServerConfigs(dispatcher, beanFactory));
                functions.add(new FilterCurrentProfile());
                functions.add(new VerifyRunningServer());
                if (existing) {
                    functions.add(new VerifyDomainFunction(dispatcher, dataSource, xa, true));
                } else {
                    // create - verify - remove
                    functions.add(new CreateFunction(dataSource, xa));
                    functions.add(new EnableFunction(dataSource, xa, false));
                    functions.add(new VerifyDomainFunction(dispatcher, dataSource, xa, false));
                    functions.add(new RemoveFunction(dataSource, xa));
                }
            }

            Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
                @Override
                public void onFailure(final FunctionContext context) {
                    callback.onSuccess(context.<VerifyResult>pop());
                }

                @Override
                public void onSuccess(final FunctionContext context) {
                    callback.onSuccess(context.<VerifyResult>pop());
                }
            };

            new Async<FunctionContext>(Footer.PROGRESS_ELEMENT)
                    .waterfall(new FunctionContext(), outcome, functions.toArray(new Function[functions.size()]));
        }
    }
}
