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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.Sampler;
import org.jboss.as.console.client.shared.runtime.charts.BulletGraphView;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.runtime.plain.PlainColumnView;
import org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 10/25/11
 */
class TXExecutionView implements Sampler {

    private TransactionPresenter presenter;
    private Sampler sampler = null;

    TXExecutionView() {
    }

    public Widget asWidget() {
        return displayStrategy();
    }

    private Widget displayStrategy() {


        NumberColumn total = new NumberColumn("number-of-transactions","Number of Transactions");

        Column[] cols = new Column[] {
                total.setBaseline(true),
                new NumberColumn("number-of-committed-transactions","Committed").setComparisonColumn(total),
                new NumberColumn("number-of-aborted-transactions","Aborted").setComparisonColumn(total),
                new NumberColumn("number-of-timed-out-transactions", "Timed Out").setComparisonColumn(total),
                new NumberColumn("number-of-heuristics", "Heuristics").setComparisonColumn(total)
        };

        String title = "Success Ratio";

        final HelpSystem.AddressCallback addressCallback = () -> {
            ModelNode address = new ModelNode();
            address.get(ModelDescriptionConstants.ADDRESS).set(RuntimeBaseAddress.get());
            address.get(ModelDescriptionConstants.ADDRESS).add("subsystem", "transactions");
            return address;
        };
        
        if (Console.protovisAvailable()) {
            sampler = new BulletGraphView(title, "total number", false, addressCallback)
                .setColumns(cols);
            
        } else {
            sampler = new PlainColumnView(title, addressCallback)
                .setColumns(cols)
                .setWidth(100, Style.Unit.PCT);
        }

        return sampler.asWidget();
    }

    @Override
    public void addSample(Metric metric) {
        sampler.addSample(metric);
    }

    @Override
    public void clearSamples() {
        sampler.clearSamples();
    }

    @Override
    public long numSamples() {
        return sampler.numSamples();
    }

    @Override
    public void recycle() {
        sampler.recycle();
    }
}
