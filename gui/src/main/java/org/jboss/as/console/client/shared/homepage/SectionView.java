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

import java.util.List;

import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

/**
 * @author Harald Pehl
 */
public class SectionView extends ViewImpl implements SectionPresenter.MyView {

    interface Binder extends UiBinder<Widget, SectionView> {}


    public static class FactoryImpl implements SectionPresenter.ViewFactory {

        private final Binder binder;

        @Inject
        public FactoryImpl(Binder binder) {
            this.binder = binder;
        }

        @Override
        public SectionPresenter.MyView create(final SectionData section, final List<IsWidget> contentBoxes) {
            return new SectionView(binder, section, contentBoxes);
        }
    }


    @UiField DisclosurePanel disclosurePanel;
    @UiField SpanElement header;
    @UiField ParagraphElement intro;
    @UiField FlowPanel body;

    public SectionView(final Binder binder, final SectionData section, final List<IsWidget> contentBoxes) {
        initWidget(binder.createAndBindUi(this));
        this.header.setInnerHTML(section.getTitle());
        this.intro.setInnerText(section.getIntro());
        for (IsWidget contentBox : contentBoxes) {
            // TODO warp columns
            this.body.add(contentBox);
        }
    }

    @Override
    public void expand() {
        disclosurePanel.setOpen(true);
    }
}
