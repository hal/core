package org.jboss.as.console.client.csp;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.SuspendableViewImpl;

import javax.inject.Inject;

/**
 * @author Heiko Braun
 * @since 19/08/14
 */
public class CSPView extends SuspendableViewImpl implements CSPPresenter.MyView {

    private final BootstrapContext bootstrapContext;
    private CSPPresenter presenter;
    private TabLayoutPanel tabs;
    private Frame supportFrame;
    private Frame searchFrame;

    @Inject
    public CSPView(BootstrapContext bootstrapContext) {
        this.bootstrapContext = bootstrapContext;
    }

    @Override
    public void setPresenter(CSPPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        String cspUrl = bootstrapContext.getProperty(ApplicationProperties.CSP_API);
        /*tabs = new TabLayoutPanel(40, Style.Unit.PX);
        tabs.setStyleName("default-tabpanel");


        searchFrame = new Frame(cspUrl + "/search.html");
        searchFrame.getElement().setAttribute("style", "margin-top:10px");
        searchFrame.setWidth("100%");
        searchFrame.setHeight("100%");*/

        /*searchFrame.addLoadHandler(new LoadHandler() {
            @Override
            public void onLoad(LoadEvent loadEvent) {
                forceResize();
            }
        });*/

        supportFrame = new Frame(cspUrl + "/customer.html");
        supportFrame.getElement().setAttribute("style", "margin-top:10px");
        supportFrame.setWidth("100%");
        supportFrame.setHeight("100%");


        /*tabs.add(searchFrame, "Knowledge Base");
        tabs.add(supportFrame, "Support Tickets");
        tabs.selectTab(0);*/

        return supportFrame;
    }


    /*private void forceResize() {
        int w = tabs.getOffsetWidth();
        int h = tabs.getOffsetHeight();
        searchFrame.setSize(w +"px", h +"px");
        supportFrame.setSize(w +"px", h +"px");
    }*/
}
