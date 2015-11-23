package org.jboss.as.console.client.shared.subsys.activemq;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import java.util.List;

import static org.jboss.as.console.client.widgets.nav.v3.MenuDelegate.Role.Operation;

public class ActivemqFinderView extends SuspendableViewImpl implements ActivemqFinder.MyView {

    interface Template extends SafeHtmlTemplates {

        @Template("<div class=\"{0}\">{1}<br/></div>")
        SafeHtml item(String cssClass, String title);
    }


    private static final Template TEMPLATE = GWT.create(Template.class);

    private final PlaceManager placeManager;
    private ActivemqFinder presenter;

    private LayoutPanel previewCanvas;
    private ColumnManager columnManager;
    private FinderColumn<Property> mailSessionColumn;
    private Widget mailSessionColumnWidget;

    @Inject
    public ActivemqFinderView(PlaceManager placeManager) {
        this.placeManager = placeManager;
    }

    @Override
    public void setPresenter(ActivemqFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateFrom(List<Property> list) {
        mailSessionColumn.updateFrom(list);
    }


    @Override
    @SuppressWarnings("unchecked")
    public Widget createWidget() {
        mailSessionColumn = new FinderColumn<>(
                FinderColumn.FinderId.CONFIGURATION,
                "Messaging Provider",
                new FinderColumn.Display<Property>() {

                    @Override
                    public boolean isFolder(Property data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(String baseCss, Property data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(Property data) {
                        return "";
                    }
                },
                new ProvidesKey<Property>() {
                    @Override
                    public Object getKey(Property item) {
                        return item.getName();
                    }
                },
                presenter.getProxy().getNameToken());
        mailSessionColumnWidget = mailSessionColumn.asWidget();

        mailSessionColumn.setTopMenuItems(
                new MenuDelegate<>("Add", mailSession -> presenter.launchNewProviderWizard(), Operation));

        mailSessionColumn.setMenuItems(
                new MenuDelegate<>("Queues/Topics", provider ->
                        placeManager.revealRelativePlace(
                                new PlaceRequest.Builder().nameToken(NameTokens.ActivemqMessagingPresenter)
                                        .with("name", provider.getName()).build())),

                new MenuDelegate<>("Connections", provider ->
                        placeManager.revealRelativePlace(
                                new PlaceRequest.Builder().nameToken(NameTokens.ActivemqMsgConnectionsPresenter)
                                        .with("name", provider.getName()).build())),

                new MenuDelegate<>("Clustering", provider ->
                        placeManager.revealRelativePlace(
                                new PlaceRequest.Builder().nameToken(NameTokens.ActivemqMsgClusteringPresenter)
                                        .with("name", provider.getName()).build())),

                new MenuDelegate<>("Provider Settings", presenter::onLaunchProviderSettings),

                new MenuDelegate<>("Remove", provider ->
                        Feedback.confirm(Console.MESSAGES.deleteTitle("Messaging Provider"),
                                Console.MESSAGES.deleteConfirm("provider " + provider.getName()),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        presenter.onDeleteProvider(provider);
                                    }
                                }), Operation)
        );

        mailSessionColumn.addSelectionChangeHandler(event -> {
            if (mailSessionColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(mailSessionColumnWidget);
            }
        });

        SplitLayoutPanel layout = new SplitLayoutPanel(2);
        previewCanvas = new LayoutPanel();
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.CONFIGURATION);
        columnManager.addWest(mailSessionColumnWidget);
        columnManager.add(previewCanvas);
        columnManager.setInitialVisible(1);
        return layout;
    }

    @Override
    public void setPreview(final SafeHtml html) {
        Scheduler.get().scheduleDeferred(() -> {
            previewCanvas.clear();
            previewCanvas.add(new HTML(html));
        });
    }
}
