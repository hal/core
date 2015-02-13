package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * @author Heiko Braun
 * @since 13/02/15
 */
public interface PreviewFactory<T> {

    SafeHtml createPreview(T data);
}
