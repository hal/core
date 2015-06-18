package org.jboss.as.console.client.shared.subsys.security.v3;

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
public class SecDomainFinderView extends SuspendableViewImpl implements SecDomainFinder.MyView {

    private final PlaceManager placeManager;
    private LayoutPanel previewCanvas;
    private SplitLayoutPanel layout;
    private FinderColumn<Property> securityDomains;
    private SecDomainFinder presenter;
    private ColumnManager columnManager;
    private Widget secDomainCol;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Inject
    public SecDomainFinderView(PlaceManager placeManager) {

        this.placeManager = placeManager;
    }

    @Override
    public void setPresenter(SecDomainFinder presenter) {

        this.presenter = presenter;
    }

    @Override
    public void updateFrom(List<Property> list) {
        securityDomains.updateFrom(list);
    }

    @Override
    public Widget createWidget() {

        previewCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.CONFIGURATION);

        securityDomains = new FinderColumn<Property>(
                FinderColumn.FinderId.CONFIGURATION,
                "Security Domain",
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

        securityDomains.setTopMenuItems(
                new MenuDelegate<Property>(
                        "Add", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property container) {
                        presenter.onLauchAddDomain();
                    }
                })
        );


        securityDomains.setMenuItems(
                new MenuDelegate<Property>(
                        "View", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property cacheContainer) {
                        placeManager.revealRelativePlace(
                                new PlaceRequest(NameTokens.SecDomain).with("domain", cacheContainer.getName())
                        );
                    }
                }),
                new MenuDelegate<Property>(
                        "Properties", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property container) {

                        if (securityDomains.hasSelectedItem()) {
                            presenter.onLaunchDomainSettings(securityDomains.getSelectedItem());
                        }
                    }
                }),
                new MenuDelegate<Property>(
                        "Remove", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property container) {

                        if (securityDomains.hasSelectedItem()) {
                            Property selectedItem = securityDomains.getSelectedItem();
                            Feedback.confirm(Console.MESSAGES.deleteTitle("Security Domain"),
                                    Console.MESSAGES.deleteConfirm(selectedItem.getName()),
                                    new Feedback.ConfirmationHandler() {
                                        @Override
                                        public void onConfirmation(boolean isConfirmed) {
                                            if (isConfirmed) {
                                                presenter.onRemove(selectedItem);
                                            }
                                        }
                                    });
                        }
                    }
                })
        );

        securityDomains.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (securityDomains.hasSelectedItem()) {
                    Property item = securityDomains.getSelectedItem();
                    columnManager.updateActiveSelection(secDomainCol);
                }
            }
        });

        secDomainCol = securityDomains.asWidget();

        columnManager.addWest(secDomainCol);
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