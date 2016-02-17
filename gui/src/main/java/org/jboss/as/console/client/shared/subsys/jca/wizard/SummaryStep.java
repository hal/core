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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.forms.PropertyListItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.console.client.Console.MESSAGES;

/**
 * @author Harald Pehl
 */
public class SummaryStep<T extends DataSource> extends WizardStep<Context<T>, State> {

    private Form<T> form;
    private PropertyListItem xaProperties;

    public SummaryStep(final NewDatasourceWizard<T> wizard) {
        super(wizard, Console.CONSTANTS.subsys_jca_dataSource_summary());
    }

    @Override
    protected Widget asWidget(final Context<T> context) {
        FlowPanel body = new FlowPanel();
        form = context.xa ? new Form<>(XADataSource.class) : new Form<>(DataSource.class);
        TextBoxItem nameItem = new TextBoxItem("name", "Name");
        TextBoxItem jndiItem = new TextBoxItem("jndiName", "JNDI Name");
        TextBoxItem connectionUrl = new TextBoxItem("connectionUrl", "Connection URL");
        xaProperties = new PropertyListItem("_do_not_populate_", "Properties");
        TextBoxItem user = new TextBoxItem("username", "Username");
        PasswordBoxItem pass = new PasswordBoxItem("password", "Password");
        if (context.xa) {
            form.setFields(nameItem, jndiItem, xaProperties, user, pass);
        } else {
            form.setFields(nameItem, jndiItem, connectionUrl, user, pass);
        }
        form.setEnabled(false);

        body.add(new ContentDescription(MESSAGES.datasourceSummaryDescription()));
        body.add(form);
        return body;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onShow(final Context<T> context) {
        if (context.xa) {
            form.edit((T) context.asXADataSource());
            Map<String, String> map = new HashMap<>();
            List<PropertyRecord> properties = context.asXADataSource().getProperties();
            for (PropertyRecord property : properties) {
                map.put(property.getKey(), property.getValue());
            }
            xaProperties.setValue(map);
        } else {
            form.edit((T) context.dataSource());
        }
    }
}
