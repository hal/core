package org.jboss.as.console.client.widgets.progress;

import com.google.gwt.user.client.ui.PopupPanel;

/**
 * @author Heiko Braun
 * @date 6/13/13
 */
public class ProgressWindow extends PopupPanel {

    private final ProgressBar bar;

    public ProgressWindow(final String title) {

        bar = new ProgressBar(0.0, 100.0);
        bar.setTextFormatter(new ProgressBar.TextFormatter() {
            @Override
            protected String getText(ProgressBar bar, double curProgress) {
                return title + " "+curProgress;
            }
        });
        setWidget(bar);

        setWidth("240px");
        setHeight("25px");

    }

    public ProgressBar getBar() {
        return bar;
    }
}
