package org.jboss.as.console.client.shared.subsys.infinispan.v3;

import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @since 28/04/15
 */
public class CachesView extends SuspendableViewImpl implements CachesPresenter.MyView {

    public static final AddressTemplate INFINISPAN_SUBSYSTEM = AddressTemplate.of("{selected.profile}/subsystem=infinispan/cache-container=*");

    private CachesPresenter presenter;
    private CommonCacheView localCaches;
    private CommonCacheView distributedCaches;
    private CommonCacheView replicatedCaches;
    private CommonCacheView invalidationCaches;

    @Override
    public void setPresenter(CachesPresenter presenter) {

        this.presenter = presenter;
    }

    @Override
    public void setPreview(SafeHtml html) {

    }

    @Override
    public Widget createWidget() {

        localCaches = new CommonCacheView(
                presenter,
                "Local Caches",
                CachesView.INFINISPAN_SUBSYSTEM.append("local-cache=*")
        );

        invalidationCaches = new CommonCacheView(
                presenter,
                "Invalidation Caches",
                CachesView.INFINISPAN_SUBSYSTEM.append("invalidation-cache=*")
        );


        replicatedCaches = new CommonCacheView(
                presenter,
                "Replicated Caches",
                CachesView.INFINISPAN_SUBSYSTEM.append("replicated-cache=*")
        );

        distributedCaches = new CommonCacheView(
                presenter,
                "Distributed Caches",
                CachesView.INFINISPAN_SUBSYSTEM.append("distributed-cache=*")
        );

        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");

        tabLayoutpanel.add(localCaches.asWidget(), "Local Caches", true);
        tabLayoutpanel.add(invalidationCaches.asWidget(), "Invalidation Caches", true);
        tabLayoutpanel.add(distributedCaches.asWidget(), "Distributed Caches", true);
        tabLayoutpanel.add(replicatedCaches.asWidget(), "Replicated Caches", true);

        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }


    @Override
    public void updateLocalCache(ModelNode modelNode) {
        localCaches.updateFrom(modelNode);
    }

    @Override
    public void updateDistributedCache(ModelNode modelNode) {
        distributedCaches.updateFrom(modelNode);
    }

    @Override
    public void updateInvalidationCache(ModelNode modelNode) {
        invalidationCaches.updateFrom(modelNode);
    }

    @Override
    public void updateReplicatedCaches(ModelNode modelNode) {
        replicatedCaches.updateFrom(modelNode);
    }
}
