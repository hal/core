package org.jboss.as.console.client.preview;

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * @author Heiko Braun
 * @since 27/03/15
 */
public interface PreviewContentFactory {
    SafeHtml createContent(String id);
}
