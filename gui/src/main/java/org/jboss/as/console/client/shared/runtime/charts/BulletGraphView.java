package org.jboss.as.console.client.shared.runtime.charts;

import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.AttachEvent;
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
    private JsArrayGeneric<Bullet> bullets;
    private ProtovisWidget graphWidget;
    private HorizontalPanel container;

    public BulletGraphView(String title, String metricName) {
        this.title = title;
        this.metricName = metricName;
        this.bullets =  JsUtils.createJsArrayGeneric();

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

        container = new HorizontalPanel();
        container.setStyleName("fill-layout-width");
        container.addStyleName("metric-panel");

        VerticalPanel desc = new VerticalPanel();
        desc.add(new HTML("<h3>" + title + "</h3>"));

        int baselineIndex = getBaseLineIndex();
        if(baselineIndex>=0)
            desc.add(new HTML("<i>Compared to " + columns[baselineIndex].getLabel() + "</i>"));

        container.add(desc);

        desc.getElement().getParentElement().setAttribute("width", "20%");
        graphWidget = new ProtovisWidget();
        graphWidget.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if(event.isAttached())
                {
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            renderDefault();    // the 'empty' display
                        }
                    });
                }
            }
        });

        container.add(graphWidget);
        graphWidget.getElement().getParentElement().setAttribute("align", "center");
        graphWidget.getElement().getParentElement().setAttribute("width", "80%");

        return container;
    }

    private void renderDefault(){
        graphWidget.initPVPanel();
        createVisualization(generateDefaultData());
        graphWidget.getPVPanel().render();
    }

    private void render(){
        graphWidget.initPVPanel();  // TODO: is this reset()?
        createVisualization(bullets);
        graphWidget.getPVPanel().render();
    }

    @Override
    public void addSample(Metric metric) {

        distinctColumnStrategy(metric);


        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                render();
            }
        });


    }

    private void distinctColumnStrategy(Metric metric) {

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
                String label = percentage(baseline, value) +"% "+c.getLabel();

                if(c.getComparisonColumn()!=null && baseline>0)
                {
                    bullets.push(
                            new Bullet(
                                    label, metricName,
                                    new double[] {baseline},
                                    new double[] {value},
                                    new double[] {}
                                    //new double[] { Double.valueOf(metric.get(getComparisonIndex(c))) }
                            )
                    );
                }
                // without comparison column
                else if(c.getComparisonColumn()==null && baseline>0)
                {
                    bullets.push(
                            new Bullet(
                                    label, metricName,
                                    new double[] {baseline},
                                    new double[] {value},
                                    new double[] {  }
                            )
                    );
                }
                else if(c.getComparisonColumn()!=null && baseline<0)
                {
                    throw new RuntimeException("Comparison column specified, but no baseline set!");
                }
            }


            row++;
        }
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

    private int getComparisonIndex(Column current) {
        int index = 0;
        for(Column c : columns)
        {
            if(c.equals(current))
            {
                break;
            }
            index++;
        }
        return index;
    }

    @Override
    public void clearSamples() {
        this.bullets =  JsUtils.createJsArrayGeneric();
    }

    @Override
    public long numSamples() {
        return 1;
    }


    @Override
    public void recycle() {

    }


    /* bullet graph specs */

    private void createVisualization(JsArrayGeneric<Bullet> bullets) {

        PVPanel vis = graphWidget.getPVPanel().data(bullets).width(400).height(30)
                .margin(20).left(120).top(new JsDoubleFunction() {
                    public double f(JsArgs args) {
                        PVMark _this = args.getThis();
                        return 10 + _this.index() * 60;
                    }
                });

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

        bullet.range().add(PV.Bar);
        bullet.measure().add(PV.Bar);

        bullet.marker().add(PV.Dot).shape(PVShape.TRIANGLE).fillStyle("white");
        bullet.tick().add(PV.Rule).anchor(BOTTOM).add(PV.Label)
                .text(bullet.x().tickFormat());

        bullet.anchor(LEFT).add(PV.Label).font("bold 12px sans-serif")
                .textAlign(RIGHT).textBaseline(BOTTOM)
                .text(new JsStringFunction() {
                    public String f(JsArgs args) {
                        Bullet d = args.getObject(0);
                        return d.title;
                    }
                });

        bullet.anchor(LEFT).add(PV.Label).textStyle("#616161").textAlign(RIGHT)
                .textBaseline(TOP).text(new JsStringFunction() {
            public String f(JsArgs args) {
                Bullet d = args.getObject(0);
                return d.subtitle;
            }
        });
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
