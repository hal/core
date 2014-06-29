package org.jboss.as.console.client.shared.runtime.ws;

import java.util.Comparator;
import java.util.List;

import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.subsys.ws.model.WebServiceEndpoint;
import org.jboss.as.console.client.widgets.tables.ColumnSortHandler;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
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

    @Override
    public void setPresenter(WebServiceRuntimePresenter presenter) {
    }

    private DefaultCellTable<WebServiceEndpoint> table;

    @SuppressWarnings("unchecked")
    public Widget createWidget() {

        table = new DefaultCellTable<WebServiceEndpoint>(10, new ProvidesKey<WebServiceEndpoint>() {
            @Override
            public Object getKey(final WebServiceEndpoint item) {
                return item.getDeployment() + "@" + item.getName() + ":" + item.getClassName();
            }
        });
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

        Form<WebServiceEndpoint> stats = new Form<WebServiceEndpoint>(WebServiceEndpoint.class);
        NumberBoxItem requestCount = new NumberBoxItem("requestCount", "Requests");
        NumberBoxItem responseCount = new NumberBoxItem("responseCount", "Responses");
        NumberBoxItem faultCount = new NumberBoxItem("faultCount", "Faults");
        NumberBoxItem min = new NumberBoxItem("minProcessingTime", "Min. Processing Time");
        NumberBoxItem avg = new NumberBoxItem("averageProcessingTime", "Average Processing Time");
        NumberBoxItem max = new NumberBoxItem("maxProcessingTime", "Max. Processing Time");
        NumberBoxItem total = new NumberBoxItem("totalProcessingTime", "Total Processing Time");
        stats.setFields(requestCount, responseCount, faultCount, min, avg, max, total);
        stats.bind(table);
        stats.setEnabled(false);

        FormLayout statsLayout = new FormLayout()
                .setHelp(new FormHelpPanel(new FormHelpPanel.AddressCallback() {
                            @Override
                            public ModelNode getAddress() {
                                return helpAddress;
                            }
                        }, stats)
                )
                .setForm(stats);

        //final StaticHelpPanel helpPanel = new StaticHelpPanel(WebServiceDescriptions.getEndpointDescription());

        OneToOneLayout layout = new OneToOneLayout()
                .setTitle("Webservices")
                .setHeadline("Web Service Endpoints")
                .setMaster(Console.MESSAGES.available("Web Service Endpoints"), tableLayout)
                .setDescription(Console.CONSTANTS.subsys_ws_endpoint_desc())
                .addDetail(Console.CONSTANTS.common_label_attributes(), basicsLayout.build())
                .addDetail(Console.CONSTANTS.common_label_stats(), statsLayout.build());

        return layout.build();
    }

    public void updateEndpoints(List<WebServiceEndpoint> endpoints) {
        dataProvider.setList(endpoints);
        sortHandler.setList(dataProvider.getList());

        // Make sure the new values are properly sorted
        ColumnSortEvent.fire(table, table.getColumnSortList());

        table.selectDefaultEntity();
    }
}
