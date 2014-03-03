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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.View;
import org.jboss.as.console.client.shared.homepage.content.ContentBox;
import org.jboss.as.console.client.shared.homepage.content.ContentBoxRegistry;

/**
 * @author Harald Pehl
 */

public class SectionPresenter extends PresenterWidget<SectionPresenter.MyView> {

    public interface MyView extends View {

        void expand();
    }


    /**
     * Factory to create {@link org.jboss.as.console.client.shared.homepage.SectionPresenter.MyView} instances.
     */
    public interface ViewFactory {

        MyView create(SectionData section, List<IsWidget> contentBoxes);
    }


    /**
     * Factory to create {@link org.jboss.as.console.client.shared.homepage.SectionPresenter} instances.
     */
    public interface Factory {

        SectionPresenter create(SectionData section);
    }


    public static class FactoryImpl implements Factory {

        private final ContentBoxRegistry contentBoxRegistry;
        private final EventBus eventBus;
        private final ViewFactory viewFactory;

        @Inject
        public FactoryImpl(ContentBoxRegistry contentBoxRegistry, EventBus eventBus, ViewFactory viewFactory) {
            this.contentBoxRegistry = contentBoxRegistry;
            this.eventBus = eventBus;
            this.viewFactory = viewFactory;
        }

        @Override
        public SectionPresenter create(SectionData section) {
            List<String> contentBoxIds = section.getContentBoxIds();
            List<IsWidget> contentBoxes = new ArrayList<IsWidget>();
            for (String id : contentBoxIds) {
                ContentBox contentBox = contentBoxRegistry.get(id);
                if (contentBox != null) {
                    contentBoxes.add(contentBox);
                }
            }
            return new SectionPresenter(eventBus, viewFactory.create(section, contentBoxes));
        }
    }


    private SectionPresenter(final EventBus eventBus, final MyView view) {
        super(eventBus, view);
    }

    public void collapse() {
        getView().expand();
    }
}
