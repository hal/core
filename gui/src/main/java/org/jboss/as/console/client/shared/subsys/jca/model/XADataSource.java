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

package org.jboss.as.console.client.shared.subsys.jca.model;

import java.util.List;

import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;

/**
 * @author Heiko Braun
 * @date 5/4/11
 */
@Address("/subsystem=datasources/xa-data-source={0}")
public interface XADataSource extends DataSource {

    @Binding(detypedName = "xa-datasource-class")
    String getDataSourceClass();
    void setDataSourceClass(String dadaSourceClass);

    @Binding(skip = true)
    List<PropertyRecord> getProperties();
    void setProperties(List<PropertyRecord> props);

    @Binding(skip=true) // does not exist on XA datasources
    boolean isJta();
    void setJta(boolean b);

    @Binding(detypedName = "pad-xid")
    Boolean isPadXid();
    void setPadXid(Boolean b);

    @Binding(detypedName = "wrap-xa-resource")
    Boolean isWrapXaResource();
    void setWrapXaResource(Boolean b);

    @Binding(detypedName = "same-rm-override")
    Boolean isEnableRMOverride();
    void setEnableRMOverride(Boolean b);

    @Binding(detypedName = "interleaving")
    Boolean isEnableInterleave();
    void setEnableInterleave(Boolean b);

    @Binding(detypedName = "xa-resource-timeout")
    Integer getXaResourceTimeout();
    void setXaResourceTimeout(Integer i);

    @Binding(detypedName = "no-tx-separate-pool")
    Boolean isNoTxSeparatePool();
    void setNoTxSeparatePool(Boolean b);

    @Binding(detypedName = "same-rm-override")
    Boolean isSameRmOverride();
    void setSameRmOverride(Boolean b);

    @Binding(detypedName = "recovery-plugin-class-name")
    String getRecoveryPluginClassName();
    void setRecoveryPluginClassName(String s);

    @Binding(detypedName = "spy")
    Boolean isSpy();
    void setSpy(Boolean b);

}
