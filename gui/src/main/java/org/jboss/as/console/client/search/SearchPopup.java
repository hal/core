/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.search;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.widgets.popups.DefaultPopup;
import org.jboss.as.console.client.widgets.progress.ProgressElement;

/**
* @author Harald Pehl
*/
class SearchPopup extends DefaultPopup {

    private final static String INDEXING_HEIGHT = "132px";
    private final static String SEARCH_HEIGHT = "26px";

    private final Harvest harvest;
    private final Index index;

    private final DeckPanel deck;
    private final SuggestBox searchBox;
    private final ProgressElement progressBar;

    SearchPopup(final Harvest harvest, final Index index, final PlaceManager placeManager) {
        super(Arrow.TOP);
        this.harvest = harvest;
        this.index = index;

        deck = new DeckPanel();

        VerticalPanel indexPanel = new VerticalPanel();
        indexPanel.setStyleName("fill-layout-width");
        indexPanel.getElement().getStyle().setPadding(2, Style.Unit.PX);
        indexPanel.add(new HTML(Console.MESSAGES.search_index_pending()));
        progressBar = new ProgressElement();
        indexPanel.add(progressBar);
        deck.add(indexPanel);

        VerticalPanel searchPanel = new VerticalPanel();
        searchPanel.setStyleName("fill-layout-width");
        IndexSuggestOracle oracle = new IndexSuggestOracle(this.index);
        TextBox textBox = new TextBox();
        textBox.getElement().setAttribute("placeholder", Console.CONSTANTS.search_placeholder());
        searchBox = new SuggestBox(oracle, textBox);
        searchBox.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(final SelectionEvent<SuggestOracle.Suggestion> event) {
                hide();
                DocumentSuggestion suggestion = (DocumentSuggestion) event.getSelectedItem();
                Document document = suggestion.getDocument();
                placeManager.revealPlace(new PlaceRequest.Builder().nameToken(document.getToken()).build());
            }
        });
        searchBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(final KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                    SuggestBox.DefaultSuggestionDisplay display = (SuggestBox.DefaultSuggestionDisplay) searchBox
                            .getSuggestionDisplay();
                    display.hideSuggestions();
                }
            }
        });
        searchPanel.add(searchBox);
        deck.add(searchPanel);

        deck.showWidget(1);
        setWidget(deck);
        addStyleName("hal-searchPopup");
    }

    @Override
    protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
        if (Event.ONKEYDOWN == event.getTypeInt()) {
            if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                // Dismiss when escape is pressed
                hide();
            }
        }
    }

    void showIndexPage() {
        setHeight(INDEXING_HEIGHT);
        deck.showWidget(0);
        show();
    }

    void index() {
        harvest.run(new FilterDuplicatesHarvest(index, this), progressBar);
    }

    void showSearchPage() {
        setHeight(SEARCH_HEIGHT);
        searchBox.setValue("");
        deck.showWidget(1);
        show();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                searchBox.setFocus(true);
            }
        });
    }
}
