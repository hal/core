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
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.util.Trim;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;

/**
 * @author Harald Pehl
 */
public class StandaloneDeploymentFinderView extends SuspendableViewImpl
        implements StandaloneDeploymentFinder.MyView {

    private StandaloneDeploymentFinder presenter;
    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private ColumnManager columnManager;

    private FinderColumn<Deployment> deploymentColumn;
    private Widget deploymentColumnWidget;
    private SubdeploymentColumn subdeploymentColumn;
    private Widget subdeploymentColumnWidget;

    @SuppressWarnings("unchecked")
    public StandaloneDeploymentFinderView() {

        contentCanvas = new LayoutPanel();
        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.DEPLOYMENT);


        // ------------------------------------------------------ deployments

        deploymentColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Deployment",
                new FinderColumn.Display<Deployment>() {
                    @Override
                    public boolean isFolder(final Deployment data) {
                        return data.hasSubdeployments();
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Deployment data) {
                        return Templates.ITEMS.item(baseCss, Trim.abbreviateMiddle(data.getName()),
                                data.getName());
                    }

                    @Override
                    public String rowCss(final Deployment data) {
                        if (!data.isEnabled()) {
                            return "inactive";
                        } else if (data.getStatus() == Deployment.Status.FAILED) {
                            return "noReferenceServer"; // TODO custom style, check for other states
                        } else { return ""; }
                    }
                },
                new ProvidesKey<Deployment>() {
                    @Override
                    public Object getKey(final Deployment item) {
                        return item.getName();
                    }
                }
        );

        deploymentColumn.setTopMenuItems(
                new MenuDelegate<>("Add", item -> presenter.launchAddDeploymentWizard()),
                new MenuDelegate<>("Refresh", item -> presenter.loadDeployments())
        );

        //noinspection Convert2MethodRef
        deploymentColumn.setMenuItems(
                new MenuDelegate<>("(En/Dis)able", item -> presenter.verifyEnableDisableDeployment(item)),
                new MenuDelegate<>("Replace", item -> presenter.launchReplaceDeploymentWizard(item)),
                new MenuDelegate<>("Remove", item -> presenter.verifyRemoveDeployment(item))
        );

        deploymentColumn.setPreviewFactory((data, callback) -> callback.onSuccess(Templates.deploymentPreview(data)));

        deploymentColumn.addSelectionChangeHandler(event -> {
            columnManager.reduceColumnsTo(1);
            if (deploymentColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(deploymentColumnWidget);
                Deployment deployment = deploymentColumn.getSelectedItem();
                if (deployment.hasSubdeployments()) {
                    columnManager.appendColumn(subdeploymentColumnWidget);
                    subdeploymentColumn.updateFrom(deployment.getSubdeployments());
                }
            }
        });


        // ------------------------------------------------------ subdeployments

        subdeploymentColumn = new SubdeploymentColumn(columnManager, 2);


        // ------------------------------------------------------ setup UI

        deploymentColumnWidget = deploymentColumn.asWidget();
        subdeploymentColumnWidget = subdeploymentColumn.asWidget();

        columnManager.addWest(deploymentColumnWidget);
        columnManager.addWest(subdeploymentColumnWidget);
        columnManager.add(contentCanvas);
        columnManager.setInitialVisible(1);
    }

    @Override
    public void setPresenter(final StandaloneDeploymentFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        return layout;
    }


    // ------------------------------------------------------ update columns

    @Override
    public void updateDeployments(Iterable<Deployment> deployments) {
        deploymentColumn.updateFrom(Lists.newArrayList(deployments));
    }


    // ------------------------------------------------------ slot management

    @Override
    public void setInSlot(final Object slot, final IsWidget content) {
        if (slot == StandaloneDeploymentFinder.TYPE_MainContent) {
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
        deploymentColumnWidget.getElement().removeClassName("active");
        subdeploymentColumnWidget.getElement().removeClassName("active");
    }
}
