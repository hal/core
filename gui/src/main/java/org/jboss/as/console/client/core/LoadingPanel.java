package org.jboss.as.console.client.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Heiko Braun
 * @date 12/7/11
 */
public class LoadingPanel implements IsWidget {

    static public LoadingPanel get() {
        if (instance == null) {
            instance = new LoadingPanel();
            instance.off();
            RootLayoutPanel.get().add(instance);
        }
        return instance;
    }

    private static LoadingPanel instance;

    // CSS might not be injected when we display the loading panel, so use inline styles in that case!
    private static final SafeStyles PANEL_STYLE = SafeStylesUtils.fromTrustedString(
            "vertical-align: middle;" +
            "margin-top: 200px;" +
            "margin-left: auto;" +
            "margin-right: auto;" +
            "height: 50px;" +
            "width: 120px;" +
            "border: 1px solid #cdcdcd;" +
            "background-color: #ffffff;" +
            "padding-top: 20px;" +
            "-moz-border-radius: 2px;" +
            "border-radius: 2px;" +
            "-moz-box-shadow:    6px 6px 6px #e3e3e3, -6px 6px 6px #e3e3e3, 6px -6px 6px #e3e3e3, -6px -6px 6px #e3e3e3;" +
            "-webkit-box-shadow: 6px 6px 6px #e3e3e3, -6px 6px 6px #e3e3e3, 6px -6px 6px #e3e3e3, -6px -6px 6px #e3e3e3;" +
            "box-shadow:         6px 6px 6px #e3e3e3, -6px 6px 6px #e3e3e3, 6px -6px 6px #e3e3e3, -6px -6px 6px #e3e3e3;");

    private static final SafeStyles IMAGE_STYLE = SafeStylesUtils.fromTrustedString(
            "vertical-align: middle;" +
            "padding-right: 10px;");

    private static final Template TEMPLATE = GWT.create(Template.class);

    private final HTMLPanel html;

    public LoadingPanel() {
        html = new HTMLPanel(TEMPLATE.panel(PANEL_STYLE, IMAGE_STYLE));
    }

    @Override
    public Widget asWidget() {
        return html.asWidget();
    }

    public void on() {
        html.setVisible(true);
    }

    public void off() {
        html.setVisible(false);
    }

    interface Template extends SafeHtmlTemplates {
        @Template("<center><div style=\"{0}\"><img src=\"images/loading_lite.gif\" style=\"{1}\"/>Loading...</div></center>")
        SafeHtml panel(SafeStyles panelStyle, SafeStyles imageStyle);
    }
}
