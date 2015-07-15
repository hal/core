package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * @author Heiko Braun
 * @since 13/07/15
 */
public class PreviewState {
    public static void warn(SafeHtmlBuilder html, String title) {
        html.appendHtmlConstant("<p/><span style='padding-left:12px' class='warn-color'>&nbsp;</span>&nbsp;");
        html.appendEscaped(title).appendHtmlConstant("<p/>");
    }

    public static void info(SafeHtmlBuilder html, String title) {
        html.appendHtmlConstant("<p/><span style='padding-left:12px' class='info-color'>&nbsp;</span>&nbsp;");
        html.appendEscaped(title).appendHtmlConstant("<p/>");
    }

    public static void paused(SafeHtmlBuilder html, String title) {
            html.appendHtmlConstant("<p/><span style='padding-left:12px' class='paused-color'>&nbsp;</span>&nbsp;");
            html.appendEscaped(title).appendHtmlConstant("<p/>");
        }

    public static void error(SafeHtmlBuilder html, String title) {
        html.appendHtmlConstant("<p/><span style='padding-left:12px' class='error-color'>&nbsp;</span>&nbsp;");
        html.appendEscaped(title).appendHtmlConstant("<p/>");
    }

    public static void good(SafeHtmlBuilder html, String title) {
        html.appendHtmlConstant("<p/><span style='padding-left:12px' class='active-color'>&nbsp;</span>&nbsp;");
        html.appendEscaped(title).appendHtmlConstant("<p/>");
    }
}
