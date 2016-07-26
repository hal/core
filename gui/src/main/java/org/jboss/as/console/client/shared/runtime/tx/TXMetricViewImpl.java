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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 11/3/11
 */
public class TXMetricViewImpl extends SuspendableViewImpl implements TXMetricPresenter.MyView {

    private TXMetricManagement presenter;
    private TXExecutionView executionMetric;
    private TXRollbackView rollbackMetric;
    private GeneralView statisticsInfo;

    @Override
    public void setPresenter(TXMetricManagement presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        HTML refreshBtn = new HTML("<i class='icon-refresh'></i> Refresh Results");
                      refreshBtn.setStyleName("html-link");
                      refreshBtn.getElement().getStyle().setPosition(Style.Position.RELATIVE);
                      refreshBtn.getElement().getStyle().setTop(-80, Style.Unit.PX);
                      refreshBtn.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
                      refreshBtn.getElement().getStyle().setFloat(Style.Float.RIGHT);
                      refreshBtn.getElement().getStyle().setLeft(80, Style.Unit.PCT);

        refreshBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_refresh_tXMetricViewImp());

        refreshBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.refresh();
            }
        });

        this.executionMetric = new TXExecutionView();
        this.rollbackMetric = new TXRollbackView();
        this.statisticsInfo = new GeneralView();

        SimpleLayout layout = new SimpleLayout()
                .setTitle("Transaction Manager")
                .setHeadline("Transaction Metrics")
                .setDescription(Console.CONSTANTS.subys_tx_metric_desc())
                .addContent("", refreshBtn)
                .addContent("General Info", statisticsInfo.asWidget())
                .addContent("Executions", executionMetric.asWidget())
                .addContent("Rollbacks", rollbackMetric.asWidget());


        return layout.build();
    }

    @Override
    public void clearSamples() {
        this.executionMetric.clearSamples();
        this.rollbackMetric.clearSamples();
        this.statisticsInfo.clear();
    }

    @Override
    public void setTxMetric(Metric txMetric) {
        this.executionMetric.addSample(txMetric);
    }
    
    @Override
    public void setGeneralMetric(ModelNode txModel) {
        this.statisticsInfo.setGeneralMetric(txModel);
    }

    @Override
    public void setRollbackMetric(Metric rollbackMetric) {
        this.rollbackMetric.addSample(rollbackMetric);
    }

    public void setStatistcsEnabled(final boolean stats) {
        statisticsInfo.setStatistcsEnabled(stats);
    }

}
