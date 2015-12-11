package org.jboss.as.console.client.shared.subsys.activemq;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
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
    private final PreviewContentFactory contentFactory;
    private ActivemqFinder presenter;

    private HTML previewCanvas;
    private ColumnManager columnManager;
    private FinderColumn<Property> messagingProviderColumn;
    private Widget messagingProviderColumnWidget;

    @Inject
    public ActivemqFinderView(PlaceManager placeManager, PreviewContentFactory contentFactory) {
        this.placeManager = placeManager;
        this.contentFactory = contentFactory;
    }

    @Override
    public void setPresenter(ActivemqFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateFrom(List<Property> list) {
        messagingProviderColumn.updateFrom(list);
    }


    @Override
    @SuppressWarnings("unchecked")
    public Widget createWidget() {
        messagingProviderColumn = new FinderColumn<>(
                FinderColumn.FinderId.CONFIGURATION,
                Console.MESSAGES.messagingProvider(),
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
        messagingProviderColumnWidget = messagingProviderColumn.asWidget();

        messagingProviderColumn.setTopMenuItems(
                new MenuDelegate<>(Console.CONSTANTS.common_label_add(),
                        mailSession -> presenter.launchNewProviderWizard(), Operation));

        messagingProviderColumn.setMenuItems(
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

                new MenuDelegate<>(Console.MESSAGES.providerSettings(), presenter::onLaunchProviderSettings),

                new MenuDelegate<>(Console.CONSTANTS.common_label_delete(), provider ->
                        Feedback.confirm(Console.MESSAGES.deleteTitle("Messaging Provider"),
                                Console.MESSAGES.deleteConfirm("provider " + provider.getName()),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        presenter.onDeleteProvider(provider);
                                    }
                                }), Operation)
        );

        messagingProviderColumn.setPreviewFactory(
                (data, callback) -> contentFactory
                        .createContent(PreviewContent.INSTANCE.messaging_provider(), callback));

        messagingProviderColumn.addSelectionChangeHandler(event -> {
            if (messagingProviderColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(messagingProviderColumnWidget);
            }
        });

        SplitLayoutPanel layout = new SplitLayoutPanel(2);
        previewCanvas = new HTML(SafeHtmlUtils.EMPTY_SAFE_HTML);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.CONFIGURATION);
        columnManager.addWest(messagingProviderColumnWidget);
        columnManager.add(new ScrollPanel(previewCanvas));
        columnManager.setInitialVisible(1);
        return layout;
    }

    @Override
    public void setPreview(final SafeHtml html) {
        Scheduler.get().scheduleDeferred(() -> {
            previewCanvas.setHTML(html);
        });
    }
}
