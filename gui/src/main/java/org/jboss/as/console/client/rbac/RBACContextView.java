package org.jboss.as.console.client.rbac;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Heiko Braun
 * @date 8/7/13
 */
public class RBACContextView {

    private SecurityFramework securityFramework = Console.MODULES.getSecurityFramework();

    public void launch() {
        final DefaultWindow window = new DefaultWindow("RBAC Diagnostics");

        LayoutPanel inner = new LayoutPanel();
        inner.setStyleName("default-window-content");
        inner.addStyleName("rbac-diagnostics");

        ClickHandler clickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                window.hide();
            }
        };
        Widget content = new WindowContentBuilder(asWidget(), new DialogueOptions(
                "Done", clickHandler, "Cancel", clickHandler)
        ).build();

        inner.add(content);

        window.setWidget(inner);

        window.setWidth(480);
        window.setHeight(360);
        window.center();
    }


    private Widget asWidget() {
        VerticalPanel container = new VerticalPanel();
        container.setStyleName("fill-layout");

        HorizontalPanel menu = new HorizontalPanel();
        menu.setStyleName("fill-layout-width");
        final TextBox nameBox = new TextBox();
        nameBox.setText(securityFramework.resolveToken());

        MultiWordSuggestOracle oracle = new  MultiWordSuggestOracle();
        oracle.addAll(Console.MODULES.getRequiredResourcesRegistry().getTokens());

        SuggestBox suggestBox = new SuggestBox(oracle, nameBox);

        Button btn = new Button("Show", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                container.clear();

                try {
                    container.add(createContent(nameBox.getText()));
                } catch (Throwable e) {
                    HTML msg = new HTML(e.getMessage());
                    msg.getElement().getStyle().setColor("red");
                    container.add(msg);
                }
            }
        });
        menu.add(new HTML("Token: "));
        menu.add(suggestBox);
        menu.add(btn);


        VerticalPanel p = new VerticalPanel();
        p.setStyleName("fill-layout-width");
        p.add(menu);
        p.add(container);
        return p;
    }

    private Widget createContent(String token) {


        SecurityContext securityContext = securityFramework.getSecurityContext(token);

        if(!securityFramework.hasContext(token))
        {
            return new HTML("Security context has not been created (yet). Probably the presenter has not been revealed.");
        }
        else
        {
            if(securityContext instanceof SecurityContextImpl)
            {
                return new HTML(((SecurityContextImpl)securityContext).asHtml());
            }
            else
            {
                return new HTML("Security context is read-only: "+ securityContext.getClass());
            }
        }

    }
}
