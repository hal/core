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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.util.IdHelper;

/**
 * @author Harald Pehl
 */
public class SectionPanel extends Composite implements OpenHandler<DisclosurePanel>, CloseHandler<DisclosurePanel> {

    interface Templates extends SafeHtmlTemplates {

        @Template("<h2 id=\"{0}\" class=\"homepage-secondary-header homepage-section-header\">{1}" +
                "<span class=\"homepage-section-icon\"><i class=\"icon-angle-right\"></i></span>" +
                "</h2>")
        SafeHtml header(String id, String title);

        @Template("<p class=\"homepage-lead-paragraph homepage-section-intro\">{0}</p>")
        SafeHtml intro(String intro);
    }


    private static final Templates TEMPLATES = GWT.create(Templates.class);
    private final DisclosurePanel dp;

    public SectionPanel(final SectionData section) {
        dp = new DisclosurePanel();
        dp.setHeader(new HTML(TEMPLATES.header(IdHelper.asId(section.getId() + "_", getClass()), section.getTitle())));
        dp.addOpenHandler(this);
        dp.addCloseHandler(this);
        dp.setOpen(section.isOpen());
        FlowPanel sectionBody = new FlowPanel();
        sectionBody.addStyleName("homepage-section-body");
        sectionBody.add(new HTML(TEMPLATES.intro(section.getIntro())));
        for (ContentBox contentBox : section.getContentBoxes()) {
            // TODO wrap columns
            Widget contentBoxWidget = contentBox.asWidget();
            contentBoxWidget.addStyleName("homepage-content-box");
            sectionBody.add(contentBox);
        }
        dp.add(sectionBody);

        initWidget(dp);
        setStyleName("homepage-section");
    }

    @Override
    public void onOpen(final OpenEvent<DisclosurePanel> event) {
        setIconClassname("icon-angle-down");
    }

    @Override
    public void onClose(final CloseEvent<DisclosurePanel> event) {
        setIconClassname("icon-angle-right");
    }

    private void setIconClassname(String classname) {
        NodeList<Element> i = dp.getElement().getElementsByTagName("i");
        if (i.getLength() == 1) {
            Element iconElem = i.getItem(0);
            iconElem.setClassName(classname);
        }
    }
}
