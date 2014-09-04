package org.jboss.as.console.client.domain.runtime;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.ViewImpl;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.widgets.TwoColumnLayout;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Heiko Braun
 */
public class DomainRuntimeView extends ViewImpl implements DomainRuntimePresenter.MyView {

    private TwoColumnLayout layout;
    private LayoutPanel contentCanvas;
    private DomainRuntimeNavigation lhsNavigation;

    @Inject
    public DomainRuntimeView(EventBus eventBus) {
        super();


        contentCanvas = new LayoutPanel();
        lhsNavigation = new DomainRuntimeNavigation();

        layout = new TwoColumnLayout(lhsNavigation.asWidget(), contentCanvas);

        eventBus.addHandler(LHSHighlightEvent.TYPE, lhsNavigation);
    }

    @Override
    public Widget asWidget() {
        return layout.asWidget();
    }


    @Override
    public void setInSlot(Object slot, IsWidget  content) {
        if (slot == DomainRuntimePresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);

        } else {
            Console.MODULES.getMessageCenter().notify(
                    new Message("Unknown slot requested:" + slot)
            );
        }
    }

    private void setContent(IsWidget  newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }

    @Override
    public void setPresenter(DomainRuntimePresenter presenter) {}


    @Override
    public void setTopology(String selectedHost, String selectedServer, HostStore.Topology topology) {

        lhsNavigation.setTopology(selectedHost, selectedServer, topology);
    }

    @Override
    public void setSubsystems(List<SubsystemRecord> result) {
        lhsNavigation.setSubsystems(result);
    }
}
