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

import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplate;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Harald Pehl
 */
class Context<T extends DataSource> {

    private final BeanFactory beanFactory;
    private DataSource dataSource;
    private XADataSource xaDataSource;
    final boolean standalone;
    final boolean xa;
    final FormHelpPanel.AddressCallback helpCallback;
    DataSourceTemplate<T> selectedTemplate;
    JDBCDriver driver;

    Context(BeanFactory beanFactory, boolean standalone, boolean xa) {
        this.beanFactory = beanFactory;
        this.standalone = standalone;
        this.xa = xa;
        this.helpCallback = () -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "datasources");
            address.add(xa ? "xa-data-source" : "data-source", "*");
            return address;
        };
    }

    void start() {
        if (selectedTemplate != null) {
            if (xa) {
                xaDataSource = (XADataSource) selectedTemplate.getDataSource();
            } else {
                dataSource = selectedTemplate.getDataSource();
            }
            driver = selectedTemplate.getDriver();
        } else {
            dataSource = xa ? null : beanFactory.dataSource().as();
            xaDataSource = xa ? beanFactory.xaDataSource().as() : null;
        }
    }

    DataSource dataSource() {
        return xa ? xaDataSource : dataSource;
    }

    XADataSource asXADataSource() {
        if (!xa) {
            throw new IllegalStateException("Not an XA data source!");
        }
        return xaDataSource;
    }
}
