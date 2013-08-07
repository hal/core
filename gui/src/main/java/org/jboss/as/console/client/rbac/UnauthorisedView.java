package org.jboss.as.console.client.rbac;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PopupViewImpl;
import org.jboss.as.console.client.Console;
import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import javax.inject.Inject;

/**
 * @author Heiko Braun
 * @date 7/24/13
 */
public class UnauthorisedView extends PopupViewImpl implements UnauthorisedPresenter.MyView {

    private final DefaultWindow window;
    private final HTML html;
    private UnauthorisedPresenter presenter;
    private final static  String DESC = "You don't have the permissions to access these resources:<p/>";

    @Inject
    UnauthorisedView(EventBus eventBus) {
        super(eventBus);

        html = new HTML(DESC);

        window = new DefaultWindow("Authorisation Required");
        DialogueOptions options = new DialogueOptions(
                "OK",
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        handleConfirmation();
                    }
                },
                Console.CONSTANTS.common_label_cancel(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        handleConfirmation();
                    }
                }
        );


        window.setWidget(new WindowContentBuilder(html, options).build());
        window.setWidth(480);
        window.setHeight(360);
        window.setGlassEnabled(true);

        /*window.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                presenter.onConfirmation();
            }
        }) ;*/

        initWidget(window);

        setAutoHideOnNavigationEventEnabled(true);
    }

    @Override
    public void setLastDecision(AuthorisationDecision decision) {

        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant(DESC);
        builder.appendHtmlConstant("<ul>");
        for(String s : decision.getErrorMessages())
            builder.appendHtmlConstant("<li>").appendEscaped(s).appendHtmlConstant("</li>");
        builder.appendHtmlConstant("</ul>");
        html.setHTML(builder.toSafeHtml());
    }

    @Override
    public void setPresenter(UnauthorisedPresenter presenter) {
        this.presenter = presenter;
    }

    private void handleConfirmation() {
        window.hide();

    }

    @Override
    public void center() {
        window.center();
    }

    @Override
    public void hide() {
      window.hide();
    }

    @Override
    public void show() {
        window.show();
    }
}
