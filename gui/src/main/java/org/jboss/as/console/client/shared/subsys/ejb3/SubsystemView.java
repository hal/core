package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class SubsystemView {

    private EJB3Presenter presenter;

    private PagedView panel;
    private ThreadPoolView threadPoolView;
    private RemotingProfileView remotingProfileView;
    private ContainerView containerView;


    public SubsystemView(EJB3Presenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {

        containerView = new ContainerView(presenter);
        threadPoolView = new ThreadPoolView(presenter);
        remotingProfileView = new RemotingProfileView(presenter);

        panel = new PagedView(true);

        panel.addPage("Container", containerView.asWidget());
        panel.addPage("Thread Pools", threadPoolView.asWidget());
        panel.addPage("Remoting Profiles", remotingProfileView.asWidget());

        // default page
        panel.showPage(0);

        return panel.asWidget();
    }

    public void updateContainerView(ModelNode payload) {
        containerView.setData(payload);
    }

    public void updateThreadPools(List<Property> threadPools) {
        threadPoolView.setData(threadPools);
    }

    public void updateRemotingProfiles(List<Property> properties) {
        remotingProfileView.setData(properties);
    }


}
