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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.deployment.DeploymentCommand;
import org.jboss.as.console.client.shared.deployment.DeploymentCommandDelegate;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.v3.deployment.DeploymentFinder.ServerGroupAssignment;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class DeploymentFinderView extends SuspendableViewImpl
        implements DeploymentFinder.MyView, ClearFinderSelectionEvent.Handler {

    interface Template extends SafeHtmlTemplates {

        @Template("<div class=\"{0}\" title='{1}'>{1}</div>")
        SafeHtml item(String cssClass, String title);
    }


    private static final Template TEMPLATE = GWT.create(Template.class);

    private DeploymentFinder presenter;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private ColumnManager columnManager;

    private FinderColumn<DeploymentRecord> deploymentsColumn;
    private Widget deploymentsColumnWidget;
    private FinderColumn<ServerGroupAssignment> assignedGroupsColumn;
    private Widget assignedGroupsColumnWidget;



    // ------------------------------------------------------ view lifecycle

    @Inject
    @SuppressWarnings("unchecked")
    public DeploymentFinderView(final Dispatcher circuit) {

        // deployments column
        deploymentsColumn = new FinderColumn<>(FinderColumn.FinderId.DEPLOYMENT, "Deployments",
                new FinderColumn.Display<DeploymentRecord>() {
                    @Override
                    public boolean isFolder(final DeploymentRecord data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final DeploymentRecord data) {
                        return TEMPLATE.item(baseCss, data.getName());
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
                });
        deploymentsColumn.setShowSize(true);

        deploymentsColumn.setTopMenuItems(
                new MenuDelegate<>("Add", item -> presenter.launchNewDeploymentDialoge(null, false)),
                new MenuDelegate<>("Refresh", item -> presenter.refreshDeployments()));

        //noinspection Convert2MethodRef
        deploymentsColumn.setMenuItems(new MenuDelegate<>("Remove", item -> presenter.onRemoveContent(item)));

        deploymentsColumn.addSelectionChangeHandler(event -> {
            clearNestedPresenter();
            columnManager.reduceColumnsTo(1);
            if (deploymentsColumn.hasSelectedItem()) {
                columnManager.appendColumn(assignedGroupsColumnWidget);
                DeploymentRecord selectedItem = deploymentsColumn.getSelectedItem();
                presenter.loadAssignmentsFor(selectedItem);
            }
        });
        deploymentsColumnWidget = deploymentsColumn.asWidget();

        // assigned groups column
        assignedGroupsColumn = new FinderColumn<>(
                FinderColumn.FinderId.RUNTIME,
                "Server Group",
                new FinderColumn.Display<ServerGroupAssignment>() {

                    @Override
                    public boolean isFolder(ServerGroupAssignment data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, ServerGroupAssignment data) {
                        return TEMPLATE.item(baseCss, data.serverGroup);
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
                });

        assignedGroupsColumn.setTopMenuItems(
                new MenuDelegate<>("Add", item ->
                        new DeploymentCommandDelegate(presenter,
                                DeploymentCommand.ADD_TO_GROUP).execute(deploymentsColumn.getSelectedItem()))
        );

        assignedGroupsColumn.setShowSize(true);
        assignedGroupsColumn.setComparisonType("filter");
        assignedGroupsColumn.setPreviewFactory((data, callback) -> {
            SafeHtmlBuilder html = new SafeHtmlBuilder();
            html.appendHtmlConstant("<div class='preview-content'><h2>").appendEscaped("Server Groups")
                    .appendHtmlConstant("</h2>");
            html.appendEscaped(
                    "A server group is set of server instances that will be managed and configured as one. In a managed domain each application server instance is a member of a server group. (Even if the group only has a single server, the server is still a member of a group.) It is the responsibility of the Domain Controller and the Host Controllers to ensure that all servers in a server group have a consistent configuration. They should all be configured with the same profile and they should have the same deployment content deployed.");
            html.appendHtmlConstant("</div>");
            callback.onSuccess(html.toSafeHtml());
        });

        assignedGroupsColumn.addSelectionChangeHandler(event -> {
            columnManager.reduceColumnsTo(2);
            if (assignedGroupsColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(assignedGroupsColumnWidget);
                ServerGroupAssignment selectedAssignment = assignedGroupsColumn.getSelectedItem();
                Scheduler.get().scheduleDeferred(() -> {
                    circuit.dispatch(new DeploymentSelection(selectedAssignment.deployment));
                    circuit.dispatch(new GroupSelection(selectedAssignment.serverGroup));
                    PlaceRequest placeRequest = new PlaceRequest.Builder().nameToken(NameTokens.DeploymentContentFinder)
                            .build();
                    presenter.getPlaceManager().revealRelativePlace(placeRequest);
                });
            }
        });
        assignedGroupsColumnWidget = assignedGroupsColumn.asWidget();

        // setup UI
        contentCanvas = new LayoutPanel();
        layout = new SplitLayoutPanel(2);

        columnManager = new ColumnManager(layout);
        columnManager.addWest(deploymentsColumnWidget);
        columnManager.addWest(assignedGroupsColumnWidget);
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
    public void updateDeployments(final List<DeploymentRecord> deployments) {
        deploymentsColumn.updateFrom(deployments);
    }

    @Override
    public void updateServerGroups(final List<ServerGroupAssignment> assignments) {
        assignedGroupsColumn.updateFrom(assignments);
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
        if (contentCanvas.getWidgetCount() == 0) {
            Scheduler.get().scheduleDeferred(() -> contentCanvas.add(new HTML(html)));
        }
    }

    @Override
    public void toggleScrolling(final boolean enforceScrolling, final int requiredWidth) {
        columnManager.toogleScrolling(enforceScrolling, requiredWidth);
    }

    @Override
    public void onClearActiveSelection(final ClearFinderSelectionEvent event) {
        deploymentsColumnWidget.getElement().removeClassName("active");
    }

    private void clearNestedPresenter() {
        presenter.clearSlot(DeploymentFinder.TYPE_MainContent);
        Scheduler.get().scheduleDeferred(() -> {
            if (presenter.getPlaceManager().getHierarchyDepth() > 1)
                presenter.getPlaceManager().revealRelativePlace(1);
        });
    }
}
