package org.jboss.as.console.client.v3.deployment;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.shared.deployment.model.DeploymentSubsystem;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 27/02/15
 */
public class DeploymentContentFinderView extends SuspendableViewImpl implements DeploymentContentFinder.MyView {

    interface Template extends SafeHtmlTemplates {

        @Template("<div class=\"{0}\" title='{1}'>{1}</div>")
        SafeHtml item(String cssClass, String title);
    }


    private static final Template TEMPLATE = GWT.create(Template.class);

    private DeploymentContentFinder presenter;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private ColumnManager columnManager;

    private FinderColumn<DeploymentRecord> subdeploymentColumn;
    private Widget subdeploymentColumnWidget;
    private FinderColumn<DeploymentSubsystem> subsystemColumn;
    private Widget subsystemColumnWidget;


    @Override
    public void setPresenter(DeploymentContentFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        // sub deployments
        subdeploymentColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Subdeployments",
                new FinderColumn.Display<DeploymentRecord>() {
                    @Override
                    public boolean isFolder(final DeploymentRecord data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final DeploymentRecord data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(final DeploymentRecord data) {
                        return null;
                    }
                },
                new ProvidesKey<DeploymentRecord>() {
                    @Override
                    public Object getKey(final DeploymentRecord item) {
                        return item.getName();
                    }
                }
        );
        subdeploymentColumn.setShowSize(true);
        subdeploymentColumn.addSelectionChangeHandler(
                event -> {
                    columnManager.reduceColumnsTo(1);
                    if (subdeploymentColumn.hasSelectedItem()) {
                        columnManager.appendColumn(subsystemColumnWidget);
                        DeploymentRecord subdeployment = subdeploymentColumn.getSelectedItem();
                        presenter.loadSubsystems(subdeployment);
                    }
                });
        subdeploymentColumnWidget = subdeploymentColumn.asWidget();

        // subsystems
        subsystemColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Subsystems",
                new FinderColumn.Display<DeploymentSubsystem>() {
                    @Override
                    public boolean isFolder(final DeploymentSubsystem data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final DeploymentSubsystem data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(final DeploymentSubsystem data) {
                        return null;
                    }
                },
                new ProvidesKey<DeploymentSubsystem>() {
                    @Override
                    public Object getKey(final DeploymentSubsystem item) {
                        return item.getName();
                    }
                }
        );
        subsystemColumn.setShowSize(true);
        subsystemColumn.addSelectionChangeHandler(
                event -> {
                    if (subsystemColumn.hasSelectedItem()) {
                        System.out.println("Selected subsystem: " + subsystemColumn.getSelectedItem().getName());
                    }
                });
        subsystemColumnWidget = subsystemColumn.asWidget();

        // setup UI
        contentCanvas = new LayoutPanel();
        layout = new SplitLayoutPanel(2);

        columnManager = new ColumnManager(layout);
        columnManager.addWest(subdeploymentColumnWidget);
        columnManager.addWest(subsystemColumnWidget);
        columnManager.add(contentCanvas);
        columnManager.setInitialVisible(1);

        return layout;
    }

    @Override
    public void toggleSubdeployments(final boolean hasSubdeployments) {
        columnManager.reduceColumnsTo(0);
        if (hasSubdeployments) {
            columnManager.appendColumn(subdeploymentColumnWidget);
        } else {
            columnManager.appendColumn(subsystemColumnWidget);
        }
    }

    @Override
    public void updateSubdeployments(final List<DeploymentRecord> subdeployments) {
        subdeploymentColumn.updateFrom(subdeployments);
    }

    @Override
    public void updateSubsystems(final List<DeploymentSubsystem> subsystems) {
        subsystemColumn.updateFrom(subsystems);
    }

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
}
