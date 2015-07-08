package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class ServletView extends SuspendableViewImpl implements ServletPresenter.MyView {
    private ServletPresenter presenter;

    private PagedView panel;
    private ContainerList serverList;
    private JSPView jspView;
    private WebSocketView webSockets;
    private CookieView cookieView;
    private SessionView sessionView;


    @Override
    public void setPresenter(ServletPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel("Servlet Container");
        layout.add(titleBar);

        panel = new PagedView();

        serverList = new ContainerList(presenter);
        jspView = new JSPView(presenter);
        webSockets = new WebSocketView(presenter);
        sessionView = new SessionView(presenter);
        cookieView = new CookieView(presenter);

        panel.addPage("Servlet Container", serverList.asWidget());
        panel.addPage("JSP", jspView.asWidget());
        panel.addPage("Web Sockets", webSockets.asWidget());
        panel.addPage("Sessions", sessionView.asWidget());
        panel.addPage("Cookies", cookieView.asWidget());


        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget);

        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(panelWidget, 40, Style.Unit.PX, 100, Style.Unit.PCT);


        // clear states

        panel.addPageCallback(
                new PagedView.PageCallback() {
                    @Override
                    public void onRevealPage(int index) {
                        if (0 == index) {
                            presenter.clearContainer();
                        }
                    }
                });

        return layout;
    }

    @Override
    public void setServletContainer(List<Property> server) {
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

            if(0==panel.getPage())
                panel.showPage(1);
        }
    }



    @Override
    public void setJSPSettings(ModelNode data) {
        jspView.setData(data);
    }

    @Override
    public void setWSSettings(ModelNode modelNode) {
        webSockets.setData(modelNode);
    }

    @Override
    public void setSessionSettings(ModelNode modelNode) {
        sessionView.setData(modelNode);
    }

    @Override
    public void setCookieSettings(ModelNode modelNode) {
        cookieView.setData(modelNode);
    }



}
