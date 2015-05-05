package org.jboss.as.console.client.v3.deployment;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.shared.deployment.model.DeploymentSubsystem;
import org.jboss.as.console.client.shared.deployment.model.DeploymentSubsystemElement;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
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

    private boolean hasSubdeployments;
    private DeploymentContentFinder presenter;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private ColumnManager columnManager;

    private FinderColumn<DeploymentRecord> subdeploymentColumn;
    private Widget subdeploymentColumnWidget;
    private FinderColumn<DeploymentSubsystem> subsystemColumn;
    private Widget subsystemColumnWidget;
    private FinderColumn<DeploymentSubsystemElement> resourceColumn;
    private Widget resourceColumnWidget;


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
                    public Object getKey(final DeploymentRecord item) {
                        return item.getName();
                    }
                }
        );
        subdeploymentColumn.setShowSize(true);

        subdeploymentColumn.setPreviewFactory((data, callback) -> {
            SafeHtmlBuilder html = new SafeHtmlBuilder();
            html.appendHtmlConstant("<div class='preview-content'><h2>").appendEscaped("Subdeployment")
                    .appendHtmlConstant("</h2>");
            html.appendEscaped("Something about the selected subdeployment '");
            html.appendEscaped(data.getName()).appendEscaped("'");
            html.appendHtmlConstant("</div>");
            callback.onSuccess(html.toSafeHtml());
        });

        subdeploymentColumn.addSelectionChangeHandler(
                event -> {
                    columnManager.reduceColumnsTo(1);
                    if (subdeploymentColumn.hasSelectedItem()) {
                        columnManager.updateActiveSelection(subdeploymentColumnWidget);
                        columnManager.appendColumn(subsystemColumnWidget);
                        presenter.loadSubsystems(subdeploymentColumn.getSelectedItem());
                    }
                });

        // subsystems
        subsystemColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Subsystems",
                new FinderColumn.Display<DeploymentSubsystem>() {
                    @Override
                    public boolean isFolder(final DeploymentSubsystem data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final DeploymentSubsystem data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(final DeploymentSubsystem data) {
                        return "";
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

        subsystemColumn.setPreviewFactory((data, callback) -> {
            SafeHtmlBuilder html = new SafeHtmlBuilder();
            html.appendHtmlConstant("<div class='preview-content'><h2>").appendEscaped("Subsystem")
                    .appendHtmlConstant("</h2>");
            html.appendEscaped("Something about the selected subsystem '");
            html.appendEscaped(data.getName()).appendEscaped("'");
            html.appendHtmlConstant("</div>");
            callback.onSuccess(html.toSafeHtml());
        });

        subsystemColumn.addSelectionChangeHandler(
                event -> {
                    columnManager.reduceColumnsTo(hasSubdeployments ? 2 : 1);
                    if (subsystemColumn.hasSelectedItem()) {
                        columnManager.updateActiveSelection(subsystemColumnWidget);
                        columnManager.appendColumn(resourceColumnWidget);
                        presenter.loadDeploymentResources(subsystemColumn.getSelectedItem());
                    }
                });

        // deployment resources
        resourceColumn = new FinderColumn<>(
                FinderColumn.FinderId.DEPLOYMENT,
                "Resources",
                new FinderColumn.Display<DeploymentSubsystemElement>() {
                    @Override
                    public boolean isFolder(final DeploymentSubsystemElement data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final DeploymentSubsystemElement data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(final DeploymentSubsystemElement data) {
                        return "";
                    }
                },
                new ProvidesKey<DeploymentSubsystemElement>() {
                    @Override
                    public Object getKey(final DeploymentSubsystemElement item) {
                        return item.getName();
                    }
                }
        );
        resourceColumn.setShowSize(true);

        resourceColumn.setPreviewFactory((data, callback) -> {
            SafeHtmlBuilder html = new SafeHtmlBuilder();
            html.appendHtmlConstant("<div class='preview-content'><h2>").appendEscaped("Resource")
                    .appendHtmlConstant("</h2>");
            html.appendEscaped("Something about the selected deployment resource '");
            html.appendEscaped(data.getName()).appendEscaped("'");
            html.appendHtmlConstant("</div>");
            callback.onSuccess(html.toSafeHtml());
        });

        resourceColumn.addSelectionChangeHandler(event -> {
            if (resourceColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(resourceColumnWidget);
            }
        });

        // setup UI
        subdeploymentColumnWidget = subdeploymentColumn.asWidget();
        subsystemColumnWidget = subsystemColumn.asWidget();
        resourceColumnWidget = resourceColumn.asWidget();

        contentCanvas = new LayoutPanel();
        layout = new SplitLayoutPanel(2);

        columnManager = new ColumnManager(layout);
        columnManager.addWest(subdeploymentColumnWidget);
        columnManager.addWest(subsystemColumnWidget);
        columnManager.addWest(resourceColumnWidget);
        columnManager.add(contentCanvas);
        columnManager.setInitialVisible(1);

        return layout;
    }

    @Override
    public void toggleSubdeployments(final boolean hasSubdeployments) {
        this.hasSubdeployments = hasSubdeployments;
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
    public void updateResources(final List<DeploymentSubsystemElement> resources) {
        resourceColumn.updateFrom(resources);
    }

    @Override
    public void setPreview(final SafeHtml html) {
        Scheduler.get().scheduleDeferred(() -> {
            contentCanvas.clear();
            contentCanvas.add(new HTML(html));
        });
    }

    public void clearActiveSelection(final ClearFinderSelectionEvent event) {
        subdeploymentColumnWidget.getElement().removeClassName("active");
        subsystemColumnWidget.getElement().removeClassName("active");
    }
}
