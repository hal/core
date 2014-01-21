package org.jboss.as.console.client.core.bootstrap;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

public class EagerLoadGroups implements Function<BootstrapContext> {

    private final ServerGroupStore groupStore;

    public EagerLoadGroups(ServerGroupStore groupStore) {
        this.groupStore = groupStore;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {
        final BootstrapContext context = control.getContext();

        if (!context.isStandalone()) {
            groupStore.loadServerGroups(new AsyncCallback<List<ServerGroupRecord>>() {
                @Override
                public void onFailure(Throwable caught) {
                    context.setlastError(caught);
                    control.abort();
                }

                @Override
                public void onSuccess(List<ServerGroupRecord> result) {
                    if (result.isEmpty()) {
                        final DefaultWindow window = new DefaultWindow(Console.MESSAGES.no_groups_header());
                        window.setWidth(480);
                        window.setHeight(220);
                        window.trapWidget(new NoGroupsWarning(new ClickHandler() {
                            @Override
                            public void onClick(final ClickEvent event) {
                                window.hide();
                                context.setGroupManagementDisabled(true);
                                control.proceed();
                            }
                        }).asWidget());
                        window.setGlassEnabled(true);
                        window.center();
                    } else {
                        Set<String> groups = new TreeSet<String>();
                        for (ServerGroupRecord group : result) { groups.add(group.getName()); }
                        context.setAdressableGroups(groups);
                        control.proceed();
                    }
                }
            });
        } else {
            // standalone
            control.proceed();
        }
    }

    private class NoGroupsWarning implements IsWidget {

        private final ClickHandler dismiss;

        public NoGroupsWarning(final ClickHandler dismiss) {
            this.dismiss = dismiss;
        }

        @Override
        public Widget asWidget() {
            VerticalPanel layout = new VerticalPanel();
            layout.setStyleName("window-content");
            layout.add(new Label(Console.MESSAGES.no_groups_warning()));

            DialogueOptions options = new DialogueOptions(
                    Console.CONSTANTS.common_label_done(), dismiss,
                    Console.CONSTANTS.common_label_cancel(), dismiss);
            options.showCancel(false);
            return new WindowContentBuilder(new ScrollPanel(layout), options).build();
        }
    }
}
