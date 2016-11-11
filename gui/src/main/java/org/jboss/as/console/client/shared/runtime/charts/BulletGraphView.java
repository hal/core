package org.jboss.as.console.client.shared.runtime.charts;

import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.Sampler;
import org.thechiselgroup.choosel.protovis.client.PV;
import org.thechiselgroup.choosel.protovis.client.PVBulletLayout;
import org.thechiselgroup.choosel.protovis.client.PVMark;
import org.thechiselgroup.choosel.protovis.client.PVPanel;
import org.thechiselgroup.choosel.protovis.client.PVShape;
import org.thechiselgroup.choosel.protovis.client.ProtovisWidget;
import org.thechiselgroup.choosel.protovis.client.jsutil.JsArgs;
import org.thechiselgroup.choosel.protovis.client.jsutil.JsArrayGeneric;
import org.thechiselgroup.choosel.protovis.client.jsutil.JsDoubleFunction;
import org.thechiselgroup.choosel.protovis.client.jsutil.JsFunction;
import org.thechiselgroup.choosel.protovis.client.jsutil.JsStringFunction;
import org.thechiselgroup.choosel.protovis.client.jsutil.JsUtils;

import static org.thechiselgroup.choosel.protovis.client.PVAlignment.*;

/**
 * @author Heiko Braun
 * @date 8/1/13
 */
public class BulletGraphView implements Sampler {

    private final String title;
    private final String metricName;
    private Column[] columns;
    private int ROW_OFFSET = 1;

    private HorizontalPanel container;
    private PVPanel vis = null;
    private Grid grid;

    private boolean embeddedUse = false;

    @Deprecated
    public BulletGraphView(String title, String metricName) {
        this.title = title;
        this.metricName = metricName;
    }

    public BulletGraphView(String title, String metricName, boolean embeddedUse) {
        this.title = title;
        this.metricName = metricName;
        this.embeddedUse = embeddedUse;
    }

    public Sampler setColumns(Column[] columns) {
        this.columns = columns;
        return this;
    }

    public static class Bullet {

        public String title;

        public String subtitle;

        public double[] ranges;

        public double[] measures;

        public double[] markers;

        private Bullet(String title, String subtitle, double[] ranges,
                       double[] measures, double[] markers) {

            this.title = title;
            this.subtitle = subtitle;
            this.ranges = ranges;
            this.measures = measures;
            this.markers = markers;
        }

    }

    public int getBaseLineIndex() {
        int i=0;
        boolean didMatch = false;
        for(Column c : columns)
        {
            if(c.isBaseline())
            {
                didMatch=true;
                break;
            }
            i++;
        }

        return didMatch ? i : -1;
    }

    @Override
    public Widget asWidget() {

        VerticalPanel desc = new VerticalPanel();
        desc.addStyleName("metric-container");
        if(embeddedUse)
            desc.add(new HTML("<h3 class='metric-label-embedded'>" + title + "</h3>"));
        else
            desc.add(new HTML("<h3 class='metric-label'>" + title + "</h3>"));

        container = new HorizontalPanel();
        container.setStyleName("fill-layout-width");
        container.addStyleName("metric-panel");


        grid = new Grid(columns.length, 2);
        grid.addStyleName("metric-grid");

        // format
        for (int row = 0; row < columns.length; ++row) {
            grid.getCellFormatter().addStyleName(row, 0,  "nominal");
            grid.getCellFormatter().addStyleName(row, 1, "numerical");
        }

        int baselineIndex = getBaseLineIndex();
        if(baselineIndex>=0)
            grid.getRowFormatter().addStyleName(baselineIndex, "baseline");

        // init
        for(int i=0; i<columns.length;i++)
        {
            grid.setText(i, 0, columns[i].label);
            grid.setText(i, 1, "0");
        }


        container.add(grid);

        ProtovisWidget graphWidget = new ProtovisWidget();
        graphWidget.initPVPanel();
        vis = createVisualization(graphWidget);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                renderDefault();    // the 'empty' display
            }
        });

        container.add(graphWidget);
        graphWidget.getElement().getParentElement().setAttribute("align", "center");
        graphWidget.getElement().getParentElement().setAttribute("width", "80%");

        desc.add(container);

        return desc;
    }

    private void renderDefault(){
        vis.data(generateDefaultData()).render();

        for(int i=0; i<columns.length;i++)
                {
                    grid.setText(i, 0, columns[i].label);
                    grid.setText(i, 1, "");
                }

    }

    private void render(JsArrayGeneric<Bullet> bullets){
        vis.data(bullets).render();
    }

    @Override
    public void addSample(Metric metric) {

        Double baseline = getBaselineValue(metric);

        if(baseline>0.0)
        {
            final JsArrayGeneric<Bullet> bullets = distinctColumnStrategy(metric);

            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    render(bullets);
                }
            });
        }

        Scheduler.get().scheduleDeferred(() -> { // defer so the values aren't rewritten by renderDefault() triggered from asWidget()
            for (int i = 0; i < columns.length; i++) {
                grid.setText(i, 0, columns[i].label);
                grid.setText(i, 1, metric.get(i));
            }
        });
    }

    private JsArrayGeneric<Bullet> distinctColumnStrategy(Metric metric) {

        JsArrayGeneric<Bullet> bullets = JsUtils.createJsArrayGeneric();
        Double baseline = getBaselineValue(metric);

        int row=ROW_OFFSET;
        for(Column c : columns)
        {

            int dataIndex = row - ROW_OFFSET;
            String actualValue = metric.get(dataIndex);

            if(null==actualValue)
                throw new RuntimeException("Metric value at index "+dataIndex+" is null");

            // with comparison column
            if(c.isBaseline())
            {
                // skip the baseline itself
            }
            else {
                Double value = Double.valueOf(actualValue);
                String label = c.getLabel();//percentage(baseline, value) +"% "+c.getLabel();

                if(c.getComparisonColumn()!=null && baseline<0)
                {
                    throw new RuntimeException("Comparison column specified, but no baseline set!");
                }

                bullets.push(
                        new Bullet(
                                label, metricName,
                                new double[] {baseline},
                                new double[] {value},
                                new double[] {}
                        )
                );

            }


            row++;
        }

        return bullets;
    }

    private Double getBaselineValue(Metric metric) {
        int baselineIndex = getBaseLineIndex();

        // check if they match
        if(baselineIndex>metric.numSamples())
            throw new RuntimeException("Illegal baseline index "+baselineIndex+" on number of samples "+metric.numSamples());

        return baselineIndex >= 0 ?
                Double.valueOf(metric.get(baselineIndex)) : -1;
    }

    static long percentage(double total, double actual)
    {
        if(total==0 || actual==0)
            return 0;

        return Math.round((actual/total)*100);
    }

    @Override
    public void clearSamples() {
        renderDefault();
    }

    @Override
    public long numSamples() {
        return 1;
    }


    @Override
    public void recycle() {

    }


    /* bullet graph specs */

    private PVPanel createVisualization(final ProtovisWidget graphWidget) {

        final PVPanel vis = graphWidget.getPVPanel()
                .width(400)
                .height(30)
                .margin(20)
                .left(100)  // translate(_,y)
                .top(
                        new JsDoubleFunction() {
                            public double f(JsArgs args) {
                                PVMark _this = args.getThis();
                                return 10 + _this.index() * 60; // translate(x,_)
                            }
                        }
                );

        PVBulletLayout bullet = vis.add(PV.Layout.Bullet()).orient(LEFT)
                .ranges(new JsFunction<JsArrayNumber>() {
                    public JsArrayNumber f(JsArgs args) {
                        Bullet d = args.getObject();
                        return JsUtils.toJsArrayNumber(d.ranges);
                    }
                }).measures(new JsFunction<JsArrayNumber>() {
                    public JsArrayNumber f(JsArgs args) {
                        Bullet d = args.getObject();
                        return JsUtils.toJsArrayNumber(d.measures);
                    }
                }).markers(new JsFunction<JsArrayNumber>() {
                    public JsArrayNumber f(JsArgs args) {
                        Bullet d = args.getObject();
                        return JsUtils.toJsArrayNumber(d.markers);
                    }
                });


        // workaround for right hand side labels
        graphWidget.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if(event.isAttached()) {
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            Element svg = graphWidget.getElement().getFirstChildElement();
                            if (svg != null) {
                                svg.setAttribute("overflow", "visible");
                            }
                        }
                    });
                }
            }
        });

        bullet.strokeStyle("#CFCFCF");
        bullet.lineWidth(0.9);

        bullet.range().add(PV.Bar).fillStyle("#ffffff");
        bullet.measure().add(PV.Bar).fillStyle("#666666");

        bullet.marker().add(PV.Dot).shape(PVShape.TRIANGLE).fillStyle("white");
        bullet.tick().add(PV.Rule)
                .strokeStyle("#CFCFCF")
                .anchor(BOTTOM)
                .add(PV.Label)
                .text(bullet.x().tickFormat());

        // title
        bullet.anchor(LEFT).add(PV.Label).font("12px sans-serif")
                .textAlign(RIGHT).textBaseline(BOTTOM)
                .text(new JsStringFunction() {
                    public String f(JsArgs args) {
                        Bullet d = args.getObject(0);
                        return d.title;
                    }
                });

        // subtitle
        bullet.anchor(LEFT).add(PV.Label).textStyle("#616161").textAlign(RIGHT)
                .textBaseline(TOP).text(new JsStringFunction() {
            public String f(JsArgs args) {
                Bullet d = args.getObject(0);
                return d.subtitle;
            }
        });

        // scale
        bullet.anchor(RIGHT)
                .add(PV.Label)
                .textStyle("#616161")
                .textAlign(LEFT)
                .textBaseline(MIDDLE)
                .text(new JsStringFunction() {
                    public String f(JsArgs args) {
                        Bullet d = args.getObject(0);
                        double measures = d.measures[0];
                        return measures > 0.00 ? String.valueOf(measures) : "";
                    }
                });


        return vis;
    }

    private JsArrayGeneric<Bullet> generateDefaultData() {
        JsArrayGeneric<Bullet> bullets = JsUtils.createJsArrayGeneric();
        for(Column col : columns)
        {
            if(col.isBaseline())
                continue;

            bullets.push(
                    new Bullet(
                            col.getLabel(), metricName,
                            new double[] { 100 },
                            new double[] { 0 },
                            new double[] { }
                    )
            );
        }
        return bullets;
    }


}
