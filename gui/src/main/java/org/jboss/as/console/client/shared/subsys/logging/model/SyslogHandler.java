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
package org.jboss.as.console.client.shared.subsys.logging.model;

import org.jboss.as.console.client.shared.viewframework.NamedEntity;
import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;
import org.jboss.as.console.client.widgets.forms.FormItem;

/**
 * Console Handler Entity
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2011 Red Hat Inc.
 */
@Address("/subsystem=logging/syslog-handler={0}")
public interface SyslogHandler extends NamedEntity, HasLevel {

    @Override
    @Binding(detypedName="name", key=true)
    @FormItem(defaultValue="",
            localLabel="common_label_name",
            required=true,
            formItemTypeForEdit="TEXT",
            formItemTypeForAdd="TEXT_BOX")
    public String getName();
    @Override
    public void setName(String name);

    @Override
    @Binding(detypedName="level")
    @FormItem(defaultValue="INFO",
            localLabel="subsys_logging_logLevel",
            required=true,
            formItemTypeForEdit="COMBO_BOX",
            formItemTypeForAdd="COMBO_BOX")
    public String getLevel();
    @Override
    public void setLevel(String logLevel);

    @Binding(detypedName="port")
    @FormItem(
            label="Port",
            defaultValue = "514",
            required=true,
            formItemTypeForEdit="NUMBER_BOX",
            formItemTypeForAdd="NUMBER_BOX")
    public Integer getPort();
    public void setPort(Integer port);

    @Binding(detypedName="app-name")
    @FormItem(defaultValue = "java",
            label="App Name",
            required=false,
            formItemTypeForEdit="TEXT_BOX",
            formItemTypeForAdd="TEXT_BOX")
    public String getAppName();
    public void setAppName(String appName);

    @Binding(detypedName="facility")
    @FormItem(
            label="Facility",
            required=true,
            defaultValue = "user-level",
            formItemTypeForEdit="TEXT_BOX",
            formItemTypeForAdd="TEXT_BOX")
    public String getFacility();
    public void setFacility(String facility);

    @Binding(detypedName="server-address")
    @FormItem(required=true,
            label="Server",
            formItemTypeForEdit="TEXT_BOX",
            formItemTypeForAdd="TEXT_BOX")
    public String getServerAddress();
    public void setServerAddress(String address);

    @Binding(detypedName="hostname")
    @FormItem(required=true,
            label="Host From",
            formItemTypeForEdit="TEXT_BOX",
            formItemTypeForAdd="TEXT_BOX")
    public String getHostname();
    public void setHostname(String name);

    @Binding(detypedName="syslog-format")
    @FormItem(required=false,
            label="Format",
            formItemTypeForEdit="FREE_FORM_TEXT_BOX",
            formItemTypeForAdd="FREE_FORM_TEXT_BOX")
    public String getFormat();
    public void setFormat(String format);

}
