package org.jboss.as.console.client.widgets.popups;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * @author Heiko Braun
 * @date 10/8/12
 */
public class DefaultPopup extends PopupPanel {

    public static enum Arrow {NONE, TOP, TOPLEFT, BOTTOM, RIGHT, RIGHTTOP}

    private Arrow arrow = Arrow.NONE;

    public DefaultPopup() {
        this(Arrow.NONE, false);
    }

    public DefaultPopup(Arrow arrow) {
        this(arrow, false);
    }

    public DefaultPopup(Arrow arrow, boolean isTooltip) {
        super(true);
        this.arrow = arrow;

        getElement().setAttribute("role", "alert");
        getElement().setAttribute("aria-live", "assertive");

        this.sinkEvents(Event.ONKEYDOWN);

        addStyleName("default-popup");
        if(isTooltip) addStyleName("tooltip");

        String baseCss = isTooltip ? "tooltip-triangle-border": "triangle-border";

        if(Arrow.TOP.equals(arrow))
        {
            addStyleName(baseCss);
            addStyleName("top");
        }
        else if(Arrow.TOPLEFT.equals(arrow))
        {
            addStyleName(baseCss);
            addStyleName("top-left");
        }
        else if(Arrow.BOTTOM.equals(arrow))
        {
            addStyleName(baseCss);
        }
        else if(Arrow.RIGHT.equals(arrow))
        {
            addStyleName(baseCss);
            addStyleName("right");
        }
        else if(Arrow.RIGHTTOP.equals(arrow))
        {
            addStyleName(baseCss);
            addStyleName("right-top");
        }

        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent resizeEvent) {
                hide();
            }
        });


        setAutoHideEnabled(true);
        setAutoHideOnHistoryEventsEnabled(true);

    }

    public Arrow getArrow() {
        return arrow;
    }

    @Override
    protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
        if (Event.ONKEYDOWN == event.getTypeInt()) {
            if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                // Dismiss when escape is pressed
                hide();
            }
        }
    }
}
