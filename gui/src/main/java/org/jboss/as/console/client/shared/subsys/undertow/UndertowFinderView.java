package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 27/05/15
 */
public class UndertowFinderView extends SuspendableViewImpl implements UndertowFinder.MyView {

    private UndertowFinder presenter;
    private LayoutPanel previewCanvas;
    private SplitLayoutPanel layout;
    private final PlaceManager placeManager;
    private FinderColumn<FinderItem> links;

    private ColumnManager columnManager;
    private Widget linksCol;
    
    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Inject
    public UndertowFinderView(PlaceManager placeManager) {
        this.placeManager = placeManager;
    }

    @Override
    public void setPresenter(UndertowFinder presenter) {

        this.presenter = presenter;
    }

    @Override
    public void setPreview(SafeHtml html) {

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

        links.setMenuItems(new MenuDelegate<FinderItem>("View", new ContextualCommand<FinderItem>() {
            @Override
            public void executeOn(FinderItem item) {
                item.getCmd().execute();
            }
        }));

        links.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if(links.hasSelectedItem())
                {
                    FinderItem item = links.getSelectedItem();
                    columnManager.updateActiveSelection(linksCol);
                }
            }
        });

        linksCol = links.asWidget();

        columnManager.addWest(linksCol);
        columnManager.add(previewCanvas);

        columnManager.setInitialVisible(1);


        List<FinderItem> settings = new ArrayList<>();
        settings.add(new FinderItem("Servlet/JSP", new Command() {
            @Override
            public void execute() {
                placeManager.revealRelativePlace(new PlaceRequest(NameTokens.ServletPresenter));
            }
        }, false));
        settings.add(new FinderItem("HTTP", new Command() {
                    @Override
                    public void execute() {
                        placeManager.revealRelativePlace(new PlaceRequest(NameTokens.HttpPresenter));
                    }
                }, false));

        links.updateFrom(settings);
        return layout;
    }
}
