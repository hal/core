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

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.as.console.client.widgets.ContentDescription;

/**
 * @author Harald Pehl
 */
public class TestConnectionStep<T extends DataSource> extends WizardStep<Context<T>, State> {

    private final static UIMessages MESSAGES = Console.MESSAGES;

    private final NewDatasourceWizard<T> wizard;

    protected TestConnectionStep(final NewDatasourceWizard<T> wizard) {
        super(wizard, "Test Connection");
        this.wizard = wizard;
    }

    @Override
    protected Widget asWidget(final Context<T> context) {
        VerticalPanel body = new VerticalPanel();

        body.add(context.standalone
                ? new ContentDescription(MESSAGES.testConnectionStandaloneDescription())
                : new ContentDescription(MESSAGES.testConnectionDomainDescription()));

        Button textConnection = new Button(Console.CONSTANTS.subsys_jca_dataSource_verify());
        textConnection.addClickHandler(clickEvent -> wizard.verifyConnection(
                () -> context.dataSourceCreatedByTest = true)); // nested callbacks w/ lambdas - need to get used to that
        body.add(textConnection);

        return body;
    }
}
