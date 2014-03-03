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

import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;

/**
 * @author Harald Pehl
 */
public class HomepageView extends ViewImpl implements HomepagePresenter.MyView {

    interface Binder extends UiBinder<Widget, HomepageView> {}


    @UiField HeadingElement header;
    @UiField ParagraphElement intro;
    @UiField FlowPanel sections;
    @UiField HeadingElement sidebarHeader;
    @UiField FlowPanel sidebarSections;

    @Inject
    public HomepageView(Binder binder, ProductConfig productConfig) {
        initWidget(binder.createAndBindUi(this));
        if (productConfig.getProfile() == ProductConfig.Profile.COMMUNITY) {
            header.setInnerText(Console.CONSTANTS.homepage_header_community());
            intro.setInnerText(Console.CONSTANTS.homepage_intro_community());

        } else {
            header.setInnerText(Console.CONSTANTS.homepage_header_product());
            intro.setInnerText(Console.CONSTANTS.homepage_intro_product());
        }
        sidebarHeader.setInnerText(Console.CONSTANTS.homepage_sidebar_header());
    }

    @Override
    public void addToSlot(final Object slot, final IsWidget content) {
        if (slot == HomepagePresenter.SECTIONS_SLOT) {
            sections.add(content);
        } else {
            super.addToSlot(slot, content);
        }
    }
}
