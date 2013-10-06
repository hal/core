package org.jboss.as.console.client.tools.modelling.workbench.repository;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.ballroom.client.widgets.common.DefaultButton;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

import java.util.Set;

/**
 * @author Heiko Braun
 * @date 10/6/13
 */
public class RepositoryNavigation {

    private RepositoryPresenter presenter;
    private Set<String> dialogs;
    private ListBox listBox;
    private DefaultCellTable<DialogRef> table;
    private ListDataProvider<DialogRef> dataProvider;
    public void setPresenter(RepositoryPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {

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
            }
        });


        // ---

        /*TabPanel tabs = new TabPanel();
        tabs.setStyleName("fill-layout-width");

        tabs.add(panel, "Preview");
        tabs.add(new HTML("Hello"), "Edit");

        tabs.selectTab(0);*/

        return panel;
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
