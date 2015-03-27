package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Heiko Braun
 * @since 13/02/15
 */
public interface PreviewFactory<T> {

    void createPreview(T data, AsyncCallback<SafeHtml> callback);
}
