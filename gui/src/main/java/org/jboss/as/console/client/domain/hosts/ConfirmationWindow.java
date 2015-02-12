package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.ballroom.client.I18n;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Heiko Braun
 * @since 12/02/15
 */
public class ConfirmationWindow {

    private String message;
    private String title;
    private final Handler handler;

    public ConfirmationWindow(String title, String message, Handler handler) {
        this.message = message;
        this.title = title;
        this.handler = handler;
    }

    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("default-window-content");


        panel.add(new ContentHeaderLabel(title));

        HTML text = new HTML(message);
        text.getElement().setId("confirmation-message");
        panel.add(text);

        ClickHandler confirmHandler = new ClickHandler() {

            public void onClick(ClickEvent event) {
                handler.onConfirmation(true);

            }
        };

        ClickHandler cancelHandler = new ClickHandler() {

            public void onClick(ClickEvent event) {
                handler.onConfirmation(false);
            }
        };

        DialogueOptions options = new DialogueOptions(I18n.CONSTANTS.common_label_confirm(), confirmHandler, I18n.CONSTANTS.common_label_cancel(), cancelHandler);
        options.getSubmit().setAttribute("aria-describedby", "confirmation-message");

        return new WindowContentBuilder(panel, options).build();
    }

    interface Handler {
        void onConfirmation(boolean isConfirmed);
    }
}
