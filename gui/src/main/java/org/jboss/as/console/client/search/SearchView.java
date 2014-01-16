package org.jboss.as.console.client.search;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

/**
 * @author Heiko Braun
 * @date 16/01/14
 */
public class SearchView {

    private final Index index;
    private ListDataProvider<Index.Document> dataProvider;
    private DefaultCellTable<Index.Document> documentTable;

    public SearchView() {
        this.index = Index.get();
    }

    public Widget asWidget() {
        ToolStrip tools = new ToolStrip();
        final TextBox searchBox = new TextBox();
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
        documentTable.addColumn(descColumn, "Description");

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

    private void onSearch(final String text) {
        List<Index.Document> documents = index.search(text);
        dataProvider.setList(documents);
    }
}
