package org.jboss.as.console.client.search;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

/**
 * @author Heiko Braun
 * @date 16/01/14
 */
public class IndexBuilderView {

    private DefaultCellTable<Asset> table;
    private ListDataProvider<Asset> dataProvider;

    class Asset {
        String token;
        String address;

        Asset(String token, String address) {
            this.token = token;
            this.address = address;
        }

        String getToken() {
            return token;
        }

        String getAddress() {
            return address;
        }
    }
    public Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        final Harvest harvest = Console.MODULES.getHarvest();

        // ----------

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton("Build", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                final List<Asset> data = new ArrayList<Asset>();

                harvest.run(new Harvest.Handler() {

                    @Override
                    public void onStart() {
                        Index.get().reset();
                        dataProvider.setList(data);
                    }

                    @Override
                    public void onHarvest(String token, String address) {
                        data.add(new Asset(token, address));
                        //dataProvider.flush();

                        dataProvider.setList(data);

                    }

                    @Override
                    public void onFinish() {
                        Console.info("Successfully build index!");
                    }

                    @Override
                    public void onError(Throwable t) {
                        Console.error("Failed to build index", t.getMessage());
                    }
                });
            }
        }));


        /*tools.addToolButton(new ToolButton("View Index", new ClickHandler(){
            @Override
            public void onClick(ClickEvent event) {

            }
        } ));*/

        layout.add(tools.asWidget());

        // ----------

        table = new DefaultCellTable<Asset>(
                12,
                new ProvidesKey<Asset>() {
                    @Override
                    public Object getKey(Asset item) {
                        return item.getAddress();
                    }
                });

        dataProvider = new ListDataProvider<Asset>();
        dataProvider.addDataDisplay(table);

        TextColumn<Asset> tokenCol = new TextColumn<Asset>() {
            @Override
            public String getValue(Asset asset) {
                return asset.getToken();
            }
        };

        TextColumn<Asset> addressCol = new TextColumn<Asset>() {
                   @Override
                   public String getValue(Asset asset) {
                       return asset.getAddress();
                   }
               };
        table.addColumn(tokenCol, "Token");
        table.addColumn(addressCol, "Resource");


        layout.add(table.asWidget());

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        layout.add(pager);

        return layout;
    }
}
