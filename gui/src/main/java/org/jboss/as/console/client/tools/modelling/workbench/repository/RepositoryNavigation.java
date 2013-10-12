package org.jboss.as.console.client.tools.modelling.workbench.repository;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.tools.modelling.workbench.repository.vfs.Entry;
import org.jboss.as.console.client.tools.modelling.workbench.repository.vfs.Vfs;
import org.jboss.ballroom.client.widgets.common.DefaultButton;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * @author Heiko Braun
 * @date 10/6/13
 */
public class RepositoryNavigation {

    private RepositoryPresenter presenter;
    private DefaultCellTable<DialogRef> table;
    private ListDataProvider<DialogRef> dataProvider;

    private Vfs vfs;
    private Stack<Entry> history = new Stack<Entry>();
    private HorizontalPanel breadcrumb ;
    private DefaultCellTable<Entry> fileSystem;
    private ListDataProvider<Entry> fileSystemProvider;

    public void setPresenter(RepositoryPresenter presenter) {
        this.presenter = presenter;
        this.vfs = new Vfs();
    }

    Widget asWidget() {

        breadcrumb = new HorizontalPanel();

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("fill-layout-width");
        panel.getElement().setAttribute("style", "padding:10px");

        table = new DefaultCellTable<DialogRef>(
                5,
                new ProvidesKey<DialogRef>() {
                    @Override
                    public Object getKey(DialogRef item) {
                        return item.getName();
                    }
                });

        dataProvider = new ListDataProvider<DialogRef>();
        dataProvider.addDataDisplay(table);


        TextColumn<DialogRef> nameColumn = new TextColumn<DialogRef>() {
            @Override
            public String getValue(DialogRef dialogRef) {
                return dialogRef.getName();
            }
        };
        table.addColumn(nameColumn, "Dialog");

        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);

        pager.getElement().setAttribute("style", "margin-bottom:10px;");

        // --
        Button visualize = new NavButton("Visualize", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onVisualize();
            }
        });
        panel.add(visualize);

        Button reify = new NavButton("Reify", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onReify();
            }
        });
        panel.add(reify);

        Button activate = new NavButton("Activate", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onActivate();
            }
        });
        panel.add(activate);

        Button reset = new NavButton("Reset", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onResetDialog();
            }
        });
        panel.add(reset);

        Button passivate = new NavButton("Passivate", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onPassivate();
            }
        });
        panel.add(passivate);

        CheckBox cacheDisabled = new CheckBox("Disable Cache");
        cacheDisabled.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                presenter.setDisableCache(event.getValue());
            }
        });
        panel.add(cacheDisabled);


        final SingleSelectionModel<DialogRef> selectionModel = new SingleSelectionModel<DialogRef>();
        table.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                presenter.setActiveDialog(selectionModel.getSelectedObject());
                presenter.onMarshall();
            }
        });


        // ---

        fileSystem = new DefaultCellTable<Entry>(
                5,
                new ProvidesKey<Entry>() {
                    @Override
                    public Object getKey(Entry item) {
                        return item.getName();
                    }
                });

        fileSystemProvider = new ListDataProvider<Entry>();
        fileSystemProvider.addDataDisplay(fileSystem);

        TextColumn<Entry> entryCol = new TextColumn<Entry>() {
            @Override
            public String getValue(Entry entry) {
                return entry.getName();
            }
        };

        fileSystem.addColumn(entryCol);


        final SingleSelectionModel<Entry> entrySelection  = new SingleSelectionModel<Entry>();
        fileSystem.setSelectionModel(entrySelection);

        entrySelection.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Entry selection = entrySelection.getSelectedObject();
                if(selection!=null)
                {
                    if(Entry.Type.DIR == selection.getType())
                    {
                        // directories
                        Entry dir = history.peek();
                        loadDir(new Entry(dir.getName() + selection.getName(), Entry.Type.DIR));
                    }
                    else
                    {
                        // files

                    }
                }
            }
        });

        Button loadDir = new Button("Init", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                history.clear();
                loadDir(Entry.ROOT);
            }
        });

        panel.add(loadDir);
        panel.add(breadcrumb);
        panel.add(fileSystem);

        return panel;
    }

    private void loadDir(final Entry dir) {

        vfs.listEntries(
                dir,
                new SimpleCallback<List<Entry>>() {
                    @Override
                    public void onSuccess(List<Entry> result) {

                        // keep a history
                        history.push(dir);
                        fileSystemProvider.getList().clear();
                        fileSystemProvider.getList().addAll(result);

                        updateBreadcrump();

                    }
                });
    }

    private void updateBreadcrump() {
        breadcrumb.clear();

        int i=1;
        for(final Entry item : history)
        {
            String name = item.getName().equals("/") ? "/Root" : item.getName();
            HTML link = new HTML(name);
            link.addClickHandler(new BreadcrumbClick(i, item));
            breadcrumb.add(link);
            i++;
        }

    }

    class BreadcrumbClick implements ClickHandler
    {
        final int index;
        private final Entry item;

        BreadcrumbClick(int index, Entry item) {
            this.index = index;
            this.item = item;
        }

        @Override
        public void onClick(ClickEvent event) {

            // clear history
            for(int i=history.size()-index; i>=0; i--)
            {
                history.pop();
            }

            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    loadDir(item);
                }
            });

        }
    }

    public void setDialogNames(Set<DialogRef> dialogs) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(dialogs);

        table.selectDefaultEntity();
    }

    class NavButton extends DefaultButton
    {
        NavButton(String title, ClickHandler handler) {
            super(title, handler);
            getElement().setAttribute("style", "margin-bottom:5px; min-width:200px; text-align:left");
        }
    }
}
