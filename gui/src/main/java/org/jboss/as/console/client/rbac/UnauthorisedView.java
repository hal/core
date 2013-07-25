package org.jboss.as.console.client.rbac;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PopupViewImpl;
import org.jboss.as.console.client.Console;
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
    private UnauthorisedPresenter presenter;

    @Inject
    UnauthorisedView(EventBus eventBus) {
        super(eventBus);

        HTML html = new HTML("You don't have the permissions to access this resource.");

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
        window.setWidth(320);
        window.setHeight(240);
        window.setGlassEnabled(true);

        initWidget(window);

        setAutoHideOnNavigationEventEnabled(true);
    }

    @Override
    public void setPresenter(UnauthorisedPresenter presenter) {
        this.presenter = presenter;
    }

    private void handleConfirmation() {
        window.hide();
        presenter.onConfirmation();

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
