package org.jboss.as.console.client.shared.runtime.vm;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.Sampler;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.runtime.plain.PlainColumnView;
import org.jboss.as.console.client.shared.runtime.charts.BulletGraphView;

/**
 * @author Heiko Braun
 * @date 9/29/11
 */
public class HeapChartView implements Sampler {

    private Sampler sampler;
    private String title;
    private boolean hasHelp = true;

    public HeapChartView(String title) {
        this.title = title;
    }

    public HeapChartView(String title, boolean hasHelp) {
        this.title = title;
        this.hasHelp = hasHelp;
    }

    public Widget asWidget() {
        return displayStrategy();
    }

    private Widget displayStrategy() {

        Column maxHeap = new NumberColumn("max","Max").setBaseline(true);
        Column[] heapCols = new Column[] {
                maxHeap,
                new NumberColumn("used","Used").setComparisonColumn(maxHeap),
                new NumberColumn("comitted","Committed"),
                new NumberColumn("init","Init"),
        };

        if(Console.protovisAvailable()) {
            sampler = new BulletGraphView(title, "mb")
                    .setColumns(heapCols);
        }
        else
        {

            // IE fallback
            sampler = new PlainColumnView(title)
                    .setColumns(heapCols);

            if(hasHelp) {
                StringBuilder html = new StringBuilder();
                html.append("<table class='help-attribute-descriptions'>");
                html.append("<tr><td>max: </td><td>The maximum amount of memory in mega bytes that can be used for memory management.</td></tr>");
                html.append("<tr><td>used: </td><td>The amount of used memory in mega bytes.</td></tr>");
                html.append("<tr><td>comitted: </td><td>The amount of memory in mega bytes that is committed for the Java virtual machine to use.</td></tr>");
                html.append("<tr><td>init: </td><td>The amount of memory in mega bytes that the Java virtual machine initially requests from the operating system for memory management.</td></tr>");
                html.append("</table>");

                ((PlainColumnView)sampler).setStaticHelp(new StaticHelpPanel(html.toString()));
            }
        }

        return sampler.asWidget();
    }


    @Override
    public void addSample(Metric metric) {

        long[] converted = new long[metric.numSamples()];
        for(int i=0; i<metric.numSamples();i++)
        {
            converted[i] = toMB(Long.valueOf(metric.get(i)).longValue());
        }
        sampler.addSample(new Metric(converted));
    }

    private static long toMB(long value) {
        return (value/1024l)/1024l;
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
