package org.jboss.as.console.client.standalone.runtime;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.ViewImpl;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.widgets.TwoColumnLayout;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 11/2/11
 */
public class StandaloneRuntimeView extends ViewImpl implements StandaloneRuntimePresenter.MyView{

    private StandaloneRuntimePresenter presenter;

    private TwoColumnLayout layout;
    private LayoutPanel contentCanvas;
    private StandaloneRuntimeNavigation lhsNavigation;

    public StandaloneRuntimeView() {
        super();

        contentCanvas = new LayoutPanel();
        lhsNavigation = new StandaloneRuntimeNavigation();

        layout = new TwoColumnLayout(lhsNavigation.asWidget(), contentCanvas.asWidget());

    }

    @Override
    public Widget asWidget() {
        return layout.asWidget();
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {

        if (slot == StandaloneRuntimePresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);

        } else {
            Console.getMessageCenter().notify(
                    new Message("Unknown slot requested:" + slot)
            );
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }

    @Override
    public void setPresenter(StandaloneRuntimePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setSubsystems(List<SubsystemRecord> result) {
        lhsNavigation.setSubsystems(result);
    }
}
