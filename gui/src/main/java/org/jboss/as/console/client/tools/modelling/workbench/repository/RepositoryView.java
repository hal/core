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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.tools.modelling.workbench.repository.vfs.Entry;
import org.jboss.as.console.client.widgets.DefaultSplitLayoutPanel;

import java.util.List;
import java.util.Set;

/**
 * @author Harald Pehl
 * @date 10/30/2012
 */
public class RepositoryView extends SuspendableViewImpl implements RepositoryPresenter.MyView
{

    private final SampleRepository sampleRepository;
    private RepositoryPresenter presenter;

    //private SimplePanel contentCanvas;
    private RepositoryNavigation lhsNavigation;
    private ModelEditor editor;
    private Widget nav;
    private DefaultSplitLayoutPanel layout;

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
    public void updateDirectory(Entry dir, List<Entry> entries) {
        lhsNavigation.updateDirectory(dir, entries);
    }

    @Override
    public void updateFile(String fileContents) {
        editor.setText(fileContents);
    }

    @Override
    public void clearHistory() {
        lhsNavigation.clearHistory();
    }

    @Override
    public Widget createWidget()
    {
        layout = new DefaultSplitLayoutPanel(2);

        nav = lhsNavigation.asWidget();
        nav.getElement().setAttribute("role", "navigation");
        layout.addWest(nav, 250);

        editor = new ModelEditor();
        layout.add(editor.asWidget());

        return layout;
    }

    @Override
    public void setDocument(String name, String content) {
        editor.setDialogName(name);
        editor.setText(content);
    }

    @Override
    public void setFullScreen(final boolean fullscreen) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                layout.setWidgetHidden(nav, fullscreen);

                editor.updateEditorConstraints();
            }
        });

    }
}
