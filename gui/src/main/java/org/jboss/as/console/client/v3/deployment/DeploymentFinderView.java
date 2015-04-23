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

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class DeploymentFinderView extends SuspendableViewImpl
        implements DeploymentFinderPresenter.MyView, ClearFinderSelectionEvent.Handler {

    private final LayoutPanel contentCanvas;
    private final SplitLayoutPanel layout;
    private final ColumnManager columnManager;
    private final FinderColumn<DeploymentRecord> deployments;

    private DeploymentFinderPresenter presenter;
    private final Widget deploymentsWidget;

    // ------------------------------------------------------ view lifecycle

    @Inject
    public DeploymentFinderView() {
        contentCanvas = new LayoutPanel();
        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout);

        deployments = new FinderColumn<>(FinderColumn.FinderId.DEPLOYMENT, "Deployments",
                new FinderColumn.Display<DeploymentRecord>() {
                    @Override
                    public boolean isFolder(final DeploymentRecord data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final DeploymentRecord data) {
                        return SafeHtmlUtils.fromString(data.getName());
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
        deployments.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {

            }
        });

        deploymentsWidget = deployments.asWidget();
        columnManager.addWest(deploymentsWidget);
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


    // ------------------------------------------------------ slot management

    @Override
    public void setInSlot(final Object slot, final IsWidget content) {
        if (slot == DeploymentFinderPresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);
            else
                contentCanvas.clear();
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }


    // ------------------------------------------------------ finder related methods

    @Override
    public void preview(final SafeHtml html) {

    }

    @Override
    public void toggleScrolling(final boolean enforceScrolling, final int requiredWidth) {
        columnManager.toogleScrolling(enforceScrolling, requiredWidth);
    }

    @Override
    public void onClearActiveSelection(final ClearFinderSelectionEvent event) {
        deploymentsWidget.getElement().removeClassName("active");
    }

    @Override
    public void setDeployments(final List<DeploymentRecord> deployments) {
        this.deployments.updateFrom(deployments);
    }
}
