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
package org.jboss.as.console.client.shared.runtime.env;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableView;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 * @date 15/10/12
 */
public class EnvironmentPresenter extends CircuitPresenter<EnvironmentPresenter.MyView, EnvironmentPresenter.MyProxy> {

    @ProxyCodeSplit
    @NameToken(NameTokens.EnvironmentPresenter)
    @SearchIndex(keywords = {"runtime", "environment", "jvm"})
    @RequiredResources(resources = {"/{selected.host}/{selected.server}/core-service=platform-mbean/type=runtime"})
    public interface MyProxy extends ProxyPlace<EnvironmentPresenter> {
    }


    public interface MyView extends SuspendableView {
        void setPresenter(EnvironmentPresenter environmentPresenter);
        void setEnvironment(List<PropertyRecord> environment);
        void clearEnvironment();
    }


    private final DispatchAsync dispatcher;
    private final BeanFactory factory;
    private final RevealStrategy revealStrategy;

    @Inject
    public EnvironmentPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
                                final DispatchAsync dispatcher, Dispatcher circuit, final BeanFactory factory,
                                final RevealStrategy revealStrategy) {
        super(eventBus, view, proxy, circuit);
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.revealStrategy = revealStrategy;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(Console.MODULES.getServerStore());
    }

    @Override
    protected void onAction(Action action) {
        refresh();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        refresh();
    }

    public void refresh() {
        getView().clearEnvironment();

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("core-service", "platform-mbean");
        operation.get(ADDRESS).add("type", "runtime");
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("system-properties");

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to read environment properties: " + response.getFailureDescription());
                } else {

                    List<Property> properties = response.get(RESULT).asPropertyList();
                    List<PropertyRecord> environment = new ArrayList<PropertyRecord>(properties.size());
                    for (Property property : properties) {
                        PropertyRecord model = factory.property().as();
                        model.setKey(property.getName());
                        model.setValue(property.getValue().asString());
                        environment.add(model);
                    }
                    getView().setEnvironment(environment);
                }
            }
        });
    }
}
