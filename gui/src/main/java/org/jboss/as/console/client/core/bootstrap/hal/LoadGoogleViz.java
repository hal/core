package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.OrgChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.gwt.flow.client.Control;

/**
 * @author Heiko Braun
 * @date 12/7/11
 */
public class LoadGoogleViz implements BootstrapStep {

    @Override
    public void execute(Control<BootstrapContext> control) {
        // GWT vis is only used by MBUI tools. These are available in dev mode only
        if (!GWT.isScript()) {
            VisualizationUtils.loadVisualizationApi(() -> {
                        System.out.println("Loaded Google Vizualization API");
                    }, LineChart.PACKAGE, OrgChart.PACKAGE
            );
        }
        // viz can be loaded in background ...
        control.proceed();
    }
}
