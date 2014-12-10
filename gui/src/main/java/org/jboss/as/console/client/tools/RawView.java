package org.jboss.as.console.client.tools;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.as.console.client.widgets.Code;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

/**
 * @author Heiko Braun
 * @date 6/15/12
 */
public class RawView {


    private Code dump;
    private BrowserPresenter presenter;
    private ModelNode currentAddress;

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");
        layout.getElement().setAttribute("style", "padding:10px");

        dump = new Code();

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton("Refresh", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                if(currentAddress!=null)
                {
                    dump.clear();
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            presenter.readResource(currentAddress, false);
                        }
                    });
                }
            }
        }));

        layout.add(tools.asWidget());

        SafeHtmlBuilder helpText = new SafeHtmlBuilder();
        helpText.appendHtmlConstant("<ul>");
        helpText.appendHtmlConstant("<li>");
        helpText.appendEscaped("This is a raw dump of the current configuration values on this node. It only resolves values on the current level, hence some attributes (or children) may show up as UNDEFINED.");
        helpText.appendHtmlConstant("</ul>");
        StaticHelpPanel help = new StaticHelpPanel(helpText.toSafeHtml());
        layout.add(help.asWidget());

        layout.add(dump);

        return layout;
    }

    public void display(ModelNode address, Property model)
    {
        currentAddress = address;
        dump.setVisible(true);
        dump.setValue(SafeHtmlUtils.fromString(model.getValue().toString()));
    }

    public void clearDisplay()
    {
        currentAddress = null;
        // to prevent an empty code widget which would render as an empty border box
        dump.setVisible(false);
        dump.clear();
    }

    public void setPresenter(BrowserPresenter presenter) {
        this.presenter = presenter;
    }
}
