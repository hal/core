package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.widgets.Code;
import org.jboss.as.console.client.widgets.nav.AriaLink;
import org.jboss.ballroom.client.widgets.icons.Icons;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Heiko Braun
 */
public class ConnectionWindow {

    private DefaultWindow window;

    public ConnectionWindow(final String name, final VerifyConnectionOp.VerifyResult result) {
        int width = result.hasDetails() ? 500 : 320;
        int height = result.hasDetails() ? 320 : 240;
        window = new DefaultWindow(Console.CONSTANTS.subsys_jca_dataSource_verify());
        window.setWidth(width);
        window.setHeight(height);
        window.setGlassEnabled(true);

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("default-window-content");

        HorizontalPanel header = new HorizontalPanel();

        String headerText = result
                .wasSuccessful() ? Console.CONSTANTS.verify_datasource_successful_header() : Console.CONSTANTS
                .verify_datasource_failed_header();
        HTML text = new HTML("<h3>" + headerText + "</h3>");
        ImageResource icon = result.wasSuccessful() ? Icons.INSTANCE.info_blue() : Icons.INSTANCE.info_red();
        Image image = new Image(icon);
        header.add(image);
        header.add(text);

        image.getElement().getParentElement().setAttribute("style","padding-right:10px;vertical-align:middle");
        text.getElement().getParentElement().setAttribute("style","vertical-align:middle");
        panel.add(header);
        panel.add(new HTML(result.getMessage()));
        if (result.hasDetails()) {
            AriaLink detailsLink = new AriaLink(Console.CONSTANTS.common_label_details());
            detailsLink.addStyleName("details-panel-header");
            detailsLink.getElement().setAttribute("style", "padding-top:15px;");
            DisclosurePanel detailsPanel = new DisclosurePanel();
            detailsPanel.setHeader(detailsLink);
            detailsPanel.addStyleName("help-panel-aligned");
            detailsPanel.addOpenHandler(new OpenHandler<DisclosurePanel>() {

                @Override
                public void onOpen(OpenEvent<DisclosurePanel> event) { event.getTarget().addStyleName("help-panel-aligned-open");
                }
            });
            Code detailsText = new Code();
            detailsText.setValue(SafeHtmlUtils.fromString(result.getDetails()));
            detailsPanel.add(detailsText);
            panel.add(detailsPanel);
        }

        ClickHandler confirmHandler = new ClickHandler() {

            public void onClick(ClickEvent event) {
                window.hide();
            }
        };
        DialogueOptions options = new DialogueOptions("OK", confirmHandler, "Cancel", confirmHandler);
        Widget content = new WindowContentBuilder(panel, options.showCancel(false)).build();
        window.trapWidget(content);
    }

    public void show() {
        window.center();
    }

    public void hide() {
        window.hide();
    }
}
