/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.shared.patching.ui;

import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import org.jboss.as.console.client.shared.patching.PatchManagerElementId;
import org.jboss.as.console.client.widgets.Code;
import org.jboss.as.console.client.widgets.nav.AriaLink;

/**
 * @author Harald Pehl
 */
public class ErrorDetails extends Composite implements PatchManagerElementId {

    private final Code details;

    public ErrorDetails(final String show, final String hide) {

        AriaLink showHide = new AriaLink(show);
        showHide.addStyleName("details-panel-header");
        showHide.getElement().setId(asId(PREFIX, getClass()));
        showHide.getElement().setAttribute("style", "padding-top:.5em;");

        final DisclosurePanel panel = new DisclosurePanel();
        panel.setHeader(showHide);
        panel.addStyleName("help-panel-aligned");
        panel.addOpenHandler(new OpenHandler<DisclosurePanel>() {
            @Override
            public void onOpen(OpenEvent<DisclosurePanel> event) {
                event.getTarget().addStyleName("help-panel-aligned-open");
                panel.getHeaderTextAccessor().setText(hide);
            }
        });
        panel.addCloseHandler(new CloseHandler<DisclosurePanel>() {
            @Override
            public void onClose(CloseEvent<DisclosurePanel> event) {
                panel.getHeaderTextAccessor().setText(show);
            }
        });

        details = new Code();
        panel.add(details);

        initWidget(panel);
        setStyleName("hal-error-details");
    }

    public void setDetails(String details) {
        this.details.setValue(SafeHtmlUtils.fromString(details));
    }
}
