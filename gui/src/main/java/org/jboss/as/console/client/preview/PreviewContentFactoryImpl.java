package org.jboss.as.console.client.preview;

import com.google.common.base.Function;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.ResourceCallback;
import com.google.gwt.resources.client.ResourceException;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Heiko Braun
 * @since 27/03/15
 */
public class PreviewContentFactoryImpl implements PreviewContentFactory {

    @Override
    public void createContent(final ExternalTextResource resource, final AsyncCallback<SafeHtml> callback) {
        createAndModifyContent(resource, input -> input, callback);
    }

    @Override
    public void createAndModifyContent(final ExternalTextResource resource, final Function<SafeHtml, SafeHtml> modifyFn,
            final AsyncCallback<SafeHtml> callback) {
        try {
            resource.getText(
                    new ResourceCallback<TextResource>() {
                        @Override
                        public void onError(ResourceException e) {
                            SafeHtmlBuilder error = new SafeHtmlBuilder();
                            error.appendEscaped(e.getMessage());
                            callback.onSuccess(error.toSafeHtml());
                        }

                        @Override
                        public void onSuccess(TextResource textResource) {
                            SafeHtmlBuilder html = new SafeHtmlBuilder();
                            html.appendHtmlConstant(textResource.getText());
                            callback.onSuccess(modifyFn.apply(html.toSafeHtml()));
                        }
                    }
            );
        } catch (ResourceException e) {
            SafeHtmlBuilder error = new SafeHtmlBuilder();
            error.appendEscaped(e.getMessage());
            callback.onSuccess(error.toSafeHtml());
        }
    }
}
