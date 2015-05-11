package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;


/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class ServicesView {

    private EJB3Presenter presenter;

    private PagedView panel;
    private AsyncSvcView asyncSvcView;
    private TimerSvcView timerSvcView;
    private RemotingSvcView remotingSvcView;
    private IIOPSvcView iiopSvcView;


    public ServicesView(EJB3Presenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {


        asyncSvcView = new AsyncSvcView(presenter);
        timerSvcView = new TimerSvcView(presenter);
        remotingSvcView = new RemotingSvcView(presenter);
        iiopSvcView = new IIOPSvcView(presenter);

        panel = new PagedView(true);

        panel.addPage("Remoting Service", remotingSvcView.asWidget());
        panel.addPage("IIOP", iiopSvcView.asWidget());
        panel.addPage("Async", asyncSvcView.asWidget());
        panel.addPage("Timer", timerSvcView.asWidget());

        // default page
        panel.showPage(0);

        return panel.asWidget();
    }


    public void updateAsyncSvc(ModelNode payload) {
        asyncSvcView.setData(payload);
    }

    public void updateTimerSvc(ModelNode service) {
        timerSvcView.setData(service);
    }

    public void updateRemoteSvc(ModelNode service) {
        remotingSvcView.setData(service);
    }

    public void updateIIOPSvc(ModelNode service) {
        iiopSvcView.setData(service);
    }
}
