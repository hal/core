package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Command;
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
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.ballroom.client.widgets.window.Feedback;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 27/02/15
 */
public class DataSourceFinderView extends SuspendableViewImpl implements DataSourceFinder.MyView {

    private final PlaceManager placeManager;
    private final ArrayList<FinderItem> types;
    private LayoutPanel previewCanvas;
    private SplitLayoutPanel layout;
    private FinderColumn<DataSource> datasources;
    private DataSourceFinder presenter;
    private ColumnManager columnManager;
    private Widget datasourceColWidget;
    private FinderColumn<FinderItem> typeColumn;
    private Widget typeColWidget;
    private FinderColumn<XADataSource> xadatasources;
    private Widget xaColWidget;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title, String jndiName);

        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Inject
    public DataSourceFinderView(PlaceManager placeManager) {

        this.placeManager = placeManager;

        this.types = new ArrayList<FinderItem>(2);
        types.add(new FinderItem("Non-XA", new Command() {
            @Override
            public void execute() {
                columnManager.reduceColumnsTo(1);
                columnManager.appendColumn(datasourceColWidget);
                presenter.loadDatasources();
            }
        }, true));

        types.add(new FinderItem("XA", new Command() {
            @Override
            public void execute() {
                columnManager.reduceColumnsTo(1);
                columnManager.appendColumn(xaColWidget);
                presenter.loadXADatasources();
            }
        }, true));

    }

    @Override
    public void setPresenter(DataSourceFinder presenter) {

        this.presenter = presenter;
    }

    @Override
    public void updateFrom(List<DataSource> list) {
        datasources.updateFrom(list);
    }

    @Override
    public Widget createWidget() {

        previewCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.CONFIGURATION);

        typeColumn = new FinderColumn<FinderItem>(
                FinderColumn.FinderId.CONFIGURATION,
                "Type",
                new FinderColumn.Display<FinderItem>() {

                    @Override
                    public boolean isFolder(FinderItem data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, FinderItem data) {
                        return TEMPLATE.item(baseCss, data.getTitle());
                    }

                    @Override
                    public String rowCss(FinderItem data) {
                        return "no-menu";
                    }
                },
                new ProvidesKey<FinderItem>() {
                    @Override
                    public Object getKey(FinderItem item) {
                        return item.getTitle();
                    }
                }, presenter.getProxy().getNameToken());


        datasources = new FinderColumn<DataSource>(
                FinderColumn.FinderId.CONFIGURATION,
                "Datasource",
                new FinderColumn.Display<DataSource>() {

                    @Override
                    public boolean isFolder(DataSource data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(String baseCss, DataSource data) {
                        return TEMPLATE.item(baseCss, data.getName(), data.getJndiName());
                    }

                    @Override
                    public String rowCss(DataSource data) {
                        String css = data.isEnabled() ? "active" : "inactive";
                        return css;
                    }
                },
                new ProvidesKey<DataSource>() {
                    @Override
                    public Object getKey(DataSource item) {
                        return item.getName();
                    }
                }, presenter.getProxy().getNameToken())
        ;

        datasources.setTopMenuItems(
                new MenuDelegate<DataSource>(
                        "Add", new ContextualCommand<DataSource>() {
                    @Override
                    public void executeOn(DataSource ds) {
                        presenter.launchNewDatasourceWizard();
                    }
                }, MenuDelegate.Role.Operation)
        );


        datasources.setMenuItems(
                new MenuDelegate<DataSource>(
                        "View", new ContextualCommand<DataSource>() {
                    @Override
                    public void executeOn(DataSource ds) {
                        placeManager.revealRelativePlace(
                                new PlaceRequest(NameTokens.DataSourcePresenter).with("name", ds.getName())
                        );
                    }
                }),
                new MenuDelegate<DataSource>(
                        "Remove", new ContextualCommand<DataSource>() {
                    @Override
                    public void executeOn(DataSource ds) {

                        Feedback.confirm(
                                Console.MESSAGES.deleteTitle("Datasource"),
                                Console.MESSAGES.deleteConfirm("Datasource " + ds.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed) {
                                            presenter.onDelete(ds);
                                        }
                                    }
                                });

                    }
                }, MenuDelegate.Role.Operation),

                new MenuDelegate<DataSource>("En/Disable",
                        new ContextualCommand<DataSource>(){
                            @Override
                            public void executeOn(DataSource item) {

                                final boolean nextState = !item.isEnabled();
                                String title = nextState ? Console.MESSAGES.enableConfirm("datasource") : Console.MESSAGES.disableConfirm("datasource");
                                String text = nextState ? Console.MESSAGES.enableConfirm("datasource "+item.getName()) : Console.MESSAGES.disableConfirm("datasource "+item.getName()) ;
                                Feedback.confirm(title, text,
                                        new Feedback.ConfirmationHandler() {
                                            @Override
                                            public void onConfirmation(boolean isConfirmed) {
                                                if (isConfirmed) {
                                                    presenter.onDisable(item, nextState);
                                                }
                                            }
                                        });
                            }
                        },
                        MenuDelegate.Role.Operation
                )
                {
                    @Override
                    public String render(DataSource data) {
                        return data.isEnabled() ? "Disable" : "Enable";
                    }
                }
        );

        // -------------


        xadatasources = new FinderColumn<XADataSource>(
                FinderColumn.FinderId.CONFIGURATION,
                "XA Datasource",
                new FinderColumn.Display<XADataSource>() {

                    @Override
                    public boolean isFolder(XADataSource data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(String baseCss, XADataSource data) {
                        return TEMPLATE.item(baseCss, data.getName(), data.getJndiName());
                    }

                    @Override
                    public String rowCss(XADataSource data) {
                        String css = data.isEnabled() ? "active" : "inactive";
                        return css;
                    }
                },
                new ProvidesKey<XADataSource>() {
                    @Override
                    public Object getKey(XADataSource item) {
                        return item.getName();
                    }
                }, presenter.getProxy().getNameToken())
        ;

        xadatasources.setTopMenuItems(
                new MenuDelegate<XADataSource>(
                        "Add", new ContextualCommand<XADataSource>() {
                    @Override
                    public void executeOn(XADataSource ds) {
                        presenter.launchNewXADatasourceWizard();
                    }
                }, MenuDelegate.Role.Operation)
        );


        xadatasources.setMenuItems(
                new MenuDelegate<XADataSource>(
                        "View", new ContextualCommand<XADataSource>() {
                    @Override
                    public void executeOn(XADataSource ds) {
                        placeManager.revealRelativePlace(
                                new PlaceRequest(NameTokens.XADataSourcePresenter).with("name", ds.getName())
                        );
                    }
                }),
                new MenuDelegate<XADataSource>(
                        "Remove", new ContextualCommand<XADataSource>() {
                    @Override
                    public void executeOn(XADataSource ds) {

                        Feedback.confirm(
                                Console.MESSAGES.deleteTitle("XA Datasource"),
                                Console.MESSAGES.deleteConfirm("XA Datasource "+ds.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed) {
                                            presenter.onDeleteXA(ds);
                                        }
                                    }
                                });


                    }
                }, MenuDelegate.Role.Operation),

                new MenuDelegate<XADataSource>("En/Disable",
                        new ContextualCommand<XADataSource>(){
                            @Override
                            public void executeOn(XADataSource item) {

                                final boolean nextState = !item.isEnabled();
                                String title = nextState ? Console.MESSAGES.enableConfirm("XA datasource") : Console.MESSAGES.disableConfirm("datasource");
                                String text = nextState ? Console.MESSAGES.enableConfirm("XA datasource "+item.getName()) : Console.MESSAGES.disableConfirm("datasource "+item.getName()) ;
                                Feedback.confirm(title, text,
                                        new Feedback.ConfirmationHandler() {
                                            @Override
                                            public void onConfirmation(boolean isConfirmed) {
                                                if (isConfirmed) {
                                                    presenter.onDisableXA(item, nextState);
                                                }
                                            }
                                        });
                            }
                        },
                        MenuDelegate.Role.Operation
                )

                {
                    @Override
                    public String render(XADataSource data) {
                        return data.isEnabled() ? "Disable" : "Enable";
                    }
                }
        );

        typeColWidget = typeColumn.asWidget();
        datasourceColWidget = datasources.asWidget();
        xaColWidget = xadatasources.asWidget();

        columnManager.addWest(typeColWidget);
        columnManager.addWest(datasourceColWidget);
        columnManager.addWest(xaColWidget);
        columnManager.add(previewCanvas);

        columnManager.setInitialVisible(1);

        // selection handlers
        typeColumn.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                columnManager.reduceColumnsTo(1);
                if(typeColumn.hasSelectedItem())
                {
                    FinderItem item = typeColumn.getSelectedItem();
                    item.getCmd().execute();
                }
            }

        });

        datasources.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if(datasources.hasSelectedItem())
                {
                    DataSource item = datasources.getSelectedItem();
                    columnManager.updateActiveSelection(datasourceColWidget);

                }
            }
        });


        xadatasources.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if(xadatasources.hasSelectedItem())
                {
                    DataSource item = xadatasources.getSelectedItem();
                    columnManager.updateActiveSelection(xaColWidget);

                }
            }
        });

        // types column items
        typeColumn.updateFrom(types);
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

    @Override
    public void updateDataSources(List<DataSource> list) {
        datasources.updateFrom(list);
    }

    @Override
    public void updateXADataSources(List<XADataSource> list) {
        xadatasources.updateFrom(list);
    }

    @Override
    public void showVerifyConncectionResult(final String name, final VerifyConnectionOp.VerifyResult result) {
        new ConnectionWindow(name, result).show();
    }
}
