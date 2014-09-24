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
package org.jboss.as.console.client.administration.authorization;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.*;
import org.jboss.as.console.client.Console;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.Map;

/**
 * @author Harald Pehl
 */
public class NoAdminRolesDialog implements IsWidget {

    private final AuthorizationPresenter presenter;
    private final Map<String, Object> changedValues;
    private final boolean providerChanged;

    public NoAdminRolesDialog(AuthorizationPresenter presenter, Map<String, Object> changedValues,
                              boolean providerChanged) {
        this.presenter = presenter;
        this.changedValues = changedValues;
        this.providerChanged = providerChanged;
    }

    @Override
    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.append(Console.MESSAGES.no_admin_roles());
        layout.add(new HTML(builder.toSafeHtml()));

        DialogueOptions options = new DialogueOptions(
                Console.CONSTANTS.common_label_cancel(),
                new ClickHandler() {
                    @Override
                    public void onClick(final ClickEvent event) {
                        presenter.closeWindow();
                        presenter.onReset();
                    }
                },
                Console.CONSTANTS.common_label_save(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.closeWindow();
                        presenter.saveInternal(changedValues, providerChanged);
                    }
                }
        );
        return new WindowContentBuilder(new ScrollPanel(layout), options).build();
    }
}
