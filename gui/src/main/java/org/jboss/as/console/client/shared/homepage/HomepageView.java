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

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
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
    private FlowPanel sectionsInfos;
    private FlowPanel contentBoxes;
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
        sectionsInfos = new FlowPanel();
        sectionsInfos.addStyleName("homepage-sections");
        main.add(sectionsInfos);

        main.add(new HTML(TEMPLATES.secondaryHeader(Console.CONSTANTS.homepage_common_tasks())));
        contentBoxes = new FlowPanel();
        contentBoxes.addStyleName("homepage-contentBoxes");
        main.add(contentBoxes);

        FlowPanel sidebar = new FlowPanel();
        sidebar.addStyleName("homepage-sidebar");
        sidebar.add(new HTML(TEMPLATES.secondaryHeader(Console.CONSTANTS.homepage_sidebar_header())));
        sidebarSections = new FlowPanel();
        sidebarSections.addStyleName("homepage-sidebar-sectionsInfos");
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
    public void addSectionInfo(final SectionInfo sectionInfo) {
        sectionsInfos.add(sectionInfo);
    }

    @Override
    public void addContentBox(final ContentBox contentBox) {
        contentBoxes.add(contentBox);
    }

    @Override
    public void addSidebarSection(final SidebarSectionData section) {
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

    @Override
    public void addToSlot(final Object slot, final IsWidget content) {
        if (slot == HomepagePresenter.SECTION_INFO_SLOT) {
            sectionsInfos.add(content);
        } else if (slot == HomepagePresenter.CONTENT_BOX_SLOT) {
            contentBoxes.add(content);
        } else if (slot == HomepagePresenter.SIDEBAR_SLOT) {
            sidebarSections.add(content);
        } else {
            super.addToSlot(slot, content);
        }
    }
}
