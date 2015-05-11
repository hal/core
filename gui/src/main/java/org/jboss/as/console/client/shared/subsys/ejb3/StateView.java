package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class StateView {

    private EJB3Presenter presenter;

    private PagedView panel;
    private CachesView cachesView;

    private static final AddressTemplate PASSIVATION_ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=ejb3/passivation-store=*");

    private static final AddressTemplate FILE_ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=ejb3/file-passivation-store=*");

    private static final AddressTemplate CLUSTER_ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=ejb3/cluster-passivation-store=*");

    private PassivationView passivationView;
    private PassivationView fileView;
    private PassivationView clusterView;

    public StateView(EJB3Presenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {

        cachesView = new CachesView(presenter);
        passivationView = new PassivationView(presenter, PASSIVATION_ADDRESS, "Passivation");
        fileView = new PassivationView(presenter, FILE_ADDRESS, "File Passivation");
        clusterView = new PassivationView(presenter, CLUSTER_ADDRESS, "Cluster Passivation");

        panel = new PagedView(true);

        panel.addPage("Caches", cachesView.asWidget());
        panel.addPage("Passivation", passivationView.asWidget());
        panel.addPage("Cluster Passivation", clusterView.asWidget());
        panel.addPage("File Passivation", fileView.asWidget());

        // default page
        panel.showPage(0);

        return panel.asWidget();
    }


    public void updateCaches(List<Property> threadPools) {
        cachesView.setData(threadPools);
    }

    public void updatePassivationStore(List<Property> threadPools) {
        passivationView.setData(threadPools);
    }

    public void updateFilePassivationStore(List<Property> threadPools) {
        fileView.setData(threadPools);
    }

    public void updateClusterPassivationStore(List<Property> threadPools) {
        clusterView.setData(threadPools);
    }

}
