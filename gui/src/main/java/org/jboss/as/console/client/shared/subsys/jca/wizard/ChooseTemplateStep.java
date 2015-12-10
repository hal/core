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

import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplate;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplates;
import org.jboss.as.console.client.v3.widgets.wizard.Wizard;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;

/**
 * @author Harald Pehl
 */
class ChooseTemplateStep<T extends DataSource> extends WizardStep<Context<T>, State> {

    private final DataSourceTemplates templates;
    private DataSourceTemplate<T> selectedTemplate;

    ChooseTemplateStep(final Wizard<Context<T>, State> wizard, final DataSourceTemplates templates) {
        super(wizard, Console.CONSTANTS.subsys_jca_dataSource_choose_template());
        this.templates = templates;
    }

    @Override
    protected Widget asWidget(final Context<T> context) {
        VerticalPanel body = new VerticalPanel();

        RadioButton customButton = new RadioButton("template",
                Console.CONSTANTS.subsys_jca_dataSource_custom_template());
        customButton.getElement().setId("custom");
        customButton.setStyleName("choose_template");
        customButton.setValue(true);
        customButton.addClickHandler(event -> {
            RadioButton button = (RadioButton) event.getSource();
            selectedTemplate = templates.getTemplate(button.getElement().getId());
        });
        customButton.setFocus(true);
        body.add(customButton);

        for (DataSourceTemplate<? extends DataSource> template : templates) {
            if (template.isXA() != context.xa) {
                continue;
            }
            RadioButton radioButton = new RadioButton("template", template.toString());
            radioButton.getElement().setId(template.getId());
            radioButton.setStyleName("choose_template");
            radioButton.addClickHandler(event -> {
                RadioButton button = (RadioButton) event.getSource();
                selectedTemplate = templates.getTemplate(button.getElement().getId());
            });
            body.add(radioButton);
        }

        return body;
    }

    @Override
    protected boolean onNext(final Context<T> context) {
        context.selectedTemplate = selectedTemplate;
        context.start();
        return true;
    }
}
