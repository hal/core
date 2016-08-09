package org.jboss.as.console.client.shared.runtime.ws;

import java.util.Comparator;
import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.Sampler;
import org.jboss.as.console.client.shared.runtime.charts.BulletGraphView;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.subsys.ws.model.WebServiceEndpoint;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.client.widgets.tables.ColumnSortHandler;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 1/23/12
 */
public class WebServiceRuntimeView extends SuspendableViewImpl implements WebServiceRuntimePresenter.MyView {

    private ListDataProvider<WebServiceEndpoint> dataProvider;
    private ColumnSortHandler<WebServiceEndpoint> sortHandler;
    private Sampler sampler;
    private Column[] columns;
    private WebServiceRuntimePresenter presenter;
    private ContentDescription description;

    private ContentDescription statsText = new ContentDescription("Statistics status.");

    @Override
    public void setPresenter(WebServiceRuntimePresenter presenter) {
        this.presenter = presenter;
    }

    private DefaultCellTable<WebServiceEndpoint> table;

    @SuppressWarnings("unchecked")
    public Widget createWidget() {

        ProvidesKey<WebServiceEndpoint> keyProvider = new ProvidesKey<WebServiceEndpoint>() {
            @Override
            public Object getKey(final WebServiceEndpoint item) {
                return item.getDeployment() + "@" + item.getName() + ":" + item.getClassName();
            }
        };
        table = new DefaultCellTable<WebServiceEndpoint>(10, keyProvider);
        sortHandler = new ColumnSortHandler<WebServiceEndpoint>();

        dataProvider = new ListDataProvider<WebServiceEndpoint>();
        dataProvider.addDataDisplay(table);

        TextColumn<WebServiceEndpoint> nameCol = new TextColumn<WebServiceEndpoint>() {
            @Override
            public String getValue(WebServiceEndpoint object) {
                return object.getName();
            }
        };
        nameCol.setSortable(true);
        sortHandler.setComparator(nameCol, new Comparator<WebServiceEndpoint>() {
            @Override
            public int compare(WebServiceEndpoint o1, WebServiceEndpoint o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        TextColumn<WebServiceEndpoint> contextCol = new TextColumn<WebServiceEndpoint>() {
            @Override
            public String getValue(WebServiceEndpoint object) {
                return object.getContext();
            }
        };
        contextCol.setSortable(true);
        sortHandler.setComparator(contextCol, new Comparator<WebServiceEndpoint>() {
            @Override
            public int compare(WebServiceEndpoint o1, WebServiceEndpoint o2) {
                return o1.getContext().compareTo(o2.getContext());
            }
        });
        TextColumn<WebServiceEndpoint> deploymentCol = new TextColumn<WebServiceEndpoint>() {
            @Override
            public String getValue(WebServiceEndpoint object) {
                return object.getDeployment();
            }
        };
        deploymentCol.setSortable(true);
        sortHandler.setComparator(deploymentCol, new Comparator<WebServiceEndpoint>() {
            @Override
            public int compare(WebServiceEndpoint o1, WebServiceEndpoint o2) {
                return o1.getDeployment().compareTo(o2.getDeployment());
            }
        });

        table.addColumn(nameCol, "Name");
        table.addColumn(contextCol, "Context");
        table.addColumn(deploymentCol, "Deployment");

        table.addColumnSortHandler(sortHandler);
        table.getColumnSortList().push(nameCol); // initial sort is on name

        final SingleSelectionModel<WebServiceEndpoint> selectionModel = new SingleSelectionModel(keyProvider);
        table.setSelectionModel(selectionModel);

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                final WebServiceEndpoint selection = selectionModel.getSelectedObject();
                if(selection!=null) {
                    sampler.clearSamples();
                    sampler.addSample(
                            new Metric(
                                    selection.getRequestCount(),
                                    selection.getResponseCount(),
                                    selection.getFaultCount()
                            ));
                }
                else
                {
                    sampler.clearSamples();
                }
            }
        });
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        VerticalPanel tableLayout = new VerticalPanel();
        tableLayout.setStyleName("fill-layout-width");
        tableLayout.add(table);
        tableLayout.add(pager);

        // -----

        final ModelNode helpAddress = RuntimeBaseAddress.get().add("deployment", "*").add("subsystem", "webservices");

        Form<WebServiceEndpoint> basics = new Form<WebServiceEndpoint>(WebServiceEndpoint.class);

        TextItem nameItem = new TextItem("name", "Name");
        TextItem contextItem = new TextItem("context", "Context");
        TextItem classItem = new TextItem("className", "Class");
        TextItem typeItem = new TextItem("type", "Type");
        TextItem wsdlItem = new TextItem("wsdl", "WSDL Url");
        TextItem dplItem = new TextItem("deployment", "Deployment");

        basics.setFields(nameItem, contextItem, classItem, typeItem, wsdlItem, dplItem);
        basics.bind(table);
        basics.setEnabled(false);

        FormLayout basicsLayout = new FormLayout()
                .setHelp(new FormHelpPanel(new FormHelpPanel.AddressCallback() {
                            @Override
                            public ModelNode getAddress() {
                                return helpAddress;
                            }
                        }, basics)
                )
                .setForm(basics);

        columns = new Column[] {
                new NumberColumn("request-count", "Number of request").setBaseline(true),
                new NumberColumn("response-count","Responses"),
                new NumberColumn("fault-count","Faults")
        };


        HTML refreshBtn = new HTML("<i class='icon-refresh'></i> Refresh Results");
        refreshBtn.setStyleName("html-link");
        refreshBtn.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
        refreshBtn.getElement().getStyle().setFloat(Style.Float.RIGHT);
        refreshBtn.getElement().getStyle().setLeft(80, Style.Unit.PCT);

        refreshBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.loadEndpoints(true);
            }
        });


        sampler = new BulletGraphView("Web Service Requests", "total number", true)
                        .setColumns(columns);

        VerticalPanel p = new VerticalPanel();
        p.setStyleName("fill-layout-width");
        p.add(refreshBtn);
        p.add(statsText);
        p.add(sampler.asWidget());

        description = new ContentDescription(Console.CONSTANTS.subsys_ws_endpoint_desc());
        
        OneToOneLayout layout = new OneToOneLayout()
                .setTitle("Webservices")
                .setHeadline("Web Service Endpoints")
                .setMaster(Console.MESSAGES.available("Web Service Endpoints"), tableLayout)
                .setDescription(description)
                .addDetail(Console.CONSTANTS.common_label_stats(), p)
                .addDetail(Console.CONSTANTS.common_label_attributes(), basicsLayout.build());


        return layout.build();
    }

    public void setStatistcsEnabled(final boolean stats) {
        if (stats) {
            statsText.setText("Status: ON");
        } else {
            statsText.setText("Status: OFF");
        }
    }

    public void updateEndpoints(List<WebServiceEndpoint> endpoints) {
        ((SingleSelectionModel)table.getSelectionModel()).clear();

        dataProvider.setList(endpoints);
        sortHandler.setList(dataProvider.getList());

        // Make sure the new values are properly sorted
        ColumnSortEvent.fire(table, table.getColumnSortList());

        table.selectDefaultEntity();

       /* ((SingleSelectionModel)table.getSelectionModel()).clear();
        dataProvider.setList(endpoints);
        table.selectDefaultEntity();*/

    }

    @Override
    public void setWiseUrl(final String url) {
        String wiseText = "<br><br><span title=\"" + Console.CONSTANTS.subsys_ws_wise_title_description() 
                + "\">WISE Url</span>: <a href=\"" + url + "\" target=\"_blank\">"+ url + "</a>";
        description.setHTML(description.getText() + wiseText);
    }
}
