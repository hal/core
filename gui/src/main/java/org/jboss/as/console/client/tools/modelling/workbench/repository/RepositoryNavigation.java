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
import org.jboss.as.console.client.tools.modelling.workbench.repository.vfs.Entry;
import org.jboss.ballroom.client.widgets.common.DefaultButton;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;

import java.util.List;
import java.util.Stack;

/**
 * @author Heiko Braun
 * @date 10/6/13
 */
public class RepositoryNavigation {

    private RepositoryPresenter presenter;

    private HorizontalPanel breadcrumb ;
    private DefaultCellTable<Entry> fileSystem;
    private Stack<Entry> history = new Stack<Entry>();
    private ListDataProvider<Entry> fileSystemProvider;
    private SingleSelectionModel<Entry> fsSelection;

    public void setPresenter(RepositoryPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {

        breadcrumb = new HorizontalPanel();

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("fill-layout-width");
        panel.getElement().setAttribute("style", "padding:10px");


        // ----

        fileSystem = new DefaultCellTable<Entry>(30);

        fileSystemProvider = new ListDataProvider<Entry>();
        fileSystemProvider.addDataDisplay(fileSystem);

        TextColumn<Entry> entryCol = new TextColumn<Entry>() {
            @Override
            public String getValue(Entry entry) {
                return entry.getName();
            }
        };

        fileSystem.addColumn(entryCol);
        fileSystem.getElement().setAttribute("style", "margin-bottom:10px;");

        panel.add(breadcrumb);
        panel.add(fileSystem);


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

        // ----

        fsSelection  = new SingleSelectionModel<Entry>();
        fileSystem.setSelectionModel(fsSelection);

        fsSelection.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Entry selection = fsSelection.getSelectedObject();
                if(selection!=null)
                {
                    if(Entry.Type.DIR == selection.getType())
                    {
                        // directories
                        Entry dir = history.peek();
                        presenter.loadDir(new Entry(dir.getName() + selection.getName(), Entry.Type.DIR), false);
                    }
                    else
                    {
                        // files
                        presenter.loadFile(selection);
                    }
                }
            }
        });


        return panel;
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

    public void updateDirectory(Entry dir, List<Entry> entries) {

        // keep a history
        history.push(dir);

        fileSystemProvider.getList().clear();
        fileSystemProvider.getList().addAll(entries);
        updateBreadcrump();
    }

    public void clearHistory() {
        history.clear();
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

                    if(fsSelection.getSelectedObject()!=null)
                        fsSelection.setSelected(fsSelection.getSelectedObject(), false);

                    presenter.loadDir(item, false);
                }
            });

        }
    }

    class NavButton extends DefaultButton
    {
        NavButton(String title, ClickHandler handler) {
            super(title, handler);
            getElement().setAttribute("style", "margin-bottom:5px; min-width:200px; text-align:left");
        }
    }
}
