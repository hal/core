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
package org.jboss.as.console.client.shared.homepage;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import org.jboss.as.console.client.shared.util.IdHelper;

/**
 * A simple content box with a header, a static html body and a link.
 *
 * @author Harald Pehl
 */
public class SimpleContentBox extends Composite implements ContentBox {

    interface Templates extends SafeHtmlTemplates {

        @Template("<div class=\"panel\">" +
                "<div class=\"panel-heading\"><h3 class=\"panel-title\">{1}</h3></div>" +
                "<div class=\"panel-body\">{2}<div id=\"{0}\" class=\"panel-link\"></div></div>" +
                "</div>")
        SafeHtml contentBox(String id, String title, SafeHtml body);
    }


    private final static Templates TEMPLATES = GWT.create(Templates.class);
    private final String id;


    public SimpleContentBox(final String id, final String title, final SafeHtml body, final String linkTitle,
            final String linkTarget) {
        this.id = id;

        String linkId = IdHelper.asId(getClass(), "_" + id);
        HTMLPanel panel = new HTMLPanel(TEMPLATES.contentBox(linkId, title, body));
        Hyperlink hyperlink = new Hyperlink(linkTitle, linkTarget);
        panel.add(hyperlink, linkId);

        initWidget(panel);
    }

    @Override
    public String getId() {
        return id;
    }
}
