package org.jboss.as.console.client.v3.presenter;

import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;

/**
 * A marker interface used to instruct MainLayoutImpl to display specific options
 *
 * @author Heiko Braun
 * @since 03/03/15
 */
public interface Finder {
    FinderColumn.FinderId getFinderId();
}
