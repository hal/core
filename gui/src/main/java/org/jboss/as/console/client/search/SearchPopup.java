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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.threads.model.ScheduledThreadPool;
import org.jboss.as.console.client.widgets.lists.DefaultCellList;
import org.jboss.as.console.client.widgets.progress.ProgressElement;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Harald Pehl
 */
class SearchPopup extends DefaultWindow {

    private final Harvest harvest;
    private final Index index;

    private final DeckPanel deck;
    private final ProgressElement progressBar;
    private final DefaultCellList<DocumentSuggestion> resultList;
    private final SingleSelectionModel<DocumentSuggestion> resultSelectionModel;
    private final ListDataProvider<DocumentSuggestion> resultProvider;
    private final TextBox textBox;
    private final PlaceManager placeManager;

    SearchPopup(final Harvest harvest, final Index index, final PlaceManager placeManager) {
        super("Search");
        this.harvest = harvest;
        this.index = index;
        this.placeManager = placeManager;

        deck = new DeckPanel();

        VerticalPanel indexPanel = new VerticalPanel();
        indexPanel.setStyleName("fill-layout-width");
        indexPanel.getElement().getStyle().setPadding(2, Style.Unit.PX);
        indexPanel.add(new HTML(Console.MESSAGES.search_index_pending()));
        progressBar = new ProgressElement();
        indexPanel.add(progressBar);
        deck.add(indexPanel);

        VerticalPanel searchPanel = new VerticalPanel();
        searchPanel.setStyleName("window-content");
        textBox = new TextBox();
        textBox.setStyleName("fill-layout-width");
        textBox.getElement().setAttribute("placeholder", "Search term ...");

        textBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                if(keyUpEvent.isDownArrow() && !resultProvider.getList().isEmpty())
                {
                    resultList.setFocus(true);
                }
                else
                {
                    executeQuery();
                }
            }
        });

        textBox.setTabIndex(0);
        searchPanel.add(textBox);

        resultList = new DefaultCellList<DocumentSuggestion>(new ResultCell());

        resultList.addCellPreviewHandler(new CellPreviewEvent.Handler() {
            @Override
            public void onCellPreview(CellPreviewEvent event) {

                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                    resultSelectionModel.setSelected(
                            resultProvider.getList().get(resultList.getKeyboardSelectedRow()
                            ), true);
                }
            }
        });

        resultSelectionModel = new SingleSelectionModel<DocumentSuggestion>();
        resultSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                navigate();
            }
        });

        resultList.setSelectionModel(resultSelectionModel);
        resultList.addStyleName("fill-layout-width");
        resultList.addStyleName("search-result");

        resultProvider = new ListDataProvider<DocumentSuggestion>();
        resultProvider.addDataDisplay(resultList);
        searchPanel.add(resultList);

        deck.add(searchPanel);

        deck.showWidget(1);
        resultList.setTabIndex(0);


        setWidget(new ScrollPanel(deck));

        /*addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                textBox.setText("");
                resultProvider.setList(Collections.EMPTY_LIST);
            }
        });*/
    }

    private void navigate() {
        DocumentSuggestion selection = resultSelectionModel.getSelectedObject();
        if(selection!=null)
        {
            hide();
            Document document = selection.getDocument();
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken(document.getToken()).build());

        }
    }

    private void executeQuery() {

        String query = textBox.getText().trim();
        if (query.length() != 0) {
            List<Document> hits = index.search(query);
            List<DocumentSuggestion> suggestions = new ArrayList<DocumentSuggestion>();
            for (Document hit : hits) {
                String description = hit.getDescription();
                boolean tooLong = description.length() > 250;
                String shortDesc = tooLong ? description.substring(0, 125) + "..." : description;
                String display = tooLong ? "<span title=\"" + description + "\">" + shortDesc + "</span>" : description;
                DocumentSuggestion suggestion = new DocumentSuggestion(hit, description,
                        display + " <span class=\"hit-token\">(#" + hit.getToken() + ")</span>");
                suggestions.add(suggestion);
            }
            resultProvider.setList(suggestions);
            resultProvider.refresh();
        }
        else
        {
            // clear display
            resultProvider.setList(Collections.EMPTY_LIST);
            resultProvider.refresh();
        }
    }

    void showIndexPage() {
        deck.showWidget(0);
    }

    void index() {
        harvest.run(new FilterDuplicatesHarvest(index, this), progressBar);
    }

    void showSearchPage() {
        deck.showWidget(1);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                textBox.setFocus(true);
            }
        });
    }

    public class ResultCell extends AbstractCell<DocumentSuggestion> {

        @Override
        public void render(
                Context context,
                DocumentSuggestion document,
                SafeHtmlBuilder safeHtmlBuilder)
        {
            safeHtmlBuilder.appendHtmlConstant(document.getDisplayString());
        }

    }

}
