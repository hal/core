package org.jboss.as.console.client.administration.audit;

import com.google.gwt.view.client.ProvidesKey;

/**
* @author Harald Pehl
* @date 08/13/2013
*/
class AuditLogItemKeyProvider implements ProvidesKey<AuditLogItem> {

    @Override
    public Object getKey(final AuditLogItem item) {
        return item.getId();
    }
}
