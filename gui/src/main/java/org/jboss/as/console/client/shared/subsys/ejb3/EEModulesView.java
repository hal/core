package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.ejb3.model.Module;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 09/09/14
 */
public class EEModulesView {

    private final EEPresenter presenter;
    ListDataProvider<ModelNode> dataProvider;
    DefaultCellTable<ModelNode> table;
    private SingleSelectionModel<ModelNode> selectionModel;

    public EEModulesView(EEPresenter presenter) {

        this.presenter = presenter;
    }

    Widget asWidget() {

        table = new DefaultCellTable<ModelNode>(5, new ProvidesKey<ModelNode>() {
            @Override
            public Object getKey(ModelNode item) {
                return item.get(NAME).asString()+"_"+item.get("slot").asString();
            }
        });

        dataProvider = new ListDataProvider<ModelNode>();
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<>();
        table.setSelectionModel(selectionModel);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);


        TextColumn<ModelNode> name = new TextColumn<ModelNode>() {
                    @Override
                    public String getValue(ModelNode node) {
                        return node.get(NAME).asString();
                    }
                };


        TextColumn<ModelNode> slot = new TextColumn<ModelNode>() {
                    @Override
                    public String getValue(ModelNode node) {
                        return node.get("slot").asString();
                    }
                };


        table.addColumn(name, "Name");
        table.addColumn(slot, "Slot");

        ToolStrip moduleTools = new ToolStrip();
        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchNewModuleDialogue();
            }
        });
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_eESubsystemView());
        moduleTools.addToolButtonRight(addBtn);

        ToolButton button = new ToolButton(Console.CONSTANTS.common_label_remove(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                final ModelNode module = ((SingleSelectionModel<ModelNode>) table.getSelectionModel()).getSelectedObject();
                if(null==module) return;

                Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Module"),
                        Console.MESSAGES.deleteConfirm("Module"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onRemoveModule(module);
                                }
                            }
                        });
            }
        });
        button.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_remove_eESubsystemView());
        moduleTools.addToolButtonRight(button);

        VerticalPanel modulePanel = new VerticalPanel();
        modulePanel.setStyleName("fill-layout-width");

        modulePanel.add(moduleTools.asWidget());
        modulePanel.add(table.asWidget());
        modulePanel.add(pager);


        modulePanel.getElement().setAttribute("style", "padding-top:5px");

        return modulePanel;
    }

    public void setModules(List<ModelNode> modules) {
        selectionModel.clear();
        dataProvider.setList(modules);
        table.selectDefaultEntity();
    }
}
