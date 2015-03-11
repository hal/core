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
package org.jboss.as.console.client.v3.widgets;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

/**
 * @author Harald Pehl
 */
public class MapAttributeAddPropertyDialog extends AddPropertyDialog {

    private final Form<PropertyRecord> form;

    public MapAttributeAddPropertyDialog(final PropertyManager propertyManager) {
        super(Console.CONSTANTS.common_label_add());

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("window-content");

        form = new Form<>(PropertyRecord.class);
        TextBoxItem nameItem = new TextBoxItem("key", "Name") {
            @Override
            public void setFiltered(boolean filtered) {
                // cannot be filtered (workaround)
            }
        };
        TextBoxItem valueItem = new TextBoxItem("value", "Value") {
            @Override
            public void setFiltered(boolean filtered) {
                // cannot be filtered (workaround)
            }
        };
        form.setFields(nameItem, valueItem);

        DialogueOptions options = new DialogueOptions(
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        PropertyRecord propertyRecord = form.getUpdatedEntity();
                        if (!form.validate().hasErrors()) {
                            ModelNode node = new ModelNode();
                            node.set(propertyRecord.getValue());
                            Property property = new Property(propertyRecord.getKey(), node);
                            propertyManager.onAdd(property, MapAttributeAddPropertyDialog.this);
                        }
                    }
                },
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        hide();
                    }
                }
        );

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width window-content");
        layout.add(new ContentDescription(Console.CONSTANTS.common_label_addProperty()));
        Widget formWidget = form.asWidget();
        layout.add(formWidget);

        ScrollPanel scroll = new ScrollPanel(layout);
        LayoutPanel content = new LayoutPanel();
        content.addStyleName("fill-layout");
        content.add(scroll);
        content.add(options);
        content.getElement().setAttribute("style", "margin-bottom:10px");
        content.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 92, Style.Unit.PCT);
        content.setWidgetBottomHeight(options, 0, Style.Unit.PX, 35, Style.Unit.PX);

        setWidget(new TrappedFocusPanel(content));
    }

    @Override
    public void clearValues() {
        form.clearValues();
    }
}
