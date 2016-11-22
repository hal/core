package org.jboss.as.console.client.shared.runtime.activemq;

import java.util.List;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import static org.jboss.dmr.client.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

/**
 * @author Claudio Miranda
 * @date 11/18/16
 */
public class PooledConnectionFactoryRuntimeView {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> provider;
    private ActivemqMetricPresenter presenter;
    private SecurityContext securityContext;
    protected SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets modelForm;
    private VerticalPanel formPanel;

    public PooledConnectionFactoryRuntimeView(ActivemqMetricPresenter presenter) {
        this.presenter = presenter;
        securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
    }

    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        ProvidesKey<Property> providesKey = Property::getName;
        table = new DefaultCellTable<>(10, providesKey);
        provider = new ListDataProvider<>(providesKey);
        provider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<>(providesKey);

        setupTable();

        formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(SafeHtmlUtils.fromString(Console.CONSTANTS.subsys_messaging_pooled_stats_desc()))
                .setMaster(Console.MESSAGES.available("Pooled Connection Factory"), table)
                .setMasterTools(setupMasterTools())
                .addDetail("Pool Statistics", formPanel.asWidget());

        return layout.build();
    }
    
    @SuppressWarnings("unchecked")
    private void setupTable() {
        Column<Property, String> name = new Column<Property, String>(new TextCell()) {
            @Override
            public String getValue(Property object) {
                return object.getName();
            }
        };
        Column<Property, String> jndi = new Column<Property, String>(new TextCell()) {
            @Override
            public String getValue(Property object) {
                return object.getValue().get("entries").asString().replace("\"", "");
            }
        };
        Column<Property, String> stats = new Column<Property, String>(new TextCell()) {
            @Override
            public String getValue(Property object) {
                return object.getValue().get("statistics-enabled").asString();
            }
        };

        table.addColumn(name, "Name");
        table.addColumn(jndi, "JNDI");
        table.addColumn(stats, "Statistics Enabled");
        table.setColumnWidth(name, 30, Style.Unit.PCT);
        table.setColumnWidth(jndi, 50, Style.Unit.PCT);
        table.setColumnWidth(stats, 20, Style.Unit.PCT);
        name.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        jndi.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        stats.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        selectionModel.addSelectionChangeHandler(event -> {
            Property selected = selectionModel.getSelectedObject();

            if (selected != null) {

                boolean statistics = selected.getValue().get("statistics-enabled").asBoolean();
                if (statistics) {
                    // read the descriptor under demand, as the statistics=pool resource only exists when the 
                    // pooled connection factory has statistics-enabled=true, otherwise the @RequiredResource
                    // would not work
                    String address = "/{implicit.host}/{selected.server}/subsystem=messaging-activemq/server=" + presenter.getCurrentServer() 
                            + "/pooled-connection-factory=" + selected.getName() + "/statistics=pool";
                    org.jboss.as.console.client.v3.dmr.ResourceAddress resAddress = AddressTemplate.of(address)
                            .resolve(presenter.getStatementContext());
                    Operation op = new Operation.Builder(READ_RESOURCE_DESCRIPTION_OPERATION, resAddress)
                            .param(INCLUDE_RUNTIME, true)
                            .build();

                    presenter.getDispatcher().execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
                        @Override
                        public void onSuccess(DMRResponse result) {
                            ModelNode response = result.get();

                            if (response.isFailure()) {
                                Console.error(Console.MESSAGES.failed("load address " + address),
                                        response.getFailureDescription());
                            } else {
                                ResourceDescription pooledStatsDescription = new ResourceDescription(response.get(RESULT));
                                ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                                        .setResourceDescription(pooledStatsDescription)
                                        .setSecurityContext(securityContext);
                                modelForm = formBuilder.build();
                                formPanel.clear();
                                formPanel.add(modelForm.getHelp().asWidget());
                                formPanel.add(modelForm.getForm().asWidget());
                                modelForm.getForm().edit(selected.getValue().get("statistics").get("pool"));
                            }
                        }
                    });
                    
                } else {
                    if (modelForm != null)
                        modelForm.getForm().clearValues();
                }
            } else {
                if (modelForm != null)
                    modelForm.getForm().clearValues();
                formPanel.clear();
            }
        });
        table.setSelectionModel(selectionModel);
    }
    
    private ToolStrip setupMasterTools() {
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_refresh(), clickEvent ->
                        refresh()
                ));

        return tools;
    }

    public void setModel(List<Property> models) {
        provider.setList(models);
        table.selectDefaultEntity();
        selectionModel.clear();
        serverName.setText("Pooled Connection Factory: Provider " + presenter.getCurrentServer());
    }
    
    private void refresh() {
        presenter.loadPooledConnectionFactory(presenter.getCurrentServer());
    }

}
