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
package org.jboss.as.console.client.shared.hosts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.StringUtils;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda
 */
public class ConfigurationChangesEditor {

    private ConfigurationChangesPresenter presenter;
    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private List<ModelNode> changes = new ArrayList<>();

    private ToolButton enableBtn;
    private ToolButton disableBtn;
    private ToolButton refreshBtn;
    private TextAreaItem detailsConfigurationChange;
    private SingleSelectionModel<ModelNode> selectionModel;

    @SuppressWarnings("unchecked")
    public Widget asWidget() {

        ProvidesKey<ModelNode> providesKey = node -> node.get("operation-date").asString();
        table = new DefaultCellTable<>(20, providesKey);

        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);

        // the date / time column
        table.addColumn(new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode item) {
                // the operation-date is 2016-07-08T22:39:50.783Z
                // there is some format to facilitate user experience 
                // by replacing the T with a blank space
                String opTimestamp = item.get("operation-date").asString();
                opTimestamp = opTimestamp.replaceFirst("T", " "); 
                return opTimestamp;
            }
        }, "Date and time");
        
        // access-mechanism column
        TextColumn<ModelNode> accessMechanismColumn = createColumn("access-mechanism");
        accessMechanismColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(accessMechanismColumn, "Access Mechanism");
        
        // remote address column
        TextColumn<ModelNode> remoteAddressColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode item) {
                // the remote address is 10.10.10.10/10.10.10.10
                // to facilitate user experience we cut at at first slash
                String clientAddress = item.get("remote-address").asString();
                clientAddress = clientAddress.substring(0, clientAddress.indexOf("/"));
                return clientAddress;
            }
        };
        remoteAddressColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(remoteAddressColumn, "Remote address");
        
        // the resource address 
        TextColumn<ModelNode> resourceColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode item) {
                return StringUtils.shortenStringIfNecessary(extractResourceAddress(item), 63);
            }
        };
        table.addColumn(resourceColumn, "Resource address");
        table.setColumnWidth(resourceColumn, 50, Style.Unit.PCT);
        
        // operation column
        table.addColumn(new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode item) {
                return item.get(OPERATIONS).get(0).get(OP).asString();
            }
        }, "Operation");
        
        // result column
        table.addColumn(createColumn(OUTCOME), "Result");
        table.setTableLayoutFixed(false);

        // the details panel
        selectionModel = new SingleSelectionModel<>(providesKey);
        selectionModel.addSelectionChangeHandler(event -> {
            ModelNode changeDetails = selectionModel.getSelectedObject();
            if (changeDetails != null) {
                detailsConfigurationChange.setValue(changeDetails.toString());
            }
        });
        table.setSelectionModel(selectionModel);

        detailsConfigurationChange = new TextAreaItem("details", "Details 1", 20);
        detailsConfigurationChange.setEnabled(false);
        
        HorizontalPanel header = new HorizontalPanel();
        header.addStyleName("fill-layout-width");
        header.add(new HTML("<h3 class='metric-label-embedded'>Configuration change details</h3>"));
        
        VerticalPanel detailsPanel = new VerticalPanel();
        detailsPanel.addStyleName("metric-container");
        detailsPanel.add(header);
        detailsPanel.add(detailsConfigurationChange.asWidget());

        // ======================
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        VerticalPanel tableAndPager = new VerticalPanel();
        tableAndPager.setStyleName("fill-layout-width");
        tableAndPager.add(table);
        tableAndPager.add(pager);
        
        SimpleLayout layout = new SimpleLayout()
                .setPlain(true)
                .setHeadline("Configuration Changes")
                .setDescription(SafeHtmlUtils.fromString(Console.MESSAGES.configuration_changes_description()))
                .addContent("", toolstripButtons())
                .addContent("", tableAndPager)
                .addContent("", detailsPanel);
        
        return layout.build();
    }
    
    /**
     * Iterate over a "operations" resource and extract the resource address
     * <pre>
     {
     "operation-date" => "2016-07-11T16:00:30.930Z",
     "domain-uuid" => "7be474f5-5be9-4040-9f19-8959cf603be0",
     "access-mechanism" => "HTTP",
     "remote-address" => "127.0.0.1/127.0.0.1",
     "outcome" => "success",
     "operations" => [{
     "operation" => "composite",
     "address" => [],
     "steps" => [{
     "address" => [
     ("profile" => "default"),
     ("subsystem" => "mail"),
     ("mail-session" => "default"),
     ("server" => "smtp")
     ],
     "operation" => "write-attribute",
     "name" => "ssl",
     "value" => true
     }],
     "operation-headers" => {
     "access-mechanism" => "HTTP",
     "caller-type" => "user"
     }
     }]
     },
     
     * </pre>
     * and concatenate to the following form
     * <pre>profile = default / subsystem = mail / mail-session = default / server = smtp</pre>
     * 
     * @param changeItem The ModelNode
     * @return The formatted resource address as in <pre>profile = default / subsystem = mail / mail-session = default / server = smtp</pre> 
     */
    private String extractResourceAddress(ModelNode changeItem) {
        StringBuilder address = new StringBuilder();
        ModelNode operations = changeItem.get(OPERATIONS);
        for (ModelNode op1 : operations.asList()) {
            String opName = op1.get(OP).asString();
            if (COMPOSITE.equals(opName)) {

                List<ModelNode> steps = op1.get(STEPS).asList();
                for (int idxStep = 0; idxStep < steps.size(); idxStep++) {
                    ModelNode step = steps.get(idxStep);
                    if (step.hasDefined(OP_ADDR)) {
                        ModelNode addressNode = step.get(OP_ADDR);
                        List<ModelNode> modelNodes = addressNode.asList();
                        for (int i = 0; i < modelNodes.size(); i++) {
                            ModelNode addr = modelNodes.get(i);
                            Property p = addr.asProperty();
                            address.append(p.getName()).append(" = ").append(p.getValue().asString());
                            if (i + 1 < modelNodes.size())
                                address.append(" / ");
                        }
                    }
                    // separates each step resource address
                    if (idxStep + 1 < steps.size())
                        address.append(" | ");
                }

            } else {
                if (op1.hasDefined(OP_ADDR)) {
                    ModelNode addressNode = op1.get(OP_ADDR);
                    List<ModelNode> modelNodes = addressNode.asList();
                    for (int i = 0; i < modelNodes.size(); i++) {
                        ModelNode addr = modelNodes.get(i);
                        Property p = addr.asProperty();
                        address.append(p.getName()).append(" = ").append(p.getValue().asString());
                        if (i + 1 < modelNodes.size())
                            address.append(" / ");
                    }
                }
            }
        }
        return address.toString();
    }
    
    private ToolStrip toolstripButtons() {

        final TextBox filter = new TextBox();
        filter.setMaxLength(30);
        filter.setVisibleLength(20);
        filter.getElement().setAttribute("style", "float:right; width:120px;");
        filter.addKeyUpHandler(keyUpEvent -> {
            String word = filter.getText();
            if (word != null && word.trim().length() > 0) {
                filter(word);
            } else {
                clearFilter();
            }
        });

        ToolStrip topLevelTools = new ToolStrip();

        final HTML label = new HTML(Console.CONSTANTS.commom_label_filter()+":&nbsp;");
        label.getElement().setAttribute("style", "padding-top:8px;");
        topLevelTools.addToolWidget(label);
        topLevelTools.addToolWidget(filter);

        enableBtn = new ToolButton(Console.CONSTANTS.common_label_enable(), event -> presenter.enable());
        disableBtn = new ToolButton(Console.CONSTANTS.common_label_disable(), event -> presenter.disable());
        refreshBtn = new ToolButton(Console.CONSTANTS.common_label_refresh(), event -> presenter.loadChanges());
        
        topLevelTools.addToolButtonRight(enableBtn);
        topLevelTools.addToolButtonRight(disableBtn);
        topLevelTools.addToolButtonRight(refreshBtn);
        
        return topLevelTools;
    }

    private void filter(String word) {
        final List<ModelNode> filteredChanges  = new ArrayList<>();
        word = word.toLowerCase();
        for(ModelNode node : changes) {
            String access = node.get("access-mechanism").asString().toLowerCase();
            String clientAddress = node.get("remote-address").asString().toLowerCase();
            String outcome = node.get("outcome").asString().toLowerCase();
            String address = extractResourceAddress(node).toLowerCase();
            String opname = node.get(OPERATIONS).get(0).get(OP).asString().toLowerCase();
            
            if (access.contains(word)
                    || clientAddress.contains(word)
                    || outcome.contains(word)
                    || address.contains(word)
                    || opname.contains(word))
                filteredChanges.add(node);
        }
        dataProvider.setList(filteredChanges);
    }

    private void clearFilter() {
        dataProvider.setList(changes);
    }

    private TextColumn<ModelNode> createColumn(String attribute) {
        return new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode item) {
                return item.get(attribute).asString();
            }
        };
    }

    public void updateChanges(List<ModelNode> _changes) {
        dataProvider.setList(_changes);
        table.selectDefaultEntity();
        this.changes.clear();
        this.changes.addAll(_changes);
        detailsConfigurationChange.clearValue();
    }

    public void setEnabled(final boolean enabled) {
        if (enabled) {
            enableBtn.setVisible(false);
            disableBtn.setVisible(true);
            refreshBtn.setEnabled(true);
        } else {
            enableBtn.setVisible(true);
            disableBtn.setVisible(false);
            refreshBtn.setEnabled(false);
            dataProvider.setList(Collections.emptyList());
        }
        detailsConfigurationChange.clearValue();
    }

    public void setPresenter(final ConfigurationChangesPresenter presenter) {
        this.presenter = presenter;
    }
}
