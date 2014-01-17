package org.jboss.as.console.client.shared.deployment;

import com.google.gwt.view.client.ListDataProvider;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.widgets.tables.DataProviderFilter;

/**
 * @author Heiko Braun
 * @date 7/31/12
 */
public class DeploymentFilter extends DataProviderFilter<DeploymentRecord> {

    public DeploymentFilter(ListDataProvider<DeploymentRecord> delegate) {
        super(delegate, new Predicate<DeploymentRecord>() {
            @Override
            public boolean apply(String prefix, DeploymentRecord candiate) {
                return candiate.getName().toLowerCase().contains(prefix.toLowerCase());
            }
        });
    }
}
