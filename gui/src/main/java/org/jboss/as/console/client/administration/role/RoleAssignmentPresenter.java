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
import static org.jboss.as.console.client.administration.role.model.PrincipalType.USER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.jboss.as.console.client.administration.role.model.PrincipalType;
import org.jboss.as.console.client.administration.role.model.Principals;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.RoleAssignments;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.administration.role.model.ScopedRole;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.rbac.Role;
import org.jboss.as.console.client.shared.BeanFactory;
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

    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final BeanFactory beanFactory;
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
            final RevealStrategy revealStrategy, final DispatchAsync dispatcher, final BeanFactory beanFactory,
            final HostInformationStore hostInformationStore, ServerGroupStore serverGroupStore) {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.beanFactory = beanFactory;
        this.loadRoleAssignmentsOp = new LoadRoleAssignmentsOp(dispatcher, beanFactory, hostInformationStore,
                serverGroupStore);

        this.principals = new Principals();
        this.assignments = new RoleAssignments(beanFactory);
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

    public void launchAddRoleAssignmentWizard(final PrincipalType type) {
        closeWindow();
        String title = type == USER ? Console.CONSTANTS.role_assignment_add_user() : Console
                .CONSTANTS.role_assignment_add_group();
        window = new DefaultWindow(title);
        window.setWidth(480);
        window.setHeight(580);
        AddRoleAssignmentWizard wizard = new AddRoleAssignmentWizard(type, principals, roles, this, beanFactory);
        window.trapWidget(wizard.asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void addRoleAssignment(final RoleAssignment assignment) {
        closeWindow();
        ManagementOperation<Stack<Boolean>> op = new ModifyRoleAssignmentOp(dispatcher, assignment,
                Collections.<Role>emptyList());
        op.extecute(new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                Console.error(Console.MESSAGES.addingFailed("role assignment"));
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                Console.info(Console.MESSAGES.added("role rssignment"));
                loadAssignments();
            }
        });
    }

    public void saveRoleAssignment(final RoleAssignment assignment, final Collection<Role> removedRoles) {
        ManagementOperation<Stack<Boolean>> op = new ModifyRoleAssignmentOp(dispatcher, assignment, removedRoles);
        op.extecute(new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                Console.error(Console.MESSAGES.saveFailed("role assignment"));
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                Console.info(Console.MESSAGES.saved("role rssignment"));
                loadAssignments();
            }
        });
    }

    public void removeRoleAssignment(final RoleAssignment assignment) {
        ManagementOperation<Object> op = new RemoveRoleAssignmentOp(dispatcher, assignment);
        op.extecute(new Outcome<Object>() {
            @Override
            public void onFailure(final Object context) {
                Console.error(Console.MESSAGES.deletionFailed("role assignment"));
            }

            @Override
            public void onSuccess(final Object context) {
                Console.info(Console.MESSAGES.deleted("role assignment"));
                loadAssignments();
            }
        });
    }

    public void launchAddScopedRoleWizard() {
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
        Console.info("Not yet implemented");
        System.out.println(
                "Add scoped role " + role.getName() + " based on " + role.getBaseRole() + " scoped to " + role
                        .getType() + " " + role.getScope());
    }

    public void saveScopedRole(final ScopedRole role, final Map<String, Object> changedValues) {
        Console.info("Not yet implemented");
        System.out.println(
                "Save scoped role " + role.getName() + " based on " + role.getBaseRole() + " scoped to " + role
                        .getType() + " " + role.getScope());
        System.out.println("Changed values: " + changedValues);
    }

    public void removeScopedRole(final ScopedRole role) {
        Console.info("Not yet implemented");
        System.out.println(
                "Remove scoped role " + role.getName() + " based on " + role.getBaseRole() + " scoped to " + role
                        .getType() + " " + role.getScope());
    }

    public void closeWindow() {
        if (window != null) {
            window.hide();
        }
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
