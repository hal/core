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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.deployment.DeploymentCommand;
import org.jboss.as.console.client.shared.deployment.DeploymentCommandDelegate;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.shared.util.Trim;
import org.jboss.as.console.client.v3.deployment.DeploymentFinder.ServerGroupAssignment;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class DeploymentFinderView extends SuspendableViewImpl
        implements DeploymentFinder.MyView {

    interface Template extends SafeHtmlTemplates {

        @Template("<div class=\"{0}\" title='{2}'>{1}</div>")
        SafeHtml item(String cssClass, String shortName, String fullName);

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "<ul>" +
                "<li>Runtime Name: {1}</li>" +
                "</ul>" +
                "</div>")
        SafeHtml content(String name, String runtimeName);

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "<ul>" +
                "<li>Runtime Name: {1}</li>" +
                "<li>Enabled: {2}</li>" +
                "</ul>" +
                "</div>")
        SafeHtml assignment(String name, String runtimeName, boolean enabled);

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "<ul>" +
                "<li>Runtime Name: {1}</li>" +
                "<li>Reference Host: {2}</li>" +
                "<li>Reference Server: {3}</li>" +
                "</ul>" +
                "</div>")
        SafeHtml deployment(String name, String runtimeName, String referenceHost, String referenceServer);
    }


    private static final Template TEMPLATE = GWT.create(Template.class);

    private DeploymentFinder presenter;
    private boolean hasSubdeployments;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private ColumnManager columnManager;

    private FinderColumn<DeploymentRecord> contentRepositoryColumn;
    private Widget contentRepositoryColumnWidget;
    private FinderColumn<ServerGroupAssignment> assignedGroupsColumn;
    private Widget assignedGroupsColumnWidget;
    private FinderColumn<DeploymentRecord> deploymentColumn;
    private Widget deploymentColumnWidget;
    private FinderColumn<DeploymentRecord> subdeploymentColumn;
    private Widget subdeploymentColumnWidget;


    @Inject
    @SuppressWarnings("unchecked")
    public DeploymentFinderView() {

        // ------------------------------------------------------ content repository

        contentRepositoryColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Content",
                new FinderColumn.Display<DeploymentRecord>() {
                    @Override
                    public boolean isFolder(final DeploymentRecord data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final DeploymentRecord data) {
                        return TEMPLATE.item(baseCss, Trim.abbreviateMiddle(data.getName(), 20), data.getName());
                    }

                    @Override
                    public String rowCss(final DeploymentRecord data) {
                        return "";
                    }
                },
                new ProvidesKey<DeploymentRecord>() {
                    @Override
                    public Object getKey(DeploymentRecord item) {
                        return item.getName();
                    }
                }
        );
        contentRepositoryColumn.setShowSize(true);

        contentRepositoryColumn.setTopMenuItems(
                new MenuDelegate<>("<i class=\"icon-plus\" style='color:black'></i>&nbsp;Add",
                        item -> presenter.launchNewDeploymentDialoge(null, false)),
                new MenuDelegate<>("<i class=\"icon-refresh\" style='color:black'></i>&nbsp;Refresh",
                        item -> presenter.refreshDeployments()));

        //noinspection Convert2MethodRef
        contentRepositoryColumn.setMenuItems(
                new MenuDelegate<>("Remove", item -> presenter.onRemoveContent(item)),
                new MenuDelegate<>("Replace", item1 -> Console.warning("Not yet implemented")));

        contentRepositoryColumn.setPreviewFactory((data, callback) ->
                callback.onSuccess(TEMPLATE.content(data.getName(), data.getRuntimeName())));

        contentRepositoryColumn.addSelectionChangeHandler(event -> {
            //            clearNestedPresenter();
            columnManager.reduceColumnsTo(1);

            if (contentRepositoryColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(contentRepositoryColumnWidget);
                columnManager.appendColumn(assignedGroupsColumnWidget);
                if (contentRepositoryColumn.hasSelectedItem()) {
                    presenter.loadAssignmentsFor(contentRepositoryColumn.getSelectedItem());
                }
            }
        });

        // ------------------------------------------------------ assignments

        assignedGroupsColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Assigned To",
                new FinderColumn.Display<ServerGroupAssignment>() {

                    @Override
                    public boolean isFolder(ServerGroupAssignment data) {
                        return data.deployment.isEnabled();
                    }

                    @Override
                    public SafeHtml render(String baseCss, ServerGroupAssignment data) {
                        return TEMPLATE.item(baseCss, Trim.abbreviateMiddle(data.serverGroup, 20), data.serverGroup);
                    }

                    @Override
                    public String rowCss(ServerGroupAssignment data) {
                        return data.deployment.isEnabled() ? "active" : "inactive";
                    }
                },
                new ProvidesKey<ServerGroupAssignment>() {
                    @Override
                    public Object getKey(ServerGroupAssignment item) {
                        return item.serverGroup;
                    }
                }
        );
        assignedGroupsColumn.setShowSize(true);

        assignedGroupsColumn.setTopMenuItems(
                new MenuDelegate<>("Assign", item ->
                        new DeploymentCommandDelegate(presenter,
                                DeploymentCommand.ADD_TO_GROUP).execute(contentRepositoryColumn.getSelectedItem()))
        );

        assignedGroupsColumn.setMenuItems(
                new MenuDelegate<>("E / D", item -> presenter.enableDisableDeployment(item.deployment)),
                new MenuDelegate<>("Remove", item -> presenter.removeDeploymentFromGroup(item.deployment))
        );

        assignedGroupsColumn.setPreviewFactory((data, callback) ->
                callback.onSuccess(TEMPLATE.assignment(data.deployment.getName(), data.deployment.getRuntimeName(),
                        data.deployment.isEnabled())));

        assignedGroupsColumn.addSelectionChangeHandler(event -> {
            //            clearNestedPresenter();
            columnManager.reduceColumnsTo(2);

            if (assignedGroupsColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(assignedGroupsColumnWidget);
                presenter.loadDeployment(assignedGroupsColumn.getSelectedItem());
/*
                    Scheduler.get().scheduleDeferred(() -> {
                        circuit.dispatch(new DeploymentSelection(selectedAssignment.deployment));
                        circuit.dispatch(new GroupSelection(selectedAssignment.serverGroup));
                        PlaceRequest placeRequest = new PlaceRequest.Builder()
                                .nameToken(NameTokens.DeploymentContentFinder)
                                .build();
                        presenter.getPlaceManager().revealRelativePlace(placeRequest);
                    });
*/
            }
        });

        // ------------------------------------------------------ sub deployments

        subdeploymentColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Subdeployment",
                new FinderColumn.Display<DeploymentRecord>() {
                    @Override
                    public boolean isFolder(final DeploymentRecord data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final DeploymentRecord data) {
                        return TEMPLATE.item(baseCss, Trim.abbreviateMiddle(data.getName(), 20), data.getName());
                    }

                    @Override
                    public String rowCss(final DeploymentRecord data) {
                        return "";
                    }
                },
                new ProvidesKey<DeploymentRecord>() {
                    @Override
                    public Object getKey(final DeploymentRecord item) {
                        return item.getName();
                    }
                }
        );

        subdeploymentColumn.setPreviewFactory((data, callback) ->
                callback.onSuccess(TEMPLATE.deployment(data.getName(), data.getRuntimeName(), "Foo", "Bar")));

        subdeploymentColumn.addSelectionChangeHandler(
                event -> {
                    columnManager.reduceColumnsTo(3);
                    if (subdeploymentColumn.hasSelectedItem()) {
                        columnManager.updateActiveSelection(subdeploymentColumnWidget);
                    }
                });

        // ------------------------------------------------------ deployments

        deploymentColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Deployment",
                new FinderColumn.Display<DeploymentRecord>() {
                    @Override
                    public boolean isFolder(final DeploymentRecord data) {
                        return data.isHasSubdeployments();
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final DeploymentRecord data) {
                        return TEMPLATE.item(baseCss, Trim.abbreviateMiddle(data.getName(), 20), data.getName());
                    }

                    @Override
                    public String rowCss(final DeploymentRecord data) {
                        return "";
                    }
                },
                new ProvidesKey<DeploymentRecord>() {
                    @Override
                    public Object getKey(final DeploymentRecord item) {
                        return item.getName();
                    }
                }
        );

        deploymentColumn.setPreviewFactory((data, callback) ->
                callback.onSuccess(TEMPLATE.deployment(data.getName(), data.getRuntimeName(), "Foo", "Bar")));

        deploymentColumn.addSelectionChangeHandler(
                event -> {
                    columnManager.reduceColumnsTo(hasSubdeployments ? 4 : 3);
                    if (subdeploymentColumn.hasSelectedItem()) {
                        columnManager.updateActiveSelection(subdeploymentColumnWidget);
                    }
                });

        // setup UI
        contentRepositoryColumnWidget = contentRepositoryColumn.asWidget();
        assignedGroupsColumnWidget = assignedGroupsColumn.asWidget();
        subdeploymentColumnWidget = subdeploymentColumn.asWidget();
        deploymentColumnWidget = deploymentColumn.asWidget();

        contentCanvas = new LayoutPanel();
        layout = new SplitLayoutPanel(2);

        columnManager = new ColumnManager(layout);
        columnManager.addWest(contentRepositoryColumnWidget);
        columnManager.addWest(assignedGroupsColumnWidget);
        columnManager.addWest(subdeploymentColumnWidget);
        columnManager.addWest(deploymentColumnWidget);
        columnManager.add(contentCanvas);
        columnManager.setInitialVisible(1);
    }

    @Override
    public Widget createWidget() {
        return layout;
    }


    @Override
    public void setPresenter(final DeploymentFinder presenter) {
        this.presenter = presenter;
    }


    // ------------------------------------------------------ deployment related methods

    @Override
    public void updateContentRepository(final List<DeploymentRecord> deployments) {
        contentRepositoryColumn.updateFrom(deployments);
    }

    @Override
    public void updateAssignments(final List<ServerGroupAssignment> assignments) {
        assignedGroupsColumn.updateFrom(assignments);
    }

    @Override
    public void toggleSubdeployments(final boolean hasSubdeployments) {
        this.hasSubdeployments = hasSubdeployments;
        columnManager.reduceColumnsTo(2);
        if (hasSubdeployments) {
            columnManager.appendColumn(subdeploymentColumnWidget);
        } else {
            columnManager.appendColumn(deploymentColumnWidget);
        }
    }

    @Override
    public void updateSubdeployments(final List<DeploymentRecord> subdeployments) {
        subdeploymentColumn.updateFrom(subdeployments);
    }

    @Override
    public void updateDeployments(final List<DeploymentRecord> deployments) {
        deploymentColumn.updateFrom(deployments);
    }


    // ------------------------------------------------------ slot management

    @Override
    public void setInSlot(final Object slot, final IsWidget content) {
        if (slot == DeploymentFinder.TYPE_MainContent) {
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
        //        if (contentCanvas.getWidgetCount() == 0) {
        Scheduler.get().scheduleDeferred(() -> {
            contentCanvas.clear();
            contentCanvas.add(new HTML(html));
        });
        //        }
    }

    @Override
    public void toggleScrolling(final boolean enforceScrolling, final int requiredWidth) {
        columnManager.toogleScrolling(enforceScrolling, requiredWidth);
    }

    public void clearActiveSelection(final ClearFinderSelectionEvent event) {
        contentRepositoryColumnWidget.getElement().removeClassName("active");
        assignedGroupsColumnWidget.getElement().removeClassName("active");
    }

    private void clearNestedPresenter() {
        presenter.clearSlot(DeploymentFinder.TYPE_MainContent);
        Scheduler.get().scheduleDeferred(() -> {
            if (presenter.getPlaceManager().getHierarchyDepth() > 1) {
                presenter.getPlaceManager().revealRelativePlace(1);
            }
        });
    }
}
