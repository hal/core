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
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.as.console.client.widgets.forms.items.NonRequiredTextBoxItem;
import org.jboss.ballroom.client.widgets.forms.ButtonItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;

/**
 * @author Harald Pehl
 */
public class ConnectionStep<T extends DataSource> extends WizardStep<Context<T>, State> {

    private final NewDatasourceWizard<T> wizard;
    private Form<T> form;

    public ConnectionStep(final NewDatasourceWizard<T> wizard, final String title) {
        super(wizard, title);
        this.wizard = wizard;
    }

    @Override
    protected Widget asWidget(final Context<T> context) {
        form = context.xa ? new Form<>(XADataSource.class) : new Form<>(DataSource.class);
        TextAreaItem connectionUrl = new TextAreaItem("connectionUrl", "Connection URL");
        NonRequiredTextBoxItem user = new NonRequiredTextBoxItem("username", "Username");
        PasswordBoxItem pass = new PasswordBoxItem("password", "Password") {{
            setRequired(false);
        }};
        NonRequiredTextBoxItem domain = new NonRequiredTextBoxItem("securityDomain", "Security Domain");

        ButtonItem testBtn = new ButtonItem("testConnection", "", Console.CONSTANTS.subsys_jca_dataSource_verify());
        testBtn.addClickHandler(clickEvent -> {
            FormValidation validation = form.validate();
            if (!validation.hasErrors()) {
                wizard.verifyConnection(form.getUpdatedEntity(), context.xa, false);
            }
        });

        if (context.xa) {
            form.setFields(user, pass, domain, testBtn);
        } else {
            form.setFields(connectionUrl, user, pass, domain, testBtn);
        }

        FlowPanel body = new FlowPanel();
        body.add(new FormHelpPanel(context.dataSourceHelp, form).asWidget());
        body.add(form.asWidget());
        return body;
    }

    @Override
    public void reset(final Context<T> context) {
        form.clearValues();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onShow(final Context<T> context) {
        form.edit((T) context.dataSource());
    }

    @Override
    protected boolean onNext(final Context<T> context) {
        FormValidation validation = form.validate();
        if (!validation.hasErrors()) {
            wizard.applyConnection(form.getUpdatedEntity());
            return true;
        }
        return false;
    }
}
