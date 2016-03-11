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

package org.jboss.as.console.client.domain.hosts.general;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.DisposableViewImpl;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyRecord;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 5/17/11
 */
public class HostPropertiesView extends DisposableViewImpl implements HostPropertiesPresenter.MyView{

    private HostPropertiesPresenter presenter;
    private PropertyEditor propertyEditor;

    @Override
    public Widget createWidget() {

        SimpleLayout layout = new SimpleLayout()
                .setTitle("Host Properties")
                .setPlain(true)
                .setHeadline("Host Property Declarations")
                .setDescription(Console.CONSTANTS.host_properties_desc());

        propertyEditor = new PropertyEditor(presenter, false);
        layout.addContent("", propertyEditor.asWidget());

        return layout.build();

    }

    @Override
    public void setPresenter(HostPropertiesPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setProperties(List<PropertyRecord> properties) {
        propertyEditor.setProperties("", properties);
    }
}
