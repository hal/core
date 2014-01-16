package org.jboss.as.console.client.search;

import java.util.List;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.widgets.tables.TextLinkCell;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;

/**
 * @author Heiko Braun
 * @date 16/01/14
 */
public class SearchView {

    private final Index index;
    private final DefaultWindow indexWindow;
    private ListDataProvider<Index.Document> dataProvider;
    private DefaultCellTable<Index.Document> documentTable;

    public SearchView(final DefaultWindow indexWindow) {
        this.index = Index.get();
        this.indexWindow = indexWindow;
    }

    public Widget asWidget() {
        ToolStrip tools = new ToolStrip();
        final TextBox searchBox = new TextBox();
        searchBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(final KeyUpEvent event) {
                boolean enterPressed = KeyCodes.KEY_ENTER == event.getNativeEvent().getKeyCode();
                if (enterPressed) {
                    onSearch(searchBox.getText());
                }
            }
        });
        tools.addToolWidget(searchBox);
        tools.addToolButtonRight(new ToolButton("Search", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                onSearch(searchBox.getText());
            }
        }));

        documentTable = new DefaultCellTable<Index.Document>(8,
                new ProvidesKey<Index.Document>() {
                    @Override
                    public Object getKey(Index.Document document) {
                        return document.getId();
                    }
                });
        TextColumn<Index.Document> descColumn = new
                TextColumn<Index.Document>() {
                    @Override
                    public String getValue(final Index.Document document) {
                        return document.getDescription();
                    }
                };
        TextColumn<Index.Document> tokenColumn = new
                TextColumn<Index.Document>() {
                    @Override
                    public String getValue(final Index.Document document) {
                        return document.getToken();
                    }
                };
        Column<Index.Document, String> viewColumn = new Column<Index.Document, String>(
                new TextLinkCell<String>("View", new ActionCell.Delegate<String>() {
                    @Override
                    public void execute(final String token) {
                        onToken(token);
                    }
                })) {
            @Override
            public String getValue(final Index.Document document) {
                return document.getToken();
            }
        };

        documentTable.addColumn(descColumn, "Description");
        documentTable.setColumnWidth(descColumn, 65, Style.Unit.PCT);

        documentTable.addColumn(tokenColumn, "Token");
        documentTable.setColumnWidth(tokenColumn, 15, Style.Unit.PCT);

        documentTable.addColumn(viewColumn, "");
        documentTable.setColumnWidth(viewColumn, 10, Style.Unit.PCT);

        dataProvider = new ListDataProvider<Index.Document>();
        dataProvider.addDataDisplay(documentTable);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(documentTable);

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");
        layout.add(tools);
        layout.add(documentTable);
        layout.add(pager);
        return layout;
    }

    private void onToken(final String token) {
        indexWindow.hide();
        Console.MODULES.getPlaceManager().revealPlace(new PlaceRequest.Builder().nameToken(token).build(), true);
    }

    private void onSearch(final String text) {
        List<Index.Document> documents = index.search(text);
        dataProvider.setList(documents);
    }
}
