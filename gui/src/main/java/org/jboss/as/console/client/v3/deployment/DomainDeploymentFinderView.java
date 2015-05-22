/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.v3.deployment;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.shared.util.Trim;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;

/**
 * @author Harald Pehl
 */
public class DomainDeploymentFinderView extends SuspendableViewImpl implements DomainDeploymentFinder.MyView {

    private DomainDeploymentFinder presenter;
    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private ColumnManager columnManager;

    private FinderColumn<ServerGroupRecord> serverGroupColumn;
    private Widget serverGroupColumnWidget;
    private FinderColumn<Assignment> assignmentColumn;
    private Widget assignmentColumnWidget;
    private SubdeploymentColumn subdeploymentColumn;
    private Widget subdeploymentColumnWidget;

    @Inject
    @SuppressWarnings("unchecked")
    public DomainDeploymentFinderView(PreviewContentFactory contentFactory) {

        contentCanvas = new LayoutPanel();
        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.DEPLOYMENT);


        // ------------------------------------------------------ server group

        serverGroupColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Server Group",
                new FinderColumn.Display<ServerGroupRecord>() {

                    @Override
                    public boolean isFolder(ServerGroupRecord data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, ServerGroupRecord data) {
                        return Templates.ITEMS.item(baseCss, data.getName(),
                                data.getName() + " (Profile " + data.getProfileName() + ")");
                    }

                    @Override
                    public String rowCss(ServerGroupRecord data) {
                        return "";
                    }
                },
                new ProvidesKey<ServerGroupRecord>() {
                    @Override
                    public Object getKey(ServerGroupRecord item) {
                        return item.getName();
                    }
                });

        serverGroupColumn.setShowSize(true);

        serverGroupColumn.setPreviewFactory((data, callback) ->
                contentFactory.createContent(PreviewContent.INSTANCE.server_group(), callback));

        serverGroupColumn.addSelectionChangeHandler(event -> {
            columnManager.reduceColumnsTo(1);
            if (serverGroupColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(serverGroupColumnWidget);
                columnManager.appendColumn(assignmentColumnWidget);
                presenter.loadAssignments(serverGroupColumn.getSelectedItem().getName());
            }
        });


        // ------------------------------------------------------ assignments

        assignmentColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Deployment",
                new FinderColumn.Display<Assignment>() {
                    @Override
                    public boolean isFolder(final Assignment data) {
                        return data.isEnabled() && data.hasDeployment() && data.getDeployment().hasSubdeployments();
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Assignment data) {
                        if (!data.hasDeployment()) {
                            return Templates.ITEMS.item(baseCss, Trim.abbreviateMiddle(data.getName(), 20),
                                    data.getName() + " (no reference server available)");
                        } else if (!data.isEnabled()) {
                            return Templates.ITEMS.item(baseCss, Trim.abbreviateMiddle(data.getName(), 20),
                                    data.getName() + " (disabled)");
                        }
                        return Templates.ITEMS.item(baseCss, Trim.abbreviateMiddle(data.getName(), 20),
                                data.getName());
                    }

                    @Override
                    public String rowCss(final Assignment data) {
                        if (!data.hasDeployment()) {
                            return "noReferenceServer";
                        } else if (!data.isEnabled()) {
                            return "inactive";
                        }
                        return "active";
                    }
                },
                new ProvidesKey<Assignment>() {
                    @Override
                    public Object getKey(final Assignment item) {
                        return item.getName();
                    }
                }
        );

        assignmentColumn.setShowSize(true);

        assignmentColumn.setTopMenuItems(
                new MenuDelegate<>("Add",
                        item -> presenter.launchAddAssignmentWizard(serverGroupColumn.getSelectedItem().getName())),
                new MenuDelegate<>("Unassigned", item -> presenter.launchUnassignedDialog()),
                new MenuDelegate<>("Refresh",
                        item -> presenter.loadAssignments(serverGroupColumn.getSelectedItem().getName()))
        );

        //noinspection Convert2MethodRef
        assignmentColumn.setMenuItems(
                new MenuDelegate<>("(En/Dis)able", item -> presenter.verifyEnableDisableAssignment(item)),
                new MenuDelegate<>("Replace", item -> presenter.launchReplaceAssignmentWizard(item)),
                new MenuDelegate<>("Remove", item -> presenter.verifyRemoveAssignment(item))
        );

        assignmentColumn.setPreviewFactory((data, callback) -> callback.onSuccess(Templates.assignmentPreview(data)));

        assignmentColumn.addSelectionChangeHandler(selectionChangeEvent -> {
            columnManager.reduceColumnsTo(2);
            if (assignmentColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(assignmentColumnWidget);
                Assignment assignment = assignmentColumn.getSelectedItem();
                if (assignment.isEnabled() && assignment.hasDeployment() && assignment.getDeployment()
                        .hasSubdeployments()) {
                    Deployment deployment = assignment.getDeployment();
                    columnManager.appendColumn(subdeploymentColumnWidget);
                    subdeploymentColumn.updateFrom(deployment.getSubdeployments());
                }
            }
        });


        // ------------------------------------------------------ subdeployments

        subdeploymentColumn = new SubdeploymentColumn(columnManager, 3);


        // ------------------------------------------------------ setup UI

        serverGroupColumnWidget = serverGroupColumn.asWidget();
        assignmentColumnWidget = assignmentColumn.asWidget();
        subdeploymentColumnWidget = subdeploymentColumn.asWidget();

        columnManager.addWest(serverGroupColumnWidget);
        columnManager.addWest(assignmentColumnWidget);
        columnManager.addWest(subdeploymentColumnWidget);
        columnManager.add(contentCanvas);
        columnManager.setInitialVisible(1);
    }

    @Override
    public void setPresenter(final DomainDeploymentFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        return layout;
    }


    // ------------------------------------------------------ update columns

    @Override
    public void updateServerGroups(final Iterable<ServerGroupRecord> serverGroups) {
        serverGroupColumn.updateFrom(Lists.newArrayList(serverGroups));
    }

    @Override
    public void updateAssignments(final Iterable<Assignment> assignments) {
        assignmentColumn.updateFrom(Lists.newArrayList(assignments));
    }


    // ------------------------------------------------------ slot management

    @Override
    public void setInSlot(final Object slot, final IsWidget content) {
        if (slot == DomainDeploymentFinder.TYPE_MainContent) {
            if (content != null) { setContent(content); } else { contentCanvas.clear(); }
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }


    // ------------------------------------------------------ finder related methods

    @Override
    public void setPreview(final SafeHtml html) {
        Scheduler.get().scheduleDeferred(() -> {
            contentCanvas.clear();
            contentCanvas.add(new HTML(html));
        });
    }

    @Override
    public void toggleScrolling(final boolean enforceScrolling, final int requiredWidth) {
        columnManager.toogleScrolling(enforceScrolling, requiredWidth);
    }

    public void clearActiveSelection(final ClearFinderSelectionEvent event) {
        serverGroupColumnWidget.getElement().removeClassName("active");
        assignmentColumnWidget.getElement().removeClassName("active");
        subdeploymentColumnWidget.getElement().removeClassName("active");
    }
}
