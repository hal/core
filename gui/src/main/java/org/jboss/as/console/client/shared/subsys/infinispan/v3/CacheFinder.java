package org.jboss.as.console.client.shared.subsys.infinispan.v3;

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
public class CacheFinder extends SuspendableViewImpl implements CacheFinderPresenter.MyView {

    private final PlaceManager placeManager;
    private LayoutPanel previewCanvas;
    private SplitLayoutPanel layout;
    private FinderColumn<Property> cacheContainer;
    private CacheFinderPresenter presenter;
    private ColumnManager columnManager;
    private Widget mailSessCol;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Inject
    public CacheFinder(PlaceManager placeManager) {

        this.placeManager = placeManager;
    }

    @Override
    public void setPresenter(CacheFinderPresenter presenter) {

        this.presenter = presenter;
    }

    @Override
    public void updateFrom(List<Property> list) {
        cacheContainer.updateFrom(list);
    }

    @Override
    public Widget createWidget() {

        previewCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout);

        cacheContainer = new FinderColumn<Property>(
                FinderColumn.FinderId.CONFIGURATION,
                "Cache Container",
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
                })
        ;

        cacheContainer.setTopMenuItems(
                new MenuDelegate<Property>(
                        "<i class=\"icon-plus\" style='color:black'></i>&nbsp;Add", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property container) {
                        presenter.onLauchAddContainer();
                    }
                })
        );


        cacheContainer.setMenuItems(
                new MenuDelegate<Property>(
                        "View", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property cacheContainer) {
                        placeManager.revealRelativePlace(
                                new PlaceRequest(NameTokens.CachesPresenter).with("container", cacheContainer.getName())
                        );
                    }
                }),
                new MenuDelegate<Property>(
                        "Container Settings", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property cacheContainer) {
                        presenter.onLaunchContainerSettings(cacheContainer);
                    }
                }),
                new MenuDelegate<Property>(
                        "Transport Settings", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property cacheContainer) {
                        presenter.onLaunchTransportSettings(cacheContainer);
                    }
                }),
                new MenuDelegate<Property>(
                        "Remove", new ContextualCommand<Property>() {
                    @Override
                    public void executeOn(Property container) {

                        if(cacheContainer.hasSelectedItem()) {
                            Property selectedItem = cacheContainer.getSelectedItem();
                            Feedback.confirm(Console.MESSAGES.deleteTitle("Cache Container"),
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

        cacheContainer.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (cacheContainer.hasSelectedItem()) {
                    Property item = cacheContainer.getSelectedItem();
                    columnManager.updateActiveSelection(mailSessCol);
                }
            }
        });

        mailSessCol = cacheContainer.asWidget();

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