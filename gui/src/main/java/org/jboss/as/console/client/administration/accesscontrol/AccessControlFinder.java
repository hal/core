/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.administration.accesscontrol;

import com.google.common.collect.Sets;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.store.AccessControlStore;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignment;
import org.jboss.as.console.client.administration.accesscontrol.store.DuplicateResourceException;
import org.jboss.as.console.client.administration.accesscontrol.store.HasSuccessMessage;
import org.jboss.as.console.client.administration.accesscontrol.store.ModifiesAssignment;
import org.jboss.as.console.client.administration.accesscontrol.store.ModifiesAssignment.Relation;
import org.jboss.as.console.client.administration.accesscontrol.store.ModifiesPrincipal;
import org.jboss.as.console.client.administration.accesscontrol.store.ModifiesRole;
import org.jboss.as.console.client.administration.accesscontrol.store.Principal;
import org.jboss.as.console.client.administration.accesscontrol.store.ReloadAccessControl;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.administration.accesscontrol.store.RoleInUseException;
import org.jboss.as.console.client.administration.accesscontrol.ui.AssignmentDialog;
import org.jboss.as.console.client.administration.accesscontrol.ui.MemberDialog;
import org.jboss.as.console.client.administration.accesscontrol.ui.PrincipalDialog;
import org.jboss.as.console.client.administration.accesscontrol.ui.ScopedRoleDialog;
import org.jboss.as.console.client.administration.accesscontrol.ui.AccessControlProviderDialog;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.dag.ActionErrorSupport;

import java.util.Set;

import static org.jboss.as.console.client.administration.accesscontrol.store.ModifiesAssignment.Relation.PRINCIPAL_TO_ROLE;

/**
 * @author Harald Pehl
 */
public class AccessControlFinder extends PerspectivePresenter<AccessControlFinder.MyView, AccessControlFinder.MyProxy>
        implements Finder, PreviewEvent.Handler, FinderScrollEvent.Handler, ClearFinderSelectionEvent.Handler {

    // @formatter:off --------------------------------------- proxy & view

    @ProxyCodeSplit
    @NameToken(NameTokens.AccessControlFinder)
    @SearchIndex(keywords = {"authorization", "access-control", "rbac", "security"})
    @RequiredResources(resources = {"/core-service=management/access=authorization"}, recursive = false)
    public interface MyProxy extends ProxyPlace<AccessControlFinder> {}

    public interface MyView extends View, HasPresenter<AccessControlFinder> {
        void reload();
        void reloadPrincipals(Principal.Type type, Iterable<Principal> principals);
        void reloadRoles( Iterable<Role> roles);
        void reloadAssignments(Iterable<Assignment> assignments, Relation relation);
        void setPreview(SafeHtml html);
        void clearActiveSelection(ClearFinderSelectionEvent event);
        void toggleScrolling(boolean enforceScrolling, int requiredWidth);
    }


    // @formatter:on ---------------------------------------- instance data

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<>();

    private static final int WINDOW_WIDTH = 400;

    private final BeanFactory beanFactory;
    private final Dispatcher circuit;
    private final AccessControlStore accessControlStore;
    private final ActionErrorSupport errorSupport;

    private boolean initialized;
    private DefaultWindow window;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public AccessControlFinder(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final PlaceManager placeManager, final BeanFactory beanFactory, final Header header,
            final Dispatcher circuit, final AccessControlStore accessControlStore) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.AccessControlFinder, TYPE_MainContent);
        this.beanFactory = beanFactory;
        this.circuit = circuit;
        this.accessControlStore = accessControlStore;
        this.errorSupport = new ActionErrorSupport();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);

        // circuit action handling
        accessControlStore.addChangeHandler(action -> {
            if (action instanceof ReloadAccessControl) {
                getView().reload();
                if (!initialized && !accessControlStore.isRbacProvider()) {
                    openWindow("Access Control Provider", 480, 220,
                            new AccessControlProviderDialog(AccessControlFinder.this).asWidget());
                    initialized = true;
                }

            } else if (action instanceof ModifiesPrincipal) {
                Principal.Type type = ((ModifiesPrincipal) action).getPrincipal().getType();
                getView().reloadPrincipals(type, accessControlStore.getPrincipals().get(type));

            } else if (action instanceof ModifiesRole) {
                getView().reloadRoles(accessControlStore.getRoles());


            } else if (action instanceof ModifiesAssignment) {
                ModifiesAssignment maa = (ModifiesAssignment) action;
                Assignment assignment = maa.getAssignment();
                Iterable<Assignment> assignments = maa.getRelation() == PRINCIPAL_TO_ROLE ?
                        accessControlStore.getAssignments(assignment.getPrincipal(), assignment.isInclude()) :
                        accessControlStore.getAssignments(assignment.getRole(), assignment.isInclude());
                getView().reloadAssignments(assignments, maa.getRelation());
            }

            if (action instanceof HasSuccessMessage) {
                String message = ((HasSuccessMessage) action).getMessage();
                Console.info(message);
            }
        });

        // circuit error handling
        errorSupport.onError((action, error) -> {
            if (error instanceof DuplicateResourceException) {
                Console.error("Resource already exists",
                        "The resource with the name '" + ((DuplicateResourceException) error)
                                .getName() + "' already exists.");

            } else if (error instanceof RoleInUseException) {
                int usage = ((RoleInUseException) error).getUsage();
                Console.error("The role is used in assignments",
                        Console.MESSAGES.administration_scoped_role_in_use(usage));

            } else {
                Console.error("Unknown Error", error.getMessage());
            }
        });
        circuit.addDiagnostics(errorSupport);

        // GWT event handler
        registerHandler(getEventBus().addHandler(PreviewEvent.TYPE, this));
        registerHandler(getEventBus().addHandler(FinderScrollEvent.TYPE, this));
        registerHandler(getEventBus().addHandler(ClearFinderSelectionEvent.TYPE, this));
    }

    @Override
    protected void onUnbind() {
        super.onUnbind();
        circuit.removeDiagnostics(errorSupport);
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, final PlaceManager placeManager,
            final boolean revealDefault) {
        circuit.dispatch(new ReloadAccessControl());
    }

    @Override
    protected void onReset() {
        super.onReset();
        Console.MODULES.getHeader().highlight(getProxy().getNameToken());
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }


    // ------------------------------------------------------ finder related methods

    @Override
    public void onPreview(PreviewEvent event) {
        if (isVisible()) { getView().setPreview(event.getHtml()); }
    }

    @Override
    public void onToggleScrolling(final FinderScrollEvent event) {
        if (isVisible()) { getView().toggleScrolling(event.isEnforceScrolling(), event.getRequiredWidth()); }
    }

    @Override
    public void onClearActiveSelection(final ClearFinderSelectionEvent event) {
        if (isVisible()) { getView().clearActiveSelection(event); }
    }


    // ------------------------------------------------------ window management

    public void openWindow(final String title, final int width, final int height, final IsWidget content) {
        closeWindow();
        window = new DefaultWindow(title);
        window.setWidth(width);
        window.setHeight(height);
        window.trapWidget(content.asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void closeWindow() {
        if (window != null) {
            window.hide();
        }
    }


    // ------------------------------------------------------ rbac methods

    public void launchAddPrincipalDialog(final Principal.Type type) {
        PrincipalDialog dialog = new PrincipalDialog(type, accessControlStore, circuit, this);
        openWindow(type == Principal.Type.USER ? "Add User" : "Add Group", WINDOW_WIDTH, 250, dialog);
    }

    public void launchAddScopedRoleDialog() {
        ScopedRoleDialog dialog = new ScopedRoleDialog(beanFactory, accessControlStore, circuit, this, true);
        openWindow("Add Scoped Role", WINDOW_WIDTH, 400, dialog);
    }

    public void editRole(final Role role) {
        ScopedRoleDialog dialog = new ScopedRoleDialog(beanFactory, accessControlStore, circuit, this, role);
        openWindow("Edit Role", WINDOW_WIDTH, 400, dialog);
    }

    public void launchAddMemberDialog(final Role role, final boolean include) {
        Set<Principal> unassignedPrincipals = Sets.newHashSet(accessControlStore.getPrincipals());
        Iterable<Assignment> assignments = accessControlStore.getAssignments(role);
        for (Assignment assignment : assignments) {
            unassignedPrincipals.remove(assignment.getPrincipal());
        }
        if (unassignedPrincipals.isEmpty()) {
            Console.warning("All users and groups are already members of " + role.getName());
        } else {
            MemberDialog dialog = new MemberDialog(role, include, unassignedPrincipals, circuit, this);
            openWindow(include ? "Add Member" : "Exclude Member", WINDOW_WIDTH, 400, dialog);
        }
    }

    public void launchAddAssignmentDialog(final Principal principal, final boolean include) {
        Set<Role> unassignedRoles = Sets.newHashSet(accessControlStore.getRoles());
        Iterable<Assignment> assignments = accessControlStore.getAssignments(principal);
        for (Assignment assignment : assignments) {
            unassignedRoles.remove(assignment.getRole());
        }
        if (unassignedRoles.isEmpty()) {
            Console.warning("All roles are already assigned to " + principal.getNameAndRealm());
        } else {
            AssignmentDialog dialog = new AssignmentDialog(principal, include, unassignedRoles, circuit, this);
            openWindow(include ? "Assign Role" : "Exclude Role", WINDOW_WIDTH, 400, dialog);
        }
    }
}
