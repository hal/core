package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class HttpView extends SuspendableViewImpl implements HttpPresenter.MyView {
    private HttpPresenter presenter;

    private PagedView panel;
    private ServerList serverList;
    private HttpListenerView httpView;
    private HttpsListenerView httpsView;
    private AJPListenerView ajpView;
    private HostView hostView;

    @Override
    public void setPresenter(HttpPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel("HTTP Server");
        layout.add(titleBar);

        panel = new PagedView();

        serverList = new ServerList(presenter);
        httpView = new HttpListenerView(presenter);
        httpsView = new HttpsListenerView(presenter);
        ajpView = new AJPListenerView(presenter);
        hostView = new HostView(presenter);

        panel.addPage(Console.CONSTANTS.common_label_back(), serverList.asWidget());
        panel.addPage("HTTP Listener", httpView.asWidget());
        panel.addPage("HTTPS Listener", httpsView.asWidget());
        panel.addPage("AJP Listener", ajpView.asWidget());
        panel.addPage("Hosts", hostView.asWidget());

        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget);

        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(panelWidget, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        return layout;
    }

    @Override
    public void setServer(List<Property> server) {
        serverList.setServer(server);
    }

    @Override
    public void setServerSelection(String name) {
        if(null==name)
        {
            panel.showPage(0);
        }
        else{

            presenter.loadDetails();

            // move to first page if still showing topology
            if(0==panel.getPage())
                panel.showPage(1);
        }
    }

    @Override
    public void setHttpListener(List<Property> httpListener) {
        httpView.setData(httpListener);
    }

    @Override
    public void setAjpListener(List<Property> ajpListener) {
        ajpView.setData(ajpListener);
    }

    @Override
    public void setHttpsListener(List<Property> httpsListener) {
        httpsView.setData(httpsListener);
    }

    @Override
    public void setHosts(List<Property> hosts) {
        hostView.setData(hosts);
    }
}
