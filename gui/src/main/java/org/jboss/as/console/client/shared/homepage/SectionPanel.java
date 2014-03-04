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

import java.util.Iterator;
import java.util.List;

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
        dp.setHeader(new HTML(TEMPLATES.header(IdHelper.asId(getClass(), "_" + section.getId()), section.getTitle())));
        dp.addOpenHandler(this);
        dp.addCloseHandler(this);
        dp.setOpen(section.isOpen());
        FlowPanel sectionBody = new FlowPanel();
        sectionBody.addStyleName("homepage-section-body");
        sectionBody.add(new HTML(TEMPLATES.intro(section.getIntro())));

        ContentBox[][] table = buildTable(section.getContentBoxes());
        for (ContentBox[] row : table) {
            FlowPanel contentBoxes = new FlowPanel();
            contentBoxes.addStyleName("homepage-content-boxes");
            sectionBody.add(contentBoxes);

            Widget widget = row[0].asWidget();
            widget.addStyleName("homepage-content-box");
            contentBoxes.add(widget);
            if (row[1] != null) {
                widget.addStyleName("two-columns"); // first column
                widget = row[1].asWidget();
                widget.addStyleName("homepage-content-box"); // second column
                widget.addStyleName("two-columns");
                contentBoxes.add(widget);
            }
        }
        dp.add(sectionBody);

        initWidget(dp);
        setStyleName("homepage-section");
    }

    private ContentBox[][] buildTable(final List<ContentBox> contentBoxes) {
        int size = contentBoxes.size();
        int rows = size % 2 == 0 ? size / 2 : size / 2 + 1;
        ContentBox[][] table = new ContentBox[rows][2];
        int columnCounter = 0;
        int rowCounter = -1;
        for (Iterator<ContentBox> iterator = contentBoxes.iterator(); iterator.hasNext(); columnCounter++) {
            columnCounter %= 2;
            if (columnCounter == 0) { rowCounter++; }
            table[rowCounter][columnCounter] = iterator.next();
        }
        return table;
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
