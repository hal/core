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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Harald Pehl
 */
class PropertiesStep<T extends DataSource> extends WizardStep<Context<T>, State> implements PropertyManagement {

    private final NewDatasourceWizard<T> wizard;
    private final BeanFactory beanFactory;
    private final List<PropertyRecord> properties;
    private PropertyEditor propertyEditor;
    private HTML errorMessages;

    PropertiesStep(final NewDatasourceWizard<T> wizard, BeanFactory beanFactory) {
        super(wizard, Console.CONSTANTS.subsys_jca_xadataSource_step3());
        this.wizard = wizard;
        this.beanFactory = beanFactory;
        this.properties = new ArrayList<>();
    }

    @Override
    protected Widget asWidget(Context<T> context) {
        propertyEditor = new PropertyEditor(this, true, true);

        errorMessages = new HTML(Console.CONSTANTS.subsys_jca_err_prop_required());
        errorMessages.setStyleName("error-panel");
        errorMessages.setVisible(false);

        FlowPanel body = new FlowPanel();
        body.add(errorMessages);
        body.add(propertyEditor.asWidget());
        return body;
    }

    @Override
    public void reset(final Context<T> context) {
        errorMessages.setVisible(false);
    }

    @Override
    protected void onShow(final Context<T> context) {
        properties.clear();
        if (context.asXADataSource().getProperties() != null) {
            properties.addAll(context.asXADataSource().getProperties());
            propertyEditor.setProperties("", context.asXADataSource().getProperties());
        }
    }

    @Override
    protected boolean onNext(final Context<T> context) {
        boolean hasProperties = propertyEditor.getPropertyTable().getRowCount() > 0;
        if (!hasProperties) {
            errorMessages.setVisible(true);
            return false;
        } else {
            try {
                wizard.applyProperties(properties);
                return true;
            } catch (IllegalArgumentException e) {
                errorMessages.setVisible(true);
                errorMessages.setText(e.getMessage());
                return false;
            }
        }
    }

    @Override
    public void onCreateProperty(String reference, PropertyRecord prop) {}

    @Override
    public void onDeleteProperty(String reference, PropertyRecord prop) {
        properties.remove(prop);
        propertyEditor.setProperties("", properties);
    }

    @Override
    public void onChangeProperty(String reference, PropertyRecord prop) {}

    @Override
    @SuppressWarnings("unchecked")
    public void launchNewPropertyDialoge(String reference) {
        PropertyRecord proto = beanFactory.property().as();
        proto.setKey("name");
        proto.setValue("<click to edit>");

        properties.add(proto);
        propertyEditor.setProperties("", properties);
        propertyEditor.getPropertyTable().getSelectionModel().setSelected(proto, true);

        errorMessages.setVisible(false);
    }

    @Override
    public void closePropertyDialoge() {}
}
