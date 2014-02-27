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

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import org.jboss.as.console.client.shared.patching.PatchManagerElementId;

/**
 * @author Harald Pehl
 */
public class Pending extends Composite implements PatchManagerElementId {

    private final static Template TEMPLATE = GWT.create(Template.class);
    private HTML html;

    public Pending(final String title) {
        html = new HTML(TEMPLATE.body(title));
        initWidget(html);
        getElement().setId(asId(PREFIX, getClass()));
        setStyleName("hal-pending");
    }

    public void setTitle(String title) {
        html.setHTML(TEMPLATE.body(title));
    }

    interface Template extends SafeHtmlTemplates {

        @SafeHtmlTemplates.Template(
                "<div><img src=\"images/loading_lite.gif\" class=\"spinner\"/><span class=\"title\">{0}</span></div>")
        SafeHtml body(String title);
    }
}
