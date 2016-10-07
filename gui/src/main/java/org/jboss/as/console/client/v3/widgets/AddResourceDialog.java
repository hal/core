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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder.FormItemFactory;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.dmr.client.ModelNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Harald Pehl
 */
public class AddResourceDialog implements IsWidget {

    private String[] includes = null;

    public interface Callback {
        void onAdd(ModelNode payload);

        void onCancel();
    }

    private SecurityContext securityContext;
    private ResourceDescription resourceDescription;
    private Callback callback;
    private Map<String, FormItemFactory> factories = new LinkedHashMap<>();
    private ModelNodeFormBuilder.FormAssets assets;

    public AddResourceDialog(SecurityContext securityContext, ResourceDescription resourceDescription, Callback callback) {
        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;
        this.callback = callback;
    }

    public AddResourceDialog(ModelNodeFormBuilder.FormAssets assets, ResourceDescription resourceDescription,
            Callback callback) {
        this.assets = assets;
        this.resourceDescription = resourceDescription;
        this.callback = callback;
    }

    public AddResourceDialog addFactory(String attribute, FormItemFactory factory) {
        if (assets != null) {
            throw new IllegalStateException("Form already initialized");
        }
        factories.put(attribute, factory);
        return this;
    }

    public AddResourceDialog include(String... attributes) {
        if (assets != null) {
            throw new IllegalStateException("Form already initialized");
        }
        this.includes = attributes;
        return this;
    }

    @Override
    public Widget asWidget() {
        if (assets == null) {
            ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                    .setCreateMode(true)
                    .setResourceDescription(resourceDescription)
                    .setRequiredOnly(true)
                    .setSecurityContext(securityContext);

            for (Map.Entry<String, FormItemFactory> entry : factories.entrySet()) {
                builder.addFactory(entry.getKey(), entry.getValue());
            }

            if(includes!=null)
                builder.include(includes);

            assets = builder.build();
            assets.getForm().setEnabled(true);
        }

        if (assets.getForm().hasWritableAttributes()) {
            DialogueOptions options = new DialogueOptions(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    FormValidation validation = assets.getForm().validate();
                    if (!validation.hasErrors()) {
                        callback.onAdd(assets.getForm().getUpdatedEntity());
                    }
                }
            }, new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    callback.onCancel();
                }
            });

            VerticalPanel layout = new VerticalPanel();
            layout.setStyleName("fill-layout-width window-content");
            Widget formWidget = assets.getForm().asWidget();
            ModelNode opDescription = resourceDescription.get("operations").get("add").get("description");
            ContentDescription text = new ContentDescription(opDescription.asString() + " At least one of ARCHIVE or MODULE is required.");
            layout.add(text);
            layout.add(assets.getHelp().asWidget());
            layout.add(formWidget);

            ScrollPanel scroll = new ScrollPanel(layout);
            LayoutPanel content = new LayoutPanel();
            content.addStyleName("fill-layout");
            content.add(scroll);
            content.add(options);
            content.getElement().setAttribute("style", "margin-bottom:10px");
            content.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 92, Style.Unit.PCT);
            content.setWidgetBottomHeight(options, 0, Style.Unit.PX, 35, Style.Unit.PX);

            return new TrappedFocusPanel(content);
        } else {
            return new HTML(Console.CONSTANTS.noConfigurableAttributes());
        }
    }

    public void clearValues() {
        if (assets != null) {
            assets.getForm().clearValues();
        }
    }
}