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
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

/**
 * @author Heiko Braun
 * @date 11/3/11
 */
public class TXMetricViewImpl extends SuspendableViewImpl implements TXMetricPresenter.MyView {

    private TXMetricManagement presenter;
    private TXExecutionView executionMetric;
    private TXRollbackView rollbackMetric;

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

        SimpleLayout layout = new SimpleLayout()
                .setTitle("Transaction Manager")
                .setHeadline("Transaction Metrics")
                .setDescription(Console.CONSTANTS.subys_tx_metric_desc())
                .addContent("", refreshBtn)
                .addContent("Executions", executionMetric.asWidget())
                .addContent("Rollbacks", rollbackMetric.asWidget());


        return layout.build();
    }

    @Override
    public void clearSamples() {
        this.executionMetric.clearSamples();
        this.rollbackMetric.clearSamples();
    }

    @Override
    public void setTxMetric(Metric txMetric) {
        this.executionMetric.addSample(txMetric);
    }

    @Override
    public void setRollbackMetric(Metric rollbackMetric) {
        this.rollbackMetric.addSample(rollbackMetric);
    }

}
