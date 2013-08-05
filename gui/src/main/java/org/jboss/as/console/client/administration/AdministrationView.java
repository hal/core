package org.jboss.as.console.client.administration;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;


/**
 * @author Harald Pehl
 * @date 07/25/2013
 */
public class AdministrationView extends SuspendableViewImpl implements AdministrationPresenter.MyView {

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private LHSAdministrationNavigation lhsNavigation;
    private AdministrationPresenter presenter;

    public AdministrationView() {
        layout = new SplitLayoutPanel(10);

        contentCanvas = new LayoutPanel();
        lhsNavigation = new LHSAdministrationNavigation();

        layout.addWest(lhsNavigation.asWidget(), 197);
        layout.add(contentCanvas);
    }

    @Override
    public void setPresenter(final AdministrationPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        return layout;
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {
        if (slot == AdministrationPresenter.TYPE_MainContent) {
            if (content != null) { setContent(content); }
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }
}
