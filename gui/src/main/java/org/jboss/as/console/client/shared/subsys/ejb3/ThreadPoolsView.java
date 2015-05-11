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
package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.ejb3.model.EJB3ThreadPool;
import org.jboss.as.console.client.shared.subsys.ejb3.threads.UnboundedQueueThreadPoolView;
import org.jboss.as.console.client.shared.viewframework.EntityToDmrBridge;
import org.jboss.as.console.client.shared.viewframework.EntityToDmrBridgeImpl;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.FormMetaData;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;
import org.jboss.dmr.client.dispatch.DispatchAsync;

import java.util.Iterator;
import java.util.List;

/**
 * @author David Bosschaert
 */
public class ThreadPoolsView extends UnboundedQueueThreadPoolView<EJB3ThreadPool> {
    private final EntityToDmrBridgeImpl<EJB3ThreadPool> bridge;
    private final FormMetaData filteredFormMetaData;
    private EJB3Presenter presenter;

    public ThreadPoolsView(ApplicationMetaData propertyMetaData, DispatchAsync dispatcher) {
        super(EJB3ThreadPool.class, propertyMetaData, dispatcher);
        bridge = new EntityToDmrBridgeImpl<EJB3ThreadPool>(propertyMetaData, EJB3ThreadPool.class, this, dispatcher);

        // HAL-477 Exclude attribute "thread-factory"
        BeanMetaData beanMetaData = propertyMetaData.getBeanMetaData(EJB3ThreadPool.class);
        List<PropertyBinding> properties = beanMetaData.getProperties();
        for (Iterator<PropertyBinding> iterator = properties.iterator(); iterator.hasNext(); ) {
            PropertyBinding property = iterator.next();
            if ("thread-factory".equals(property.getDetypedName())) {
                iterator.remove();
            }
        }
        filteredFormMetaData = new FormMetaData(EJB3ThreadPool.class, properties);
    }

    @Override
    public Widget createWidget() {
        setDescription(Console.CONSTANTS.subsys_ejb3_threadpools_desc());

        return createEmbeddableWidget();
    }

    @Override
    public EntityToDmrBridge<EJB3ThreadPool> getEntityBridge() {
        return bridge;
    }

    @Override
    protected String getEntityDisplayName() {
        return Console.CONSTANTS.subsys_ejb3_threadPools();
    }

    @Override
    public void refresh() {
        super.refresh();
        presenter.propagateThreadPoolNames(bridge.getEntityList());
    }

    public void setPresenter(EJB3Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    protected FormMetaData getFormMetaData() {
        // HAL-477 Exclude attribute "thread-factory"
        return filteredFormMetaData;
    }
}
