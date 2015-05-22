package org.jboss.as.console.client.preview;

import com.google.common.base.Function;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Heiko Braun
 * @since 27/03/15
 */
public interface PreviewContentFactory {
    void createContent(ExternalTextResource resource, AsyncCallback<SafeHtml> callback);
    void createAndModifyContent(ExternalTextResource resource, Function<SafeHtml, SafeHtml> modifyFn, AsyncCallback<SafeHtml> callback);
}
