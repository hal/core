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
 * @author Harald Pehl
 */
public class SectionInfo extends Composite {

    interface Templates extends SafeHtmlTemplates {

        @Template("<div class=\"perspectiveInfo\">" +
                "<h3 class=\"perspectiveInfo-title\"><span id=\"{0}\"></span></h3>" +
                "<div class=\"perspectiveInfo-body\">{1}</div>" +
                "</div>")
        SafeHtml perspectiveInfo(String id, String description);
    }


    private final static Templates TEMPLATES = GWT.create(Templates.class);

    public SectionInfo(final String token, final String title, final String description) {
        String linkId = IdHelper.asId(getClass(), "_" + token);
        HTMLPanel panel = new HTMLPanel(TEMPLATES.perspectiveInfo(linkId, description));
        Hyperlink hyperlink = new Hyperlink(title, token);
        hyperlink.addStyleName("homepage-link");
        panel.add(hyperlink, linkId);

        initWidget(panel);
    }
}
