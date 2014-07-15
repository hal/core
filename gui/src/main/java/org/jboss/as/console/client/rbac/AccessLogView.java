package org.jboss.as.console.client.rbac;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

import java.util.Iterator;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public class AccessLogView {

    private ResourceAccessLog log = ResourceAccessLog.INSTANCE;

    private Tree tree;

    public AccessLogView() {
        this.tree = new Tree();

        log.addListener(new ResourceAccessLog.Listener() {
            @Override
            public void onChange() {
                updateTreee();
            }
        });
    }

    private void updateTreee() {
        tree.removeItems();

        Iterator<String> keys = log.getKeys();

        while(keys.hasNext())
        {
            String token = keys.next();
            Set<String> addresses = log.getAddresses(token);

            SafeHtmlBuilder sh = new SafeHtmlBuilder();
            TreeItem parent = tree.addItem(sh.appendEscaped(token+" ("+addresses.size()+")").toSafeHtml());

            for(String address : addresses)
            {
                SafeHtmlBuilder sh2 = new SafeHtmlBuilder();
                parent.addItem(sh2.appendEscaped(address).toSafeHtml());
            }
        }
    }

    public Widget asWidget() {


        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("fill-layout-width");


        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton("Reset", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                log.flush();
            }
        }));

        panel.add(tools);
        panel.add(tree);

        // update
        updateTreee();

        return panel;
    }

}
