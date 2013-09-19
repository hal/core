/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.administration.role;

import static org.jboss.as.console.client.administration.role.model.Principal.Type.USER;
import static org.jboss.as.console.client.administration.role.operation.LoadRoleAssignmentsOp.Results;
import static org.jboss.as.console.client.administration.role.operation.ManagementOperation.Operation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.Principals;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.RoleAssignments;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.administration.role.operation.LoadRoleAssignmentsOp;
import org.jboss.as.console.client.administration.role.operation.ManagementOperation;
import org.jboss.as.console.client.administration.role.operation.ModifyRoleAssignmentOp;
import org.jboss.as.console.client.administration.role.operation.ModifyRoleOp;
import org.jboss.as.console.client.administration.role.operation.ShowMembersOperation;
import org.jboss.as.console.client.administration.role.ui.AccessControlProviderDialog;
import org.jboss.as.console.client.administration.role.ui.AddRoleAssignmentWizard;
import org.jboss.as.console.client.administration.role.ui.AddScopedRoleWizard;
import org.jboss.as.console.client.administration.role.ui.MembersDialog;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Outcome;

/**
 * There are some constraints when managing role assignments in the console:
 * <ol>
 * <li>There has to be at least one inclusion for the role assignment</li>
 * <li>An exclusion can only contain users excluded from a group</li>
 * </ol>
 * <p>Role assignment which do not met these constraints, won't be visible in the conole and have to be
 * managed using other tools (e.g. the CLI)</p>
 *
 * @author Harald Pehl
 */
public class RoleAssignmentPresenter
        extends Presenter<RoleAssignmentPresenter.MyView, RoleAssignmentPresenter.MyProxy> {

    static final String SIMPLE_ACCESS_CONTROL_PROVIDER = "simple";
    private final boolean standalone;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final ManagementOperation<Map<Results, Object>> loadRoleAssignmentsOp;
    private boolean initialized;
    private DefaultWindow window;
    private Principals principals;
    private RoleAssignments assignments;
    private Roles roles;
    private List<String> hosts;
    private List<String> serverGroups;

    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public RoleAssignmentPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final RevealStrategy revealStrategy, final DispatchAsync dispatcher,
            final HostInformationStore hostInformationStore, ServerGroupStore serverGroupStore) {
        super(eventBus, view, proxy);

        this.standalone = Console.getBootstrapContext().isStandalone();
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.loadRoleAssignmentsOp = new LoadRoleAssignmentsOp(this, dispatcher, hostInformationStore,
                serverGroupStore);

        // empty defaults to prevent NPE before the first call to loadAssignments()
        this.principals = new Principals();
        this.assignments = new RoleAssignments();
        this.roles = new Roles();
        this.hosts = new ArrayList<String>();
        this.serverGroups = new ArrayList<String>();

        this.initialized = false;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInAdministration(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadAssignments();
    }

    private void loadAssignments() {
        if (!loadRoleAssignmentsOp.isPending()) {
            System.out.print("Loading role assignments...");
            final long start = System.currentTimeMillis();
            loadRoleAssignmentsOp.execute(new Outcome<Map<Results, Object>>() {
                @Override
                public void onFailure(final Map<Results, Object> context) {
                    System.out.println("FAILED");
                    Throwable caught = (Throwable) context.get(Results.ERROR);
                    if (caught != null) {
                        Log.error("Unknown error", caught);
                        Console.error("Unknown error", caught.getMessage());
                    }
                }

                @Override
                @SuppressWarnings("unchecked")
                public void onSuccess(final Map<Results, Object> context) {
                    long end = System.currentTimeMillis();
                    System.out.println("DONE in " + (end - start) + " ms");
                    principals = (Principals) context.get(Results.PRINCIPALS);
                    assignments = (RoleAssignments) context.get(Results.ASSIGNMENTS);
                    roles = (Roles) context.get(Results.ROLES);
                    hosts = (List<String>) context.get(Results.HOSTS);
                    serverGroups = (List<String>) context.get(Results.SERVER_GROUPS);
                    getView().update(principals, assignments, roles, hosts, serverGroups);

                    // show warning about simple access control provider (if not already done)
                    if (!initialized) {
                        String acp = (String) context.get(Results.ACCESS_CONTROL_PROVIDER);
                        if (SIMPLE_ACCESS_CONTROL_PROVIDER.equals(acp)) {
                            openWindow(Console.CONSTANTS.administration_access_control_provider_header(), 480, 200,
                                    new AccessControlProviderDialog(RoleAssignmentPresenter.this).asWidget());
                        }
                    }
                    initialized = true;
                }
            });
        }
    }

    // ------------------------------------------------------ callback methods triggered by the view

    public void launchAddRoleAssignmentWizard(final Principal.Type type) {
        String title = type == USER ? Console.CONSTANTS.role_assignment_add_user() : Console
                .CONSTANTS.role_assignment_add_group();
        openWindow(title, 480, 630, new AddRoleAssignmentWizard(this, type, principals, roles).asWidget());
    }

    public void addRoleAssignment(final RoleAssignment assignment) {
        closeWindow();
        ManagementOperation<Stack<Boolean>> mo = new ModifyRoleAssignmentOp(dispatcher, assignment, ADD);
        mo.execute(new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                Console.error(Console.MESSAGES.addingFailed("role assignment"));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                Console.info(Console.MESSAGES.added("role assignment"));
                loadAssignments();
            }
        });
    }

    public void saveRoleAssignment(final RoleAssignment assignment, final RoleAssignment oldValue) {
        ManagementOperation<Stack<Boolean>> mo = new ModifyRoleAssignmentOp(dispatcher, assignment, oldValue, MODIFY);
        mo.execute(new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                Console.error(Console.MESSAGES.saveFailed("role assignment"));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                Console.info(Console.MESSAGES.saved("role assignment"));
                loadAssignments();
            }
        });
    }

    public void removeRoleAssignment(final RoleAssignment assignment) {
        ManagementOperation<Stack<Boolean>> mo = new ModifyRoleAssignmentOp(dispatcher, assignment, REMOVE);
        mo.execute(new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                Console.error(Console.MESSAGES.deletionFailed("role assignment"));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                Console.info(Console.MESSAGES.deleted("role assignment"));
                loadAssignments();
            }
        });
    }

    public void launchAddScopedRoleWizard() {
        if (!assertDomainMode()) { return; }
        openWindow(Console.CONSTANTS.administration_add_scoped_role(), 480, 420,
                new AddScopedRoleWizard(hosts, serverGroups, this).asWidget());
    }

    public void addScopedRole(final Role role) {
        if (!assertDomainMode()) { return; }

        closeWindow();
        ManagementOperation<Stack<Boolean>> mo = new ModifyRoleOp(dispatcher, role, role, ADD);
        mo.execute(new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                Console.error(Console.MESSAGES.addingFailed(role.getName()));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                Console.info(Console.MESSAGES.added(role.getName()));
                loadAssignments();
            }
        });
    }

    public void saveScopedRole(final Role scopedRole, final Role oldValue) {
        if (!assertDomainMode()) { return; }

        ManagementOperation.Operation operation = (scopedRole.getName().equals(oldValue.getName())) ? MODIFY : RENAME;
        if (operation == RENAME) {
            int usage = usedInAssignments(oldValue);
            if (usage > 0) {
                Console.error(Console.MESSAGES
                        .saveFailed(oldValue.getName() + " as " + scopedRole.getName() + ". " + Console.MESSAGES
                                .administration_scoped_role_in_use(usage)));
                loadAssignments();
                return;
            }
        }

        ManagementOperation<Stack<Boolean>> mo = new ModifyRoleOp(dispatcher, scopedRole, oldValue, operation);
        mo.execute(new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                Console.error(Console.MESSAGES.saveFailed(scopedRole.getName()));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                Console.info(Console.MESSAGES.saved(scopedRole.getName()));
                loadAssignments();
            }
        });
    }

    public void modifyIncludeAll(final Role role) {
        ManagementOperation<Stack<Boolean>> mo = new ModifyRoleOp(dispatcher, role, role, MODIFY);
        mo.execute(new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                Console.error(Console.MESSAGES.saveFailed(role.getName()));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                Console.info(Console.MESSAGES.saved(role.getName()));
                loadAssignments();
            }
        });
    }

    public void removeScopedRole(final Role role) {
        if (!assertDomainMode()) { return; }

        int usage = usedInAssignments(role);
        if (usage > 0) {
            Console.error(Console.MESSAGES
                    .deletionFailed(role.getName() + ". " + Console.MESSAGES.administration_scoped_role_in_use(usage)));
            loadAssignments();
            return;
        }

        ManagementOperation<Stack<Boolean>> mo = new ModifyRoleOp(dispatcher, role, role, REMOVE);
        mo.execute(new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                Console.error(Console.MESSAGES.deletionFailed(role.getName()));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                Console.info(Console.MESSAGES.deleted(role.getName()));
                loadAssignments();
            }
        });
    }

    public void showMembers(final Role role) {
        if (role == null) {
            return;
        }

        ManagementOperation<RoleAssignment.Internal> mo = new ShowMembersOperation(dispatcher, role, principals);
        mo.execute(new Outcome<RoleAssignment.Internal>() {
            @Override
            public void onFailure(final RoleAssignment.Internal internal) {
                show(internal);
            }

            @Override
            public void onSuccess(final RoleAssignment.Internal internal) {
                show(internal);
            }

            private void show(final RoleAssignment.Internal internal) {
                openWindow(Console.MESSAGES.administration_members(role.getName()), 480, 420,
                        new MembersDialog(RoleAssignmentPresenter.this, internal).asWidget());
            }
        });
    }

    private boolean assertDomainMode() {
        if (standalone) {
            Log.error("Scoped roles are not supported in standalone mode!");
            return false;
        }
        return true;
    }

    private int usedInAssignments(final Role scopedRole) {
        int counter = 0;
        for (RoleAssignment assignment : assignments) {
            boolean found = false;
            for (Role role : assignment.getRoles()) {
                if (role.getName().equals(scopedRole.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (Role role : assignment.getExcludes()) {
                    if (role.getName().equals(scopedRole.getName())) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                counter++;
            }
        }
        return counter;
    }

    public void openWindow(final String title, final int width, final int height, final Widget content) {
        closeWindow();
        window = new DefaultWindow(title);
        window.setWidth(width);
        window.setHeight(height);
        window.trapWidget(content);
        window.setGlassEnabled(true);
        window.center();
    }

    public void closeWindow() {
        if (window != null) {
            window.hide();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ------------------------------------------------------ properties

    public boolean isStandalone() {
        return standalone;
    }

    // ------------------------------------------------------ proxy and view

    @ProxyCodeSplit
    @NameToken(NameTokens.RoleAssignmentPresenter)
    @AccessControl(resources = {"/core-service=management/access=authorization"})
    public interface MyProxy extends Proxy<RoleAssignmentPresenter>, Place {
    }

    public interface MyView extends View {

        void setPresenter(final RoleAssignmentPresenter presenter);

        void update(final Principals principals, final RoleAssignments assignments, final Roles roles,
                final List<String> hosts, final List<String> serverGroups);
    }
}
