package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.as.console.mbui.widgets.ModelDrivenWidget;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 07/04/15
 */
public class ConnectorMetricView extends ModelDrivenWidget {
    private static final String BASE_ADDRESS = "{selected.profile}/subsystem=undertow/server={selected.server}";

    private final HttpMetricPresenter presenter;
    private final DefaultCellTable table;
    private final ListDataProvider<Property> dataProvider;
    private Column[] columns;
    private Grid grid;

    public ConnectorMetricView(HttpMetricPresenter presenter) {
        super(BASE_ADDRESS);
        this.presenter = presenter;
        this.table = new DefaultCellTable(5);

        ProvidesKey<Property> keyProvider = new ProvidesKey<Property>() {
            @Override
            public Object getKey(Property property) {
                return property.getName();
            }
        };

        this.dataProvider = new ListDataProvider<Property>(keyProvider);
        this.dataProvider.addDataDisplay(table);
        this.table.setSelectionModel(new SingleSelectionModel<Property>(keyProvider));
    }

    @Override
    public Widget buildWidget(ResourceAddress address, ResourceDefinition definition) {
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        TextColumn<Property> socketColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getValue().get("socket-binding").asString();
            }
        };


        TextColumn<Property> enabledColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return String.valueOf(node.getValue().get("enabled").asBoolean());
            }
        };

        table.addColumn(nameColumn, "Name");
        table.addColumn(socketColumn, "Socket Binding");
        table.addColumn(enabledColumn, "Is Enabled?");

        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");



        final SingleSelectionModel<Property> selectionModel = new SingleSelectionModel<Property>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property selection = selectionModel.getSelectedObject();
                if(selection!=null)
                {
                    ModelNode payload = selection.getValue();
                    updateFrom(payload);

                }
            }
        });
        table.setSelectionModel(selectionModel);


        columns = new Column[] {
                new NumberColumn("request-count", "Request Count"),
                new NumberColumn("error-count","Error Count"),
                new NumberColumn("bytes-sent","Bytes Send"),
                new NumberColumn("bytes-received", "Bytes Received")
        };

        grid = new Grid(columns.length, 2);
        grid.addStyleName("metric-grid");

        // format
        for (int row = 0; row < columns.length; ++row) {
            grid.getCellFormatter().addStyleName(row, 0,  "nominal");
            grid.getCellFormatter().addStyleName(row, 1, "numerical");
        }

        VerticalPanel desc = new VerticalPanel();
        desc.addStyleName("metric-container");
        desc.add(new HTML("<h3 class='metric-label-embedded'>HTTP Requests</h3>"));
        desc.add(grid);


        HTML refreshBtn = new HTML("<i class='icon-refresh'></i> Refresh Results");
               refreshBtn.setStyleName("html-link");
               refreshBtn.getElement().getStyle().setPosition(Style.Position.RELATIVE);
               refreshBtn.getElement().getStyle().setTop(40, Style.Unit.PX);
               refreshBtn.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
               refreshBtn.getElement().getStyle().setFloat(Style.Float.RIGHT);
               refreshBtn.getElement().getStyle().setLeft(80, Style.Unit.PCT);

        refreshBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.loadDetails();
            }
        });

        // ----
        SimpleLayout layoutBuilder = new SimpleLayout()
                .setPlain(true)
                .setHeadline("HTTP Connectors")
                .setDescription("")
                        //.setMasterTools(tools)
                .addContent(Console.MESSAGES.available("HTTP Connectors"), table)
                .addContent("", refreshBtn)
                .addContent("Metrics", desc);


        return layoutBuilder.build();
    }

    public void clearSamples() {
        for(int i=0; i<columns.length;i++)
        {
            grid.setText(i, 0, columns[i].getLabel());
            grid.setText(i, 1, "0");
        }

    }

    public void updateFrom(ModelNode metrics) {

        List<Property> atts = metrics.asPropertyList();

        for(int i=0; i<columns.length; i++)
        {
            for(Property att : atts)
            {
                if(att.getName().equals(columns[i].getDeytpedName()))
                {
                    grid.setText(i, 0, columns[i].getLabel());
                    grid.setText(i, 1, att.getValue().asString());
                }
            }
        }

    }

    public void setData(List<Property> data) {
        dataProvider.setList(data);
        table.selectDefaultEntity();
    }
}
