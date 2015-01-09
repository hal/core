package org.jboss.as.console.client.widgets;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Heiko Braun
 * @since 03/09/14
 */
public class TwoColumnLayout {

    private Widget centerPanel;
    private Widget westPanel;
    private final int width;

    public TwoColumnLayout(Widget west, Widget center) {
        this(west, center, 217);

    }

    public TwoColumnLayout(Widget west, Widget center, int width) {
        this.centerPanel = center;
        this.westPanel = west;
        this.width = width;

        westPanel.getElement().setAttribute("role", "navigation");
        westPanel.getElement().setId("content-west");
        centerPanel.getElement().setAttribute("role", "main");
        centerPanel.getElement().setId("content-center");

    }

    public Widget asWidget() {
        final CollapsibleSplitLayoutPanel layout = new CollapsibleSplitLayoutPanel(2);


        final LayoutPanel westPanelWrapper = new LayoutPanel();
        westPanelWrapper.setStyleName("fill-layout");

        final HTML collapseButton = new HTML("<i class='icon-double-angle-left'></i>");
        collapseButton.setStyleName("lhs-toggle");
        collapseButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                boolean collapsed = layout.toggleCollapsedState(westPanelWrapper);
                westPanel.setVisible(!collapsed);
                if (collapsed)
                    collapseButton.setHTML("<i class='icon-double-angle-right'></i>");
                else
                    collapseButton.setHTML("<i class='icon-double-angle-left'></i>");

            }
        });

        westPanelWrapper.add(collapseButton);
        westPanelWrapper.add(westPanel);

        // fix display issue
        collapseButton.getElement().getParentElement().getStyle().setOverflow(Style.Overflow.VISIBLE);

        // workaround: the panel always open not collapsed, but the west panel might be invisible
        layout.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if(event.isAttached() && !layout.isCollapsed(westPanelWrapper))
                    westPanel.setVisible(true); // workaround

            }
        });

        westPanelWrapper.setWidgetTopHeight(westPanel, 0, Style.Unit.PX, 100, Style.Unit.PCT);
        westPanelWrapper.setWidgetTopHeight(collapseButton, 5, Style.Unit.PX, 15, Style.Unit.PX);
        westPanelWrapper.setWidgetRightWidth(collapseButton, 5, Style.Unit.PX, 15, Style.Unit.PX);

        layout.addWest(westPanelWrapper, width);
        layout.add(centerPanel);

        //layout.setWidgetMinSize(nav, 15);
        layout.setWidgetMinSize(westPanelWrapper, 20);
        layout.setWidgetToggleDisplayAllowed(westPanelWrapper, true);

        return layout;
    }

}
