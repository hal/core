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

import static com.google.gwt.dom.client.Style.Unit.PCT;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;

/**
 * @author Harald Pehl
 */
public class HomepageView extends ViewImpl implements HomepagePresenter.MyView {

    interface Templates extends SafeHtmlTemplates {

        @Template("<header><h1 class=\"homepage-primary-header\">{0}</h1></header>")
        SafeHtml header(String title);

        @Template("<h2 class=\"homepage-secondary-header\">{0}</h2>")
        SafeHtml secondaryHeader(String title);

        @Template("<h3 class=\"homepage-sidebar-section-header\">{0}</h3>")
        SafeHtml sidebarSectionHeader(String title);
    }


    private static final Templates TEMPLATES = GWT.create(Templates.class);
    private final ProductConfig productConfig;
    private HorizontalPanel infoBoxes;
    private HorizontalPanel contentBoxes;
    private FlowPanel sidebarSections;

    @Inject
    public HomepageView(final ProductConfig productConfig) {
        this.productConfig = productConfig;
        initWidget(createWidget());
    }

    private Widget createWidget() {

        FlowPanel main = new FlowPanel();
        main.addStyleName("homepage-main");
        if (productConfig.getProfile() == ProductConfig.Profile.COMMUNITY) {
            main.add(new HTML(TEMPLATES.header(Console.CONSTANTS.homepage_header_community())));
        } else {
            main.add(new HTML(TEMPLATES.header(Console.CONSTANTS.homepage_header_product())));
        }

        main.add(new HTML(TEMPLATES.secondaryHeader(Console.CONSTANTS.homepage_view_and_manage())));
        infoBoxes = new HorizontalPanel();
        infoBoxes.addStyleName("homepage-info-boxes");
        main.add(infoBoxes);

        main.add(new HTML(TEMPLATES.secondaryHeader(Console.CONSTANTS.homepage_common_tasks())));
        contentBoxes = new HorizontalPanel();
        contentBoxes.addStyleName("homepage-content-boxes");
        main.add(contentBoxes);

        FlowPanel sidebar = new FlowPanel();
        sidebar.addStyleName("homepage-sidebar");
        sidebar.add(new HTML(TEMPLATES.secondaryHeader(Console.CONSTANTS.homepage_sidebar_header())));
        sidebarSections = new FlowPanel();
        sidebarSections.addStyleName("homepage-sidebar-sections");
        sidebar.add(sidebarSections);

        DockLayoutPanel root = new DockLayoutPanel(PCT);
        ScrollPanel sp = new ScrollPanel(sidebar);
        root.addEast(sp, 25);
        sp.getElement().getParentElement().addClassName("homepage-sidebar-root");
        sp = new ScrollPanel(main);
        root.add(sp);
        sp.getElement().getParentElement().addClassName("homepage-main-root");
        return root;
    }

    @Override
    public void addInfoBoxes(final List<InfoBox> boxes) {
        InfoBox[][] table = new InfoBox[calculateRows(boxes)][2];
        fillTable(table, boxes);
        addBoxes(table, infoBoxes);
    }

    @Override
    public void addContentBoxes(final List<ContentBox> boxes) {
        ContentBox[][] table = new ContentBox[calculateRows(boxes)][2];
        fillTable(table, boxes);
        addBoxes(table, contentBoxes);
    }

    private <T extends IsWidget> int calculateRows(final List<T> boxes) {
        int rows = boxes.size();
        return rows % 2 == 0 ? rows / 2 : rows / 2 + 1;
    }

    private <T extends IsWidget> void fillTable(final IsWidget[][] table, final List<T> boxes) {
        int columnCounter = 0;
        int rowCounter = -1;
        for (Iterator<T> iterator = boxes.iterator(); iterator.hasNext(); columnCounter++) {
            columnCounter %= 2;
            if (columnCounter == 0) { rowCounter++; }
            table[rowCounter][columnCounter] = iterator.next();
        }
    }

    private void addBoxes(final Widget[][] widgets, final Panel panel) {
        FlowPanel left = new FlowPanel();
        FlowPanel right = new FlowPanel();
        panel.add(left);
        panel.add(right);
        left.getElement().getParentElement().addClassName("column");
        left.getElement().getParentElement().addClassName("left");
        right.getElement().getParentElement().addClassName("column");
        right.getElement().getParentElement().addClassName("right");

        for (Widget[] row : widgets) {
            left.add(row[0]);
            if (row[1] != null) {
                right.add(row[1]);
            }
        }
    }

    @Override
    public void addSidebarSections(final List<SidebarSection> sections) {
        for (SidebarSection section : sections) {
            FlowPanel sidebarSection = new FlowPanel();
            sidebarSection.addStyleName("homepage-sidebar-section");
            sidebarSection.add(new HTML(TEMPLATES.sidebarSectionHeader(section.getTitle())));
            FlowPanel links = new FlowPanel();
            links.addStyleName("homepage-sidebar-links");
            sidebarSection.add(links);
            for (Map.Entry<String, String> linkText : section.getLinks().entrySet()) {
                String href = linkText.getKey();
                String text = linkText.getValue();

                // No template / new HTML() here. We don't want nested divs. Otherwise :last-child rules won't work!
                AnchorElement link = Document.get().createAnchorElement();
                link.setHref(href);
                link.setTarget("_blank");
                link.setClassName("homepage-link");
                link.setInnerText(text);
                links.getElement().appendChild(link);
            }
            sidebarSections.add(sidebarSection);
        }
    }

    @Override
    public void addToSlot(final Object slot, final IsWidget content) {
        if (slot == HomepagePresenter.SECTION_INFO_SLOT) {
            infoBoxes.add(content);
        } else if (slot == HomepagePresenter.CONTENT_BOX_SLOT) {
            contentBoxes.add(content);
        } else if (slot == HomepagePresenter.SIDEBAR_SLOT) {
            sidebarSections.add(content);
        } else {
            super.addToSlot(slot, content);
        }
    }
}
