package org.jboss.as.console.client.csp;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.LayoutPanel;
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

    private CSPPresenter presenter;
    private Frame supportFrame;
    private String cspUrl;

    @Inject
    public CSPView(BootstrapContext bootstrapContext) {
        cspUrl = bootstrapContext.getProperty(ApplicationProperties.CSP_API);
    }

    @Override
    public void setPresenter(CSPPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setRef(String angularRef) {

        if(angularRef.equals("search"))
        {
            supportFrame.setUrl(cspUrl + "/customer.html#search");
        }
        else if(angularRef.equals("open"))
        {
            supportFrame.setUrl(cspUrl + "/customer.html#case/new");
        }
        else if(angularRef.equals("modify"))
        {
            supportFrame.setUrl(cspUrl + "/customer.html#case/list");
        }

    }

    @Override
    public Widget createWidget() {

        supportFrame = new Frame(cspUrl + "/customer.html");
        supportFrame.getElement().setAttribute("style", "margin-top:10px");
        supportFrame.setWidth("100%");
        supportFrame.setHeight("100%");

        LayoutPanel layout = new LayoutPanel();
        layout.setStyleName("fill-layout");
        layout.add(supportFrame);
        layout.setWidgetTopHeight(supportFrame, 0, Style.Unit.PX, 100, Style.Unit.PCT);
        layout.setWidgetLeftWidth(supportFrame, 0, Style.Unit.PX, 100, Style.Unit.PCT);
        return layout;
    }


    /*private void forceResize() {
        int w = tabs.getOffsetWidth();
        int h = tabs.getOffsetHeight();
        searchFrame.setSize(w +"px", h +"px");
        supportFrame.setSize(w +"px", h +"px");
    }*/
}
