package org.jboss.as.console.client.widgets.pages;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.Console;

import java.util.LinkedList;
import java.util.List;

import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * @author Heiko Braun
 * @date 12/6/11
 */
public class PagedView {

    private DeckPanel deck;
    private LinkBar bar;
    private boolean navOnFirstPage = false;
    private Widget navigationBar;
    private List<PageCallback> callbacks = new LinkedList<PageCallback>();
    private SplitLayoutPanel layout;

    public PagedView(boolean navOnFirstPage) {
        this.navOnFirstPage = navOnFirstPage;

        deck = new DeckPanel();
        deck.addStyleName("fill-layout");
        bar = new LinkBar(navOnFirstPage);
    }

    public PagedView() {
        this(false);
    }

    public Widget asWidget() {

        layout = new SplitLayoutPanel(2);
        layout.getElement().setId("PagedView");
        layout.setStyleName("fill-layout");


        navigationBar = bar.asWidget();
        navigationBar.addStyleName("paged-view-navigation");
        navigationBar.getElement().getStyle().setMarginTop(0, PX);
        navigationBar.getElement().getStyle().setMarginBottom(0, PX);
        navigationBar.getElement().getStyle().setMarginLeft(15, PX);
        navigationBar.getElement().getStyle().setMarginRight(15, PX);
        layout.addWest(navigationBar, 180);
        layout.add(deck);



        layout.setWidgetHidden(navigationBar, !navOnFirstPage);

        return layout;
    }

    public void addPage(String title, Widget pageWidget)
    {
        deck.add(pageWidget);
        final int index = deck.getWidgetCount()-1;

        bar.addLink(title, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showPage(index);
            }
        });
    }

    public void showPage(int index) {


        // notify callbacks
        for(PageCallback callback : callbacks)
            callback.onRevealPage(index);


        if(!navOnFirstPage && navigationBar!=null)
        {
            // navigation only on subsequent pages
            if(index>0)
            {

                layout.setWidgetHidden(navigationBar, false);
            }
            else
            {
                layout.setWidgetHidden(navigationBar, true);
            }
        }

         // TODO: clear history tokens
        if(index==0)
        {
            PlaceManager placeManager = Console.getPlaceManager();
            String nameToken = placeManager.getCurrentPlaceRequest().getNameToken();
            History.newItem(nameToken, false);
        }

        deck.showWidget(index);
        bar.setActive(index);
    }

    public void addPageCallback(PageCallback callback) {
        callbacks.add(callback);
    }

    public int getPage() {
        return deck.getVisibleWidget();
    }

    public int getPageCount() {
        return deck.getWidgetCount();
    }

    public interface PageCallback {
        void onRevealPage(int index);
    }
}
