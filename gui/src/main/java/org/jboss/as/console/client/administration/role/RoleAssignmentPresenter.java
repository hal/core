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

import static org.jboss.as.console.client.administration.role.LoadRoleAssignmentsOp.Results;
import static org.jboss.as.console.client.administration.role.ManagementOperation.Operation.*;
import static org.jboss.as.console.client.administration.role.model.Principal.Type.USER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.allen_sauer.gwt.log.client.Log;
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
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.RoleAssignments;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.administration.role.model.ScopedRole;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.rbac.Role;
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

    private final boolean standalone;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final ManagementOperation<Map<Results, Object>> loadRoleAssignmentsOp;
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
        this.loadRoleAssignmentsOp = new LoadRoleAssignmentsOp(dispatcher, hostInformationStore, serverGroupStore,
                standalone);

        // empty defaults to prevent NPE before the first call to loadAssignments()
        this.principals = new Principals();
        this.assignments = new RoleAssignments();
        this.roles = new Roles();
        this.hosts = new ArrayList<String>();
        this.serverGroups = new ArrayList<String>();
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
            loadRoleAssignmentsOp.extecute(new Outcome<Map<Results, Object>>() {
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
                }
            });
        }
    }

    // ------------------------------------------------------ callback methods triggered by the view

    public void launchAddRoleAssignmentWizard(final Principal.Type type) {
        closeWindow();
        String title = type == USER ? Console.CONSTANTS.role_assignment_add_user() : Console
                .CONSTANTS.role_assignment_add_group();
        window = new DefaultWindow(title);
        window.setWidth(480);
        window.setHeight(580);
        AddRoleAssignmentWizard wizard = new AddRoleAssignmentWizard(this, type, principals, roles);
        window.trapWidget(wizard.asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void addRoleAssignment(final RoleAssignment assignment) {
        closeWindow();
        ManagementOperation<Stack<Boolean>> mo = new ModifyRoleAssignmentOp(dispatcher, assignment, ADD);
        mo.extecute(new Outcome<Stack<Boolean>>() {
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

    public void saveRoleAssignment(final RoleAssignment assignment, final Set<Role> removedRoles,
            final Set<Role> removedExcludes) {
        ManagementOperation<Stack<Boolean>> mo = new ModifyRoleAssignmentOp(dispatcher, assignment, MODIFY,
                removedRoles, removedExcludes);
        mo.extecute(new Outcome<Stack<Boolean>>() {
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
        mo.extecute(new Outcome<Stack<Boolean>>() {
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

        closeWindow();
        window = new DefaultWindow(Console.CONSTANTS.administration_add_scoped_role());
        window.setWidth(480);
        window.setHeight(400);
        AddScopedRoleWizard wizard = new AddScopedRoleWizard(hosts, serverGroups, this);
        window.trapWidget(wizard.asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void addScopedRole(final ScopedRole role) {
        if (!assertDomainMode()) { return; }

        closeWindow();
        ManagementOperation<Object> mo = new ModifyScopedRoleOp(dispatcher, role, role, ADD);
        mo.extecute(new Outcome<Object>() {
            @Override
            public void onFailure(final Object context) {
                Console.error(Console.MESSAGES.addingFailed(role.getName()));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Object context) {
                Console.info(Console.MESSAGES.added(role.getName()));
                loadAssignments();
            }
        });
    }

    public void saveScopedRole(final ScopedRole scopedRole, final ScopedRole oldValue) {
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

        ManagementOperation<Object> mo = new ModifyScopedRoleOp(dispatcher, scopedRole, oldValue, operation);
        mo.extecute(new Outcome<Object>() {
            @Override
            public void onFailure(final Object context) {
                Console.error(Console.MESSAGES.saveFailed(scopedRole.getName()));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Object context) {
                Console.info(Console.MESSAGES.saved(scopedRole.getName()));
                loadAssignments();
            }
        });
    }

    public void removeScopedRole(final ScopedRole role) {
        if (!assertDomainMode()) { return; }

        int usage = usedInAssignments(role);
        if (usage > 0) {
            Console.error(Console.MESSAGES
                    .deletionFailed(role.getName() + ". " + Console.MESSAGES.administration_scoped_role_in_use(usage)));
            loadAssignments();
            return;
        }

        ManagementOperation<Object> mo = new ModifyScopedRoleOp(dispatcher, role, role, REMOVE);
        mo.extecute(new Outcome<Object>() {
            @Override
            public void onFailure(final Object context) {
                Console.error(Console.MESSAGES.deletionFailed(role.getName()));
                loadAssignments();
            }

            @Override
            public void onSuccess(final Object context) {
                Console.info(Console.MESSAGES.deleted(role.getName()));
                loadAssignments();
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

    private int usedInAssignments(final ScopedRole scopedRole) {
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

    public void closeWindow() {
        if (window != null) {
            window.hide();
        }
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
