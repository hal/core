/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.core;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.ViewImpl;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.core.message.MessageCenter;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;

/**
 * The main console layout that builds on GWT 2.1 layout panels.
 *
 * @author Heiko Braun
 */
public class MainLayoutViewImpl extends ViewImpl
        implements MainLayoutPresenter.MainLayoutView {

    private static final String INACTIVE_CSS = "fadeOut";
    private static final String ACTIVE_CSS = "fadeIn";
    private DockLayoutPanel panel;

    private LayoutPanel headerPanel;
    private LayoutPanel mainContentPanel;

    private LayoutPanel footerPanel;

    private Header header;
    private final PlaceManager placeManager;
    private MessageCenter messageCenter;
    private DefaultWindow window;
    private MainLayoutPresenter presenter;

    @Inject
    public MainLayoutViewImpl(final Header header, Footer footer, MessageCenter messageCenter, PlaceManager placeManager) {

        this.messageCenter = messageCenter;
        this.header = header;
        this.placeManager = placeManager;

        mainContentPanel = new LayoutPanel();
        mainContentPanel.setStyleName("main-content-panel");
        mainContentPanel.addStyleName("animated");

        // see http://www.w3.org/TR/wai-aria/states_and_properties#aria-live
        mainContentPanel.getElement().setAttribute("role", "region");
        mainContentPanel.getElement().setAttribute("aria-live", "polite");
        mainContentPanel.getElement().setId("main-content-area");

        headerPanel = new LayoutPanel();
        headerPanel.setStyleName("header-panel");
        headerPanel.getElement().setId("header");

        footerPanel = new LayoutPanel();
        footerPanel.setStyleName("footer-panel");
        footerPanel.getElement().setId("footer");

        panel = new DockLayoutPanel(Style.Unit.PX);
        panel.getElement().setAttribute("id", "container");

        panel.addNorth(headerPanel, 80);
        panel.addSouth(footerPanel, 42);
        panel.add(mainContentPanel);

        getHeaderPanel().add(header.asWidget());
        getFooterPanel().add(footer.asWidget());

        // the application window
        window = new DefaultWindow("");
       // window.addStyleName("animated");

        window.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {

                Console.getPlaceManager().revealRelativePlace(-1);
                // clearing the slot:
                // this is necessary to signal GWTP that the slot is not used
                // without subsequent attempts to reveal the same place twice would not succeed
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        presenter.clearSlot(MainLayoutPresenter.TYPE_Popup);
                        header.toggleNavigation(false);
                    }
                });

               /* window.removeStyleName(ACTIVE_CSS);
                window.addStyleName(INACTIVE_CSS);*/
            }
        });

        window.setWidth(640);
        window.setHeight(480);
        window.setAutoHideOnHistoryEventsEnabled(true);
        //window.setGlassStyleName("application-panel-glass");
        window.setGlassEnabled(true);
    }




    public Widget asWidget() {
        return panel;
    }

    @Override
    public void setPresenter(MainLayoutPresenter presenter) {

        this.presenter = presenter;
    }

    @Override
    public void setInSlot(final Object slot, final IsWidget content) {

        if (slot == MainLayoutPresenter.TYPE_MainContent) {

            // necessary to prevent onReset() callbacks
            // GWTP (correctly) assumes the presenters are still visible

            presenter.clearSlot(MainLayoutPresenter.TYPE_Popup);
            presenter.clearSlot(MainLayoutPresenter.TYPE_Hidden);

            if(content instanceof Finder) {
                header.toggleNavigation(false);
            }
            else {
                header.toggleNavigation(true);
            }


            if(content!=null)
                setMainContent(content);
        }
        else if(slot == MainLayoutPresenter.TYPE_Popup)
        {
            if(content!=null) {  // clearSlot() can cause this

                window.setWidget(content);
               /* window.removeStyleName(INACTIVE_CSS);
                window.addStyleName(ACTIVE_CSS);*/
                window.center();

                header.toggleNavigation(true);
            }
        }
        else if(slot == MainLayoutPresenter.TYPE_Hidden)
        {
            //System.out.println("<<< hidden widget >>>");
        }
        else {
            messageCenter.notify(
                    new Message("Unknown slot requested:" + slot)
            );
        }
    }

    @Override
    public void closeApplication() {

        System.out.println("<< close application >>");

        if(window!=null && window.isVisible())
            window.hide();

    }

    public void setMainContent(IsWidget content) {

        mainContentPanel.clear();

        if (content != null) {
            mainContentPanel.add(content);
        }
    }

    public LayoutPanel getHeaderPanel() {
        return headerPanel;
    }

    public LayoutPanel getFooterPanel() {
        return footerPanel;
    }

}