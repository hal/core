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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.CustomProvider;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.*;
import org.jboss.as.console.client.administration.role.operation.*;
import org.jboss.as.console.client.administration.role.ui.AccessControlProviderDialog;
import org.jboss.as.console.client.administration.role.ui.AddRoleAssignmentWizard;
import org.jboss.as.console.client.administration.role.ui.AddScopedRoleWizard;
import org.jboss.as.console.client.administration.role.ui.MembersDialog;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.RequiredResourcesProvider;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Outcome;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.console.client.administration.role.model.Principal.Type.USER;
import static org.jboss.as.console.client.administration.role.operation.ManagementOperation.Operation.*;

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

    @ProxyCodeSplit
    @NameToken(NameTokens.RoleAssignmentPresenter)
    @CustomProvider(RequiredResourcesProvider.class)
    @SearchIndex(keywords = {"authorization", "access-control", "rbac", "security"})
    @RequiredResources(resources = {"/core-service=management/access=authorization"}, recursive = false)
    public interface MyProxy extends ProxyPlace<RoleAssignmentPresenter> {}


    public interface MyView extends View {
        void setPresenter(final RoleAssignmentPresenter presenter);
        void update(final Principals principals, final RoleAssignments assignments, final Roles roles,
                    final List<String> hosts, final List<String> serverGroups);
    }


    class FunctionOutcome implements Outcome<FunctionContext> {

        private final String successMessage;
        private final String failureMessage;

        public FunctionOutcome(final String entity, final ManagementOperation.Operation op) {
            switch (op) {
                case ADD:
                    successMessage = Console.MESSAGES.added(entity);
                    failureMessage = Console.MESSAGES.addingFailed(entity);
                    break;
                case MODIFY:
                    successMessage = Console.MESSAGES.saved(entity);
                    failureMessage = Console.MESSAGES.saveFailed(entity);
                    break;
                case REMOVE:
                    successMessage = Console.MESSAGES.deleted(entity);
                    failureMessage = Console.MESSAGES.deletionFailed(entity);
                    break;
                default:
                    successMessage = Console.CONSTANTS.common_label_success();
                    failureMessage = Console.CONSTANTS.common_error_failure();
            }
        }

        @Override
        public void onSuccess(final FunctionContext context) {
            Console.info(successMessage);
            loadAssignments();
        }

        public void onFailure(final FunctionContext context) {
            if (context.isForbidden()) {
                Console.error(failureMessage, Console.CONSTANTS.forbidden_desc());
            } else {
                //noinspection ThrowableResultOfMethodCallIgnored
                String details = context.getError() != null ? context.getError().getMessage() : null;
                Console.error(failureMessage, details);
            }
            loadAssignments();
        }
    }


    static final String SIMPLE_ACCESS_CONTROL_PROVIDER = "simple";

    private final boolean standalone;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final ManagementOperation<FunctionContext> loadRoleAssignmentsOp;
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
            loadRoleAssignmentsOp.execute(new Outcome<FunctionContext>() {
                @Override
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                public void onFailure(final FunctionContext context) {
                    System.out.println("FAILED");
                    String details = null;
                    String message = Console.CONSTANTS.common_error_unknownError();
                    if (context.getError() != null) {
                        details = context.getError().getMessage();
                        Log.error(details, context.getError());
                        if (context.isForbidden()) {
                            message = Console.CONSTANTS.unauthorized();
                            details = Console.CONSTANTS.forbidden_desc();
                        }
                    }
                    Console.error(message, details);
                }

                @Override
                @SuppressWarnings("unchecked")
                public void onSuccess(final FunctionContext context) {
                    long end = System.currentTimeMillis();
                    System.out.println("DONE in " + (end - start) + " ms");
                    principals = context.get(LoadRoleAssignmentsOp.PRINCIPALS);
                    assignments = context.get(LoadRoleAssignmentsOp.ASSIGNMENTS);
                    roles = context.get(LoadRoleAssignmentsOp.ROLES);
                    hosts = context.get(LoadRoleAssignmentsOp.HOSTS);
                    serverGroups = context.get(LoadRoleAssignmentsOp.SERVER_GROUPS);
                    getView().update(principals, assignments, roles, hosts, serverGroups);

                    // show warning about simple access control provider (if not already done)
                    if (!initialized) {
                        String acp = context.get(LoadRoleAssignmentsOp.ACCESS_CONTROL_PROVIDER);
                        if (SIMPLE_ACCESS_CONTROL_PROVIDER.equals(acp)) {
                            openWindow("Access Control Provider", 480, 220,
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
        String title = type == USER ? Console.CONSTANTS.administration_add_user_assignment() : Console
                .CONSTANTS.administration_add_group_assignment();
        openWindow(title, 480, 550, new AddRoleAssignmentWizard(this, type, principals, roles).asWidget());
    }

    public void addRoleAssignment(final RoleAssignment assignment) {
        closeWindow();
        ManagementOperation<FunctionContext> mo = new ModifyRoleAssignmentOp(dispatcher, assignment, ADD);
        mo.execute(new FunctionOutcome("Role Assignment", ADD));
    }

    public void saveRoleAssignment(final RoleAssignment assignment, final RoleAssignment oldValue) {
        ManagementOperation<FunctionContext> mo = new ModifyRoleAssignmentOp(dispatcher, assignment, oldValue, MODIFY);
        mo.execute(new FunctionOutcome("Role Assignment", MODIFY));
    }

    public void removeRoleAssignment(final RoleAssignment assignment) {
        ManagementOperation<FunctionContext> mo = new ModifyRoleAssignmentOp(dispatcher, assignment, REMOVE);
        mo.execute(new FunctionOutcome("Role Assignment", REMOVE));
    }

    public void launchAddScopedRoleWizard() {
        if (!assertDomainMode()) { return; }
        openWindow(Console.CONSTANTS.administration_add_scoped_role(), 480, 420,
                new AddScopedRoleWizard(hosts, serverGroups, this).asWidget());
    }

    public void addScopedRole(final Role role) {
        if (!assertDomainMode()) { return; }

        closeWindow();
        ManagementOperation<FunctionContext> mo = new ModifyRoleOp(dispatcher, role, ADD);
        mo.execute(new FunctionOutcome(role.getName(), ADD));
    }

    public void saveScopedRole(final Role role) {
        if (!assertDomainMode()) { return; }

        ManagementOperation<FunctionContext> mo = new ModifyRoleOp(dispatcher, role, MODIFY);
        mo.execute(new FunctionOutcome(role.getName(), MODIFY));
    }

    public void modifyIncludeAll(final Role role) {
        ManagementOperation<FunctionContext> mo = new ModifyRoleOp(dispatcher, role, MODIFY);
        mo.execute(new FunctionOutcome(role.getName(), MODIFY));
    }

    public void removeScopedRole(final Role role) {
        if (!assertDomainMode()) { return; }

        int usage = usedInAssignments(role);
        if (usage > 0) {
            String errorMessage = Console.MESSAGES.deletionFailed(role.getName()) + ": " +
                    Console.MESSAGES.administration_scoped_role_in_use(usage);
            Console.error(errorMessage);
            loadAssignments();
            return;
        }

        ManagementOperation<FunctionContext> mo = new ModifyRoleOp(dispatcher, role, REMOVE);
        mo.execute(new FunctionOutcome(role.getName(), REMOVE));
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


    // ------------------------------------------------------ properties

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isStandalone() {
        return standalone;
    }
}
