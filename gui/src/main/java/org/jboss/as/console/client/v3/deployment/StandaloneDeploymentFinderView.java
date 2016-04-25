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
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.as.console.client.widgets.nav.v3.MenuDelegate.Role.Navigation;
import static org.jboss.as.console.client.widgets.nav.v3.MenuDelegate.Role.Operation;

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

    @Inject
    @SuppressWarnings("unchecked")
    public StandaloneDeploymentFinderView(final PlaceManager placeManager, final Dispatcher circuit,
            final PreviewContentFactory contentFactory) {

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
                        return Templates.ITEMS.item(baseCss, data.getName(), ""); // tooltip is defined below
                    }

                    @Override
                    public String rowCss(final Deployment data) {
                        if (!data.isEnabled()) {
                            return "paused";
                        } else if (data.getStatus() == Deployment.Status.FAILED) {
                            return "error"; // TODO custom style, check for other states
                        } else { return "good"; }
                    }
                },
                new ProvidesKey<Deployment>() {
                    @Override
                    public Object getKey(final Deployment item) {
                        return item.getName();
                    }
                },
                NameTokens.StandaloneDeploymentFinder,
                999);

        deploymentColumn.setFilter((item, token) -> item.getName().contains(token)
                || item.getRuntimeName().contains(token));

        deploymentColumn.setTopMenuItems(new MenuDelegate<>(Console.CONSTANTS.common_label_add(), item -> presenter.launchAddDeploymentWizard(),
                Operation));

        //noinspection Convert2MethodRef
        MenuDelegate<Deployment> enableDisableDelegate = new MenuDelegate<Deployment>(Console.CONSTANTS.common_label_enOrDisable(),
                item -> presenter.verifyEnableDisableDeployment(item), Operation) {
            @Override
            public String render(final Deployment data) {
                return data.isEnabled() ? Console.CONSTANTS.common_label_disable() : Console.CONSTANTS.common_label_enable();
            }
        };
        //noinspection Convert2MethodRef
        deploymentColumn.setMenuItems(
                new MenuDelegate<>(Console.CONSTANTS.common_label_view(), item -> presenter.showDetails(), Navigation),
                enableDisableDelegate,
                new MenuDelegate<>(Console.CONSTANTS.common_label_replace(), item -> presenter.launchReplaceDeploymentWizard(item), Operation),
                new MenuDelegate<>(Console.CONSTANTS.common_label_delete(), item -> presenter.verifyRemoveDeployment(item), Operation)
        );

        deploymentColumn.setTooltipDisplay(Templates::deploymentTooltip);
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
                circuit.dispatch(new SelectDeploymentAction(deployment));
            } else {
                startupContent(contentFactory);
            }
        });


        // ------------------------------------------------------ subdeployments

        subdeploymentColumn = new SubdeploymentColumn(placeManager, circuit, columnManager, 2,
                NameTokens.StandaloneDeploymentFinder);


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
        Deployment oldDeployment = deploymentColumn.getSelectedItem();
        Deployment newDeployment = null;

        if (oldDeployment != null) {
            for (Deployment d : deployments) {
                if (d.getName().equals(oldDeployment.getName())) {
                    newDeployment = d;
                }
            }
        }
        deploymentColumn.updateFrom(Lists.newArrayList(deployments), newDeployment);
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

    private void startupContent(PreviewContentFactory contentFactory) {
        contentFactory.createContent(PreviewContent.INSTANCE.deployments_empty(),
                new SimpleCallback<SafeHtml>() {
                    @Override
                    public void onSuccess(SafeHtml previewContent) {
                        setPreview(previewContent);
                    }
                }
        );
    }
}
