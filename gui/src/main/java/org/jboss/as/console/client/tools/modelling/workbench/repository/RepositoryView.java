/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.tools.modelling.workbench.repository;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.domain.runtime.DomainRuntimePresenter;
import org.jboss.as.console.client.widgets.DefaultSplitLayoutPanel;

import java.util.Set;

/**
 * @author Harald Pehl
 * @date 10/30/2012
 */
public class RepositoryView extends SuspendableViewImpl implements RepositoryPresenter.MyView
{

    private final SampleRepository sampleRepository;
    private RepositoryPresenter presenter;

    private SimplePanel contentCanvas;
    private RepositoryNavigation lhsNavigation;


    @Inject
    public RepositoryView(final SampleRepository sampleRepository) {
        super();
        this.sampleRepository = sampleRepository;
        this.lhsNavigation = new RepositoryNavigation();

    }

    @Override
    public void setPresenter(RepositoryPresenter presenter) {
        this.presenter = presenter;
        lhsNavigation.setPresenter(presenter);
    }

    @Override
    public void setDialogNames(Set<DialogRef> names) {
        lhsNavigation.setDialogNames(names);
    }

    @Override
    public Widget createWidget()
    {
        SplitLayoutPanel layout = new DefaultSplitLayoutPanel(2);

        contentCanvas = new SimplePanel();

        Widget nav = lhsNavigation.asWidget();
        nav.getElement().setAttribute("role", "navigation");

        contentCanvas.getElement().setAttribute("role", "main");

        layout.addWest(nav, 250);
        layout.add(contentCanvas);

        return layout;
    }

    @Override
    public void show(Widget widget) {
        contentCanvas.setWidget(widget);
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {

        if (slot == DomainRuntimePresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);

        } else {
            Console.getMessageCenter().notify(
                    new Message("Unknown slot requested:" + slot)
            );
        }
    }

    private void setContent(IsWidget  newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }



}
