package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.ResourceAdapter;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.ballroom.client.widgets.window.Feedback;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 27/02/15
 */
public class ResourceAdapterFinderView extends SuspendableViewImpl implements ResourceAdapterFinder.MyView {

    private final PlaceManager placeManager;
    private LayoutPanel previewCanvas;
    private SplitLayoutPanel layout;
    private FinderColumn<ResourceAdapter> adapter;
    private ResourceAdapterFinder presenter;
    private ColumnManager columnManager;
    private Widget adapterCol;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Inject
    public ResourceAdapterFinderView(PlaceManager placeManager) {

        this.placeManager = placeManager;
    }

    @Override
    public void setPresenter(ResourceAdapterFinder presenter) {

        this.presenter = presenter;
    }

    @Override
    public void updateFrom(List<ResourceAdapter> list) {
        adapter.updateFrom(list);
    }

    @Override
    public Widget createWidget() {

        previewCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.CONFIGURATION);

        adapter = new FinderColumn<ResourceAdapter>(
                FinderColumn.FinderId.CONFIGURATION,
                "Resource Adapter",
                new FinderColumn.Display<ResourceAdapter>() {

                    @Override
                    public boolean isFolder(ResourceAdapter data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(String baseCss, ResourceAdapter data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(ResourceAdapter data) {
                        return "";
                    }
                },
                new ProvidesKey<ResourceAdapter>() {
                    @Override
                    public Object getKey(ResourceAdapter item) {
                        return item.getName();
                    }
                }, presenter.getProxy().getNameToken())
        ;

        adapter.setTopMenuItems(
                new MenuDelegate<ResourceAdapter>(
                        "Add", new ContextualCommand<ResourceAdapter>() {
                    @Override
                    public void executeOn(ResourceAdapter ra) {
                        presenter.launchNewAdapterWizard();
                    }
                }, MenuDelegate.Role.Operation)
        );


        adapter.setMenuItems(
                new MenuDelegate<ResourceAdapter>(
                        "View", new ContextualCommand<ResourceAdapter>() {
                    @Override
                    public void executeOn(ResourceAdapter ra) {
                        placeManager.revealRelativePlace(
                                new PlaceRequest(NameTokens.ResourceAdapterPresenter).with("name", ra.getName())
                        );
                    }
                }),
                new MenuDelegate<ResourceAdapter>(
                        "Remove", new ContextualCommand<ResourceAdapter>() {
                    @Override
                    public void executeOn(ResourceAdapter ra) {
                        Feedback.confirm(
                                Console.MESSAGES.deleteTitle("Resource Adapter"),
                                Console.MESSAGES.deleteConfirm("Resource Adapter " + ra.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed) {
                                            presenter.onDelete(ra);
                                        }
                                    }
                                });
                    }
                }, MenuDelegate.Role.Operation)
        );

        adapter.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if(adapter.hasSelectedItem())
                {
                    ResourceAdapter item = adapter.getSelectedItem();
                    columnManager.updateActiveSelection(adapterCol);
                }
            }
        });

        adapterCol = adapter.asWidget();

        columnManager.addWest(adapterCol);
        columnManager.add(previewCanvas);

        columnManager.setInitialVisible(1);

        return layout;
    }

    @Override
    public void setPreview(final SafeHtml html) {

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                previewCanvas.clear();
                previewCanvas.add(new HTML(html));
            }
        });

    }
}
