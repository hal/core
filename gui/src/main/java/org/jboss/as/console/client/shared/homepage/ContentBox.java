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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHyperlink;
import org.jboss.as.console.client.shared.util.IdHelper;

/**
 * A content box on the homepage.
 *
 * @author Harald Pehl
 */
public class ContentBox extends Composite implements OpenHandler<DisclosurePanel>, CloseHandler<DisclosurePanel> {

    interface Templates extends SafeHtmlTemplates {

        @Template("<h3 id=\"{0}\" class=\"homepage-content-box-header\">{1}" +
                "<span class=\"homepage-content-box-icon\"><i class=\"icon-angle-right\"></i></span></h3>")
        SafeHtml header(String headerId, String title);

        @Template("{0}<div id=\"{1}\"></div>")
        SafeHtml body(SafeHtml body, String linkId);
    }


    private final static Templates TEMPLATES = GWT.create(Templates.class);
    private final DisclosurePanel dp;

    public ContentBox(final String id, final String title, final SafeHtml body, final String linkTitle,
            final String linkTarget) {

        dp = new DisclosurePanel();
        dp.setHeader(new HTML(TEMPLATES.header(IdHelper.asId(id + "_", getClass(), "_" + "header"), title)));
        dp.addOpenHandler(this);
        dp.addCloseHandler(this);
        dp.setOpen(true);

        String linkId = IdHelper.asId(id + "_", getClass(), "_" + "link");
        HTMLPanel panel = new HTMLPanel(TEMPLATES.body(body, linkId));
        panel.addStyleName("homepage-content-box-body");
        InlineHyperlink hyperlink = new InlineHyperlink(linkTitle, linkTarget);
        hyperlink.addStyleName("homepage-link");
        panel.add(hyperlink, linkId);
        dp.add(panel);

        initWidget(dp);
        setStyleName("homepage-content-box");
    }

    @Override
    public void onOpen(final OpenEvent<DisclosurePanel> event) {
        setIconClassname("icon-angle-down");
    }

    @Override
    public void onClose(final CloseEvent<DisclosurePanel> event) {
        setIconClassname("icon-angle-right");
    }

    private void setIconClassname(String styleName) {
        NodeList<Element> i = dp.getElement().getElementsByTagName("i");
        if (i.getLength() == 1) {
            Element iconElem = i.getItem(0);
            iconElem.setClassName(styleName);
        }
    }
}
