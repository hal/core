/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.runtime.tx;

import java.util.List;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.help.MetricHelpPanel;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.runtime.charts.TextColumn;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

/**
 * @author Claudio Miranda
 */
class GeneralView implements IsWidget {

    private Grid grid;
    private Column[] columns;

    public Widget asWidget() {

        columns = new Column[]{
                new TextColumn("Status", "OFF"),
                new NumberColumn("number-of-inflight-transactions", "Inflight Transactions"),
                new NumberColumn("number-of-nested-transactions", "Nested Transactions")
        };
        grid = new Grid(columns.length, 2);
        grid.addStyleName("metric-grid");

        for (int row = 0; row < columns.length; ++row) {
            grid.getCellFormatter().addStyleName(row, 0, "nominal");
            grid.getCellFormatter().addStyleName(row, 1, "numerical");
        }
        grid.setText(0, 0, "Status");

        HorizontalPanel header = new HorizontalPanel();
        header.addStyleName("fill-layout-width");
        header.add(new HTML("<h3 class='metric-label'>General Statistics</h3>"));

        final HelpSystem.AddressCallback addressCallback = () -> {
            ModelNode address = new ModelNode();
            address.get(ModelDescriptionConstants.ADDRESS).set(RuntimeBaseAddress.get());
            address.get(ModelDescriptionConstants.ADDRESS).add("subsystem", "transactions");
            return address;
        };

        MetricHelpPanel helpPanel = new MetricHelpPanel(addressCallback, this.columns);

        VerticalPanel metricsPanel = new VerticalPanel();
        metricsPanel.addStyleName("metric-container");
        metricsPanel.add(header);
        metricsPanel.add(helpPanel.asWidget());
        metricsPanel.add(grid);

        return metricsPanel.asWidget();
    }

    void setGeneralMetric(ModelNode metric) {
        if (metric != null) {
            List<Property> attributes = metric.asPropertyList();
            for (int i = 1; i < columns.length; i++) {
                for (Property attribute : attributes) {
                    if (attribute.getName().equals(columns[i].getDeytpedName())) {
                        grid.setText(i, 0, columns[i].getLabel());
                        grid.setText(i, 1, attribute.getValue().asString());
                    }
                }
            }
        }
    }

    public void clear() {
        grid.clear();
    }

    public void setStatistcsEnabled(final boolean stats) {
        if (stats) {
            grid.setText(0, 1, "ON");
        } else {
            grid.setText(0, 1, "OFF");
        }
    }

}
