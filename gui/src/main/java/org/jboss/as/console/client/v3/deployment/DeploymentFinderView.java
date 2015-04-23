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
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class DeploymentFinderView extends SuspendableViewImpl
        implements DeploymentFinderPresenter.MyView, ClearFinderSelectionEvent.Handler {

    interface Template extends SafeHtmlTemplates {

        @Template("<div class=\"{0}\" title='{1}'>{1}</div>")
        SafeHtml item(String cssClass, String title);
    }


    private static final Template TEMPLATE = GWT.create(Template.class);

    private DeploymentFinderPresenter presenter;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private ColumnManager columnManager;

    private FinderColumn<DeploymentRecord> deploymentsColumn;
    private Widget deploymentsColumnWidget;
    private FinderColumn<String> assignedGroupsColumn;
    private Widget assignedGroupsColumnWidget;



    // ------------------------------------------------------ view lifecycle

    @Inject
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
        deploymentsColumn.addSelectionChangeHandler(event -> {
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
                new FinderColumn.Display<String>() {

                    @Override
                    public boolean isFolder(String data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, String data) {
                        return TEMPLATE.item(baseCss, data);
                    }

                    @Override
                    public String rowCss(String data) {
                        return "";
                    }
                },
                new ProvidesKey<String>() {
                    @Override
                    public Object getKey(String item) {
                        return item;
                    }
                });

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
            if (assignedGroupsColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(assignedGroupsColumnWidget);
                String selectedGroup = assignedGroupsColumn.getSelectedItem();
                // TODO Place request for nested deployment finder
                Scheduler.get().scheduleDeferred(() -> circuit.dispatch(new GroupSelection(selectedGroup)));
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
    public void setPresenter(final DeploymentFinderPresenter presenter) {
        this.presenter = presenter;
    }


    // ------------------------------------------------------ deployment related methods

    @Override
    public void updateDeployments(final List<DeploymentRecord> deployments) {
        this.deploymentsColumn.updateFrom(deployments);
    }

    @Override
    public void updateServerGroups(final List<String> serverGroups) {
        this.assignedGroupsColumn.updateFrom(serverGroups);
    }


    // ------------------------------------------------------ slot management

    @Override
   public void setInSlot(final Object slot, final IsWidget content) {
        if (slot == DeploymentFinderPresenter.TYPE_MainContent) {
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
}
