package org.jboss.as.console.client.standalone.runtime;

import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.jvm.model.OSMetric;
import org.jboss.as.console.client.shared.jvm.model.RuntimeMetric;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.vm.HeapChartView;
import org.jboss.as.console.client.shared.runtime.vm.ThreadChartView;
import org.jboss.as.console.client.shared.runtime.vm.VMMetricsManagement;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Heiko Braun
 * @date 9/28/11
 */
public class VMMetricsView extends SuspendableViewImpl implements VMMetricsPresenter.MyView {

    private VMMetricsManagement presenter;

    private VerticalPanel osPanel;

    private HeapChartView heapChart;
    private HeapChartView nonHeapChart;
    private ThreadChartView threadChart;

    private ContentHeaderLabel vmName;

    private HTML osName;
    private HTML processors;
    private HTML uptime;

    protected boolean hasServerPicker = false;


    @Override
    public void setPresenter(VMMetricsManagement presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {

        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel("Virtual Machine Status");
        layout.add(titleBar);

        ClickHandler refreshHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.refresh();
            }
        };

        // ----

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setStyleName("rhs-content-panel");

        ScrollPanel scroll = new ScrollPanel(vpanel);
        layout.add(scroll);

        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(scroll, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        // ------------------------

        osName = new HTML();
        processors = new HTML();
        uptime = new HTML();

        HorizontalPanel header = new HorizontalPanel();
        header.setStyleName("fill-layout-width");
        vmName = new ContentHeaderLabel("");
        header.add(vmName);

        HTML refreshBtn = new HTML("<i class='icon-refresh'></i> Refresh Results");
        refreshBtn.setStyleName("html-link");
        refreshBtn.addClickHandler(refreshHandler);

        osPanel = new VerticalPanel();
        osPanel.add(refreshBtn);
        osPanel.add(osName);
        osPanel.add(processors);
        osPanel.add(uptime);

        header.add(osPanel);

        vpanel.add(header);
        vpanel.add(osName);
        vpanel.add(processors);

        // 50/50
        osPanel.getElement().getParentElement().setAttribute("style", "width:50%; vertical-align:top;padding-right:15px;");
        osPanel.getElement().getParentElement().setAttribute("align", "right");
        vmName.getElement().getParentElement().setAttribute("style", "width:50%; vertical-align:top");




        // --

        heapChart = new HeapChartView("Heap Usage") ;
        nonHeapChart = new HeapChartView("Non Heap Usage", false) ;

        vpanel.add(heapChart.asWidget());
        vpanel.add(nonHeapChart.asWidget());

        // --

        threadChart = new ThreadChartView("Thread Usage");
        vpanel.add(threadChart.asWidget());
        //threadPanel.add(osPanel);

        return layout;
    }

    @Override
    public void setHeap(Metric heap) {

        if(heapChart!=null)
        {
            heapChart.addSample(heap);
        }
        //heapForm.edit(heap);
    }

    @Override
    public void setNonHeap(Metric nonHeap) {

        if(nonHeapChart!=null)
        {
            nonHeapChart.addSample(nonHeap);
        }
        //nonHeapForm.edit(nonHeap);
    }

    @Override
    public void setThreads(Metric metric) {

        if(threadChart!=null)
            threadChart.addSample(metric);
    }

    @Override
    public void setRuntimeMetric(RuntimeMetric runtime) {
        vmName.setText(runtime.getVmName());
        uptime.setHTML("<b style='color:#A7ABB4'>JVM Uptime:</b>   " + humanReadable(runtime.getUptime()));
    }

    /*
     * Converts the long uptime to an human readable format, examples:
     *   2 d, 0 hour, 34 min, 2s
     *   12 hours, 12 min, 22s
     *
     */
    private String humanReadable(long uptime) {
        uptime = uptime / 1000;

        int sec = (int) uptime % 60;
        uptime /= 60;

        int min = (int) uptime % 60;
        uptime /= 60;

        int hour = (int) uptime % 24;
        uptime /= 24;

        int day = (int) uptime;

        String str = "";
        if (day > 0)
            if (day > 1)
                str += day + " days, ";
            else
                str += day + " day, ";
        // prints 0 hour in case days exists. Otherwise prints 2 days, 34 min, sounds weird.
        if (hour > 0 || (day > 0))
            if (hour > 1)
                str += hour + " hours, ";
            else
                str += hour + " hour, ";
        if (min > 0)
            str += min + " min, ";
        if (sec > 0)
            str += sec + " s";

        return str;
    }

    @Override
    public void setOSMetric(OSMetric osMetric) {

        if(threadChart!=null)
        {
            osName.setHTML("<b style='color:#A7ABB4'>Operating System:</b>   "+osMetric.getName()+" "+osMetric.getVersion());
            processors.setHTML("<b style='color:#A7ABB4'>Processors:</b>   "+osMetric.getNumProcessors());
        }
    }

    @Override
    public void clearSamples() {

        osName.setHTML("");
        processors.setHTML("");

        heapChart.clearSamples();
        nonHeapChart.clearSamples();
        threadChart.clearSamples();
    }

}
