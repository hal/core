package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import org.jboss.as.console.client.widgets.popups.DefaultPopup;

/**
 * @author Heiko Braun
 * @since 09/07/15
 */
public class FinderTooltip<T> {

    private final static int DELAY_MS = 1000;
    private final Context<T> context;

    private Timer timer;
    private DefaultPopup popup;
    private T currentItem;
    private Element currentAnchor;

    public FinderTooltip(Context<T> context) {
        this.context = context;

        this.timer = new Timer() {
            @Override
            public void run() {
                showTooltip();
            }
        };

        this.popup = new DefaultPopup(DefaultPopup.Arrow.BOTTOM, true);

    }

    private void showTooltip() {
        HTML html = new HTML(context.render(this.currentItem));
        html.setStyleName("finder-tooltip-content");
        this.popup.setWidget(html);

        switch (this.popup.getArrow())
        {
            case BOTTOM:
            {
                int left = currentAnchor.getAbsoluteLeft();
                int top = currentAnchor.getAbsoluteTop();
                int width = currentAnchor.getOffsetWidth();
                this.popup.setPopupPosition(
                        left+20,
                        top-40
                );
                break;
            }
            default: {
                this.popup.setPopupPosition(
                        currentAnchor.getAbsoluteLeft(),
                        currentAnchor.getAbsoluteTop()
                );
            }

        }

        this.popup.setAutoHideEnabled(true);
        this.popup.show();
    }

    /**
     * prepare the displaying of the tooltip
     * or ignore the call when it's already active
     */
    public void prepare(Element anchor, T item) {
        this.currentAnchor = anchor;
        this.currentItem = item;
        if(!this.timer.isRunning())
        {
            timer.schedule(DELAY_MS);
        }
    }

    /**
     * If a timer is active or the tooltip is showing discard any of it.
     */
    public void cancel() {
        timer.cancel();
        if(this.popup.isVisible())
            this.popup.hide(true);
    }

    public interface Context<T> {
        SafeHtml render(T item);
    }
}
