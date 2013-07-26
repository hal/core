package org.jboss.as.console.client.administration.role;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.widgets.browser.DefaultCellBrowser;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 * @date 07/25/2013
 */
public class RoleAssignmentEditor implements IsWidget {

    private final Principal.Type principalType;
    private final BeanFactory beanFactory;
    private final DispatchAsync dispatcher;
    private RoleAssignmentPresenter presenter;
    private RoleAssignmentTreeModel treeModel;
    private ToolButton addButton;
    private ToolButton deleteButton;
    private boolean roleSelected;
    private boolean roleAssignmentSelected;
    private boolean principalSelected;


    public RoleAssignmentEditor(final Principal.Type principalType, final BeanFactory beanFactory,
            final DispatchAsync dispatcher) {
        this.principalType = principalType;
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
    }

    @Override
    public Widget asWidget() {

        treeModel = new RoleAssignmentTreeModel(principalType, beanFactory, dispatcher);
        treeModel.getRoleSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                roleSelected = treeModel.getRoleSelectionModel().getSelectedObject() != null;
                updateToolButtons();
            }
        });
        treeModel.getRoleAssignmentSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                roleAssignmentSelected = treeModel.getRoleAssignmentSelectionModel().getSelectedObject() != null;
                updateToolButtons();
            }
        });
        treeModel.getPrincipalSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                principalSelected = treeModel.getPrincipalSelectionModel().getSelectedObject() != null;
                updateToolButtons();
            }
        });
        DefaultCellBrowser cellBrowser = new DefaultCellBrowser.Builder(treeModel, null).build();
        cellBrowser.setHeight("250px");

        String add = principalType == Principal.Type.USER ? Console.CONSTANTS.role_assignment_add_user() : Console
                .CONSTANTS.role_assignment_add_group();
        String delete = principalType == Principal.Type.USER ? Console.CONSTANTS.role_assignment_delete_user() : Console
                .CONSTANTS.role_assignment_delete_group();

        ToolStrip tools = new ToolStrip();
        addButton = new ToolButton(add, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                StandardRole role = treeModel.getRoleSelectionModel().getSelectedObject();
                RoleAssignment roleAssignment = treeModel.getRoleAssignmentSelectionModel().getSelectedObject();
                if (role != null && roleAssignment != null) {
                    presenter.launchAddDialg(role, roleAssignment, principalType);
                } else {
                    // TODO Change this to a console message?
                    Log.warn("No role or no includes/excludes selected");
                }
            }
        });
        addButton.setEnabled(false);
        tools.addToolButtonRight(addButton);
        deleteButton = new ToolButton(delete, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final StandardRole role = treeModel.getRoleSelectionModel().getSelectedObject();
                final RoleAssignment roleAssignment = treeModel.getRoleAssignmentSelectionModel().getSelectedObject();
                final Principal principal = treeModel.getPrincipalSelectionModel().getSelectedObject();
                if (role != null && roleAssignment != null && principal != null) {
                    String principalTypeName = principalType == Principal.Type.USER ? Console.CONSTANTS
                            .common_label_user() : Console.CONSTANTS.common_label_group();
                    Feedback.confirm(
                            Console.MESSAGES.deleteTitle(principal.getName()),
                            Console.MESSAGES.deleteConfirm(principalTypeName + " " + principal.getName()),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean isConfirmed) {
                                    if (isConfirmed) {
                                        presenter.onDelete(role, roleAssignment, principal);
                                    }
                                }
                            });
                } else {
                    // TODO Change this to a console message?
                    Log.warn("No role or no includes/excludes or no principal selected");
                }
            }
        });
        deleteButton.setEnabled(false);
        tools.addToolButtonRight(deleteButton);

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setStyleName("rhs-content-panel");
        vpanel.add(tools);
        vpanel.add(cellBrowser);
        return vpanel;
    }

    private void updateToolButtons() {
        System.out.println(
                "roleSelected && roleAssignmentSelected && principalSelected: " + roleSelected + " && " + roleAssignmentSelected + " && " + principalSelected);
        addButton.setEnabled(roleSelected && roleAssignmentSelected);
        deleteButton.setEnabled(roleSelected && roleAssignmentSelected && principalSelected);
    }

    public void setPresenter(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
    }

    public void refresh() {
        final RoleAssignment roleAssignment = treeModel.getRoleAssignmentSelectionModel().getSelectedObject();
        treeModel.refreshRoleAssignments(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                treeModel.getRoleAssignmentSelectionModel().setSelected(roleAssignment, true);
            }
        });
    }
}
