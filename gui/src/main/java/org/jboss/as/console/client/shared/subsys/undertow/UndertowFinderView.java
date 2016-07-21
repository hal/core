package org.jboss.as.console.client.shared.subsys.undertow;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
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
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.PreviewFactory;

/**
 * @author Heiko Braun
 * @since 27/05/15
 */
public class UndertowFinderView extends SuspendableViewImpl implements UndertowFinder.MyView {

    public static final String SERVLET_JSP_ITEM = "Servlet/JSP";
    public static final String HTTP_ITEM = "HTTP";
    public static final String FILTERS_ITEM = "Filters";

    private UndertowFinder presenter;
    private LayoutPanel previewCanvas;
    private SplitLayoutPanel layout;
    private final PlaceManager placeManager;
    private final PreviewContentFactory previewContentFactory;
    private FinderColumn<FinderItem> links;

    private ColumnManager columnManager;
    private Widget linksCol;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Inject
    public UndertowFinderView(PlaceManager placeManager, PreviewContentFactory previewContentFactory) {
        this.placeManager = placeManager;
        this.previewContentFactory = previewContentFactory;
    }

    @Override
    public void setPresenter(UndertowFinder presenter) {

        this.presenter = presenter;
    }

    @Override
    public void setPreview(SafeHtml html) {
        Scheduler.get().scheduleDeferred(() -> {
            previewCanvas.clear();
            previewCanvas.add(new ScrollPanel(new HTML(html)));
        });
    }

    @Override
    public Widget createWidget() {
        previewCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.CONFIGURATION);

        links = new FinderColumn<FinderItem>(
                FinderColumn.FinderId.CONFIGURATION,
                "Settings",
                new FinderColumn.Display<FinderItem>() {

                    @Override
                    public boolean isFolder(FinderItem data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(String baseCss, FinderItem data) {
                        return TEMPLATE.item(baseCss, data.getTitle());
                    }

                    @Override
                    public String rowCss(FinderItem data) {
                        return "";
                    }
                },
                new ProvidesKey<FinderItem>() {
                    @Override
                    public Object getKey(FinderItem item) {
                        return item.getTitle();
                    }
                }, presenter.getProxy().getNameToken())
        ;

        links.setPreviewFactory(new PreviewFactory<FinderItem>() {
            @Override
            public void createPreview(final FinderItem data, final AsyncCallback<SafeHtml> callback) {
                if (SERVLET_JSP_ITEM.equals(data.getTitle())) {
                    previewContentFactory.createContent(PreviewContent.INSTANCE.jsp_servlet(), callback);
                } else if (HTTP_ITEM.equals(data.getTitle())) {
                    previewContentFactory.createContent(PreviewContent.INSTANCE.http(), callback);
                } else if (FILTERS_ITEM.equals(data.getTitle())) {
                    previewContentFactory.createContent(PreviewContent.INSTANCE.undertow_filters(), callback);
                }
            }
        });

        links.setMenuItems(new MenuDelegate<>(Console.CONSTANTS.common_label_view(), item -> item.getCmd().execute()));

        links.addSelectionChangeHandler(event -> {
            if(links.hasSelectedItem())
            {
                FinderItem item = links.getSelectedItem();
                columnManager.updateActiveSelection(linksCol);
            }
        });

        linksCol = links.asWidget();

        columnManager.addWest(linksCol);
        columnManager.add(previewCanvas);

        columnManager.setInitialVisible(1);


        List<FinderItem> settings = new ArrayList<>();
        settings.add(new FinderItem(SERVLET_JSP_ITEM, () -> placeManager.revealRelativePlace(new PlaceRequest(NameTokens.ServletPresenter)), false));
        settings.add(new FinderItem(HTTP_ITEM, () -> placeManager.revealRelativePlace(new PlaceRequest(NameTokens.HttpPresenter)), false));
        settings.add(new FinderItem(FILTERS_ITEM, () -> placeManager.revealRelativePlace(new PlaceRequest(NameTokens.UndertowFilters)), false));

        links.updateFrom(settings);
        return layout;
    }
}
