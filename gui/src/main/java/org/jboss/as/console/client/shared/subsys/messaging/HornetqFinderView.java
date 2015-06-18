package org.jboss.as.console.client.shared.subsys.messaging;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 27/02/15
 */
public class HornetqFinderView extends SuspendableViewImpl implements HornetqFinder.MyView {

    private final PlaceManager placeManager;
    private LayoutPanel previewCanvas;
    private SplitLayoutPanel layout;
    private FinderColumn<Property> mailSessions;
    private HornetqFinder presenter;
    private ColumnManager columnManager;
    private Widget mailSessCol;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}<br/></div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Inject
    public HornetqFinderView(PlaceManager placeManager) {

        this.placeManager = placeManager;
    }

    @Override
    public void setPresenter(HornetqFinder presenter) {

        this.presenter = presenter;
    }

    @Override
    public void updateFrom(List<Property> list) {
        mailSessions.updateFrom(list);
    }

    @Override
    public Widget createWidget() {

        previewCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.CONFIGURATION);

        mailSessions = new FinderColumn<Property>(
                FinderColumn.FinderId.CONFIGURATION,
                "Messaging Provider",
                new FinderColumn.Display<Property>() {

                    @Override
                    public boolean isFolder(Property data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(String baseCss, Property data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(Property data) {
                        return "";
                    }
                },
                new ProvidesKey<Property>() {
                    @Override
                    public Object getKey(Property item) {
                        return item.getName();
                    }
                }, presenter.getProxy().getNameToken())
        ;

        mailSessions.setTopMenuItems(
                new MenuDelegate<Property>(
                        "Add", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property mailSession) {
                        presenter.launchNewProviderWizard();
                    }
                }, MenuDelegate.Role.Operation)
        );


        mailSessions.setMenuItems(

                new MenuDelegate<Property>(
                        "Destinations", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property provider) {
                        placeManager.revealRelativePlace(
                                new PlaceRequest(NameTokens.MessagingPresenter).with("name", provider.getName())
                        );
                    }
                }),
                new MenuDelegate<Property>(
                        "Connections", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property provider) {
                        placeManager.revealRelativePlace(
                                new PlaceRequest(NameTokens.MsgConnectionsPresenter).with("name", provider.getName())
                        );
                    }
                }),
                new MenuDelegate<Property>(
                        "Clustering", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property provider) {
                        placeManager.revealRelativePlace(
                                new PlaceRequest(NameTokens.MsgClusteringPresenter).with("name", provider.getName())
                        );
                    }
                }),
                new MenuDelegate<Property>(
                        "Provider Settings", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property provider) {
                        presenter.onLaunchProviderSettings(provider);
                    }
                }),
                new MenuDelegate<Property>(
                        "Remove", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property provider) {

                        Feedback.confirm(Console.MESSAGES.deleteTitle("Messaging Provider"),
                                Console.MESSAGES.deleteConfirm("provider "+provider.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed) {
                                            presenter.onDeleteProvider(provider);
                                        }
                                    }
                                });

                    }
                },MenuDelegate.Role.Operation)
        );

        mailSessions.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if(mailSessions.hasSelectedItem())
                {
                    Property item = mailSessions.getSelectedItem();
                    columnManager.updateActiveSelection(mailSessCol);
                }
            }
        });

        mailSessCol = mailSessions.asWidget();

        columnManager.addWest(mailSessCol);
        columnManager.add(previewCanvas);

        columnManager.setInitialVisible(1);

        return layout;
    }

    @Override
    public void setPreview(final SafeHtml html) {

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                previewCanvas.clear();
                previewCanvas.add(new HTML(html));
            }
        });

    }
}
