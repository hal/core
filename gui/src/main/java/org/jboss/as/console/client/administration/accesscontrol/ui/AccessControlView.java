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
package org.jboss.as.console.client.administration.accesscontrol.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.AccessControlFinder;
import org.jboss.as.console.client.administration.accesscontrol.store.AccessControlStore;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignment;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignments;
import org.jboss.as.console.client.administration.accesscontrol.store.ModifiesAssignment.Relation;
import org.jboss.as.console.client.administration.accesscontrol.store.Principal;
import org.jboss.as.console.client.administration.accesscontrol.store.Principals;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.administration.accesscontrol.store.Roles;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.v3.deployment.DomainDeploymentFinder;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Arrays;
import java.util.List;

/**
 * @author Harald Pehl
 */
public class AccessControlView extends SuspendableViewImpl implements AccessControlFinder.MyView {

    static final PreviewContent PREVIEW_CONTENT = PreviewContent.INSTANCE;

    private final Dispatcher circuit;
    private final AccessControlStore accessControlStore;
    private final BootstrapContext bootstrapContext;
    private final PreviewContentFactory contentFactory;

    private AccessControlFinder presenter;
    private LayoutPanel contentCanvas;
    private ColumnManager columnManager;

    private List<BrowseByItem> browseByItems;
    private BrowseByColumn browseByColumn;
    private Widget browseByColumnWidget;

    private PrincipalColumn userColumn;
    private Widget userColumnWidget;

    private PrincipalColumn groupColumn;
    private Widget groupColumnWidget;

    private RoleColumn roleColumn;
    private Widget roleColumnWidget;

    private AggregationColumn assignmentAggregationColumn;
    private Widget assignmentAggregationColumnWidget;
    private AssignmentColumn assignmentColumn;
    private Widget assignmentColumnWidget;

    private AggregationColumn memberAggregationColumn;
    private Widget memberAggregationColumnWidget;
    private MemberColumn memberColumn;
    private Widget memberColumnWidget;


    @Inject
    @SuppressWarnings("unchecked")
    public AccessControlView(final Dispatcher circuit,
            final AccessControlStore accessControlStore,
            final BootstrapContext bootstrapContext,
            final PreviewContentFactory contentFactory) {
        this.circuit = circuit;
        this.accessControlStore = accessControlStore;
        this.bootstrapContext = bootstrapContext;
        this.contentFactory = contentFactory;
    }

    @Override
    public void setPresenter(final AccessControlFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        contentCanvas = new LayoutPanel();
        SplitLayoutPanel layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.ACCESS_CONTROL);


        // create the columns from right to left,
        // since left columns depend on right columns
        // ------------------------------------------------------ members

        //noinspection Convert2MethodRef
        memberColumn = new MemberColumn(accessControlStore, circuit, presenter, columnManager,
                () -> roleColumn.getSelectedItem(), () -> memberAggregationColumn.getSelectedItem().isInclude());
        memberColumnWidget = memberColumn.asWidget();

        List<AggregationItem> memberAggregationItems = Arrays.asList(
                new AggregationItem(false,
                        () -> Assignments.orderedByPrincipal()
                                .immutableSortedCopy(
                                        accessControlStore.getAssignments(roleColumn.getSelectedItem(), false))),
                new AggregationItem(true,
                        () -> Assignments.orderedByPrincipal()
                                .immutableSortedCopy(
                                        accessControlStore.getAssignments(roleColumn.getSelectedItem(), true)))
        );
        memberAggregationColumn = new AggregationColumn("Membership", columnManager, memberColumn, memberColumnWidget);
        memberAggregationColumnWidget = memberAggregationColumn.asWidget();
        memberAggregationColumn.updateFrom(memberAggregationItems);


        // ------------------------------------------------------ assignments

        //noinspection Convert2MethodRef
        assignmentColumn = new AssignmentColumn(circuit, presenter, contentFactory, columnManager,
                () -> selectedPrincipal(), () -> assignmentAggregationColumn.getSelectedItem().isInclude());
        assignmentColumnWidget = assignmentColumn.asWidget();

        List<AggregationItem> assignmentAggregationItems = Arrays.asList(
                new AggregationItem(false,
                        () -> Assignments.orderedByRole()
                                .immutableSortedCopy(
                                        accessControlStore.getAssignments(selectedPrincipal(), false))),
                new AggregationItem(true,
                        () -> Assignments.orderedByRole()
                                .immutableSortedCopy(
                                        accessControlStore.getAssignments(selectedPrincipal(), true)))
        );
        assignmentAggregationColumn = new AggregationColumn("Assignment", columnManager, assignmentColumn,
                assignmentColumnWidget);
        assignmentAggregationColumnWidget = assignmentAggregationColumn.asWidget();
        assignmentAggregationColumn.updateFrom(assignmentAggregationItems);


        // ------------------------------------------------------ user, group and roles

        userColumn = new PrincipalColumn(Console.CONSTANTS.common_label_user(), Principal.Type.USER,
                accessControlStore, circuit, presenter, columnManager,
                () -> {
                    assignmentAggregationColumn.updateFrom(assignmentAggregationItems);
                    columnManager.appendColumn(assignmentAggregationColumnWidget);
                });
        userColumnWidget = userColumn.asWidget();

        groupColumn = new PrincipalColumn(Console.CONSTANTS.common_label_group(), Principal.Type.GROUP,
                accessControlStore, circuit, presenter, columnManager,
                () -> {
                    assignmentAggregationColumn.updateFrom(assignmentAggregationItems);
                    columnManager.appendColumn(assignmentAggregationColumnWidget);
                });
        groupColumnWidget = groupColumn.asWidget();

        roleColumn = new RoleColumn(bootstrapContext, accessControlStore, circuit, presenter, contentFactory,
                columnManager,
                () -> {
                    memberAggregationColumn.updateFrom(memberAggregationItems);
                    columnManager.appendColumn(memberAggregationColumnWidget);
                });
        roleColumnWidget = roleColumn.asWidget();


        // ------------------------------------------------------ browse by

        browseByItems = Arrays.asList(
                new BrowseByItem(Console.CONSTANTS.common_label_users(), PREVIEW_CONTENT.users(),
                        () -> {
                            columnManager.appendColumn(userColumnWidget);
                            List<Principal> principals = Principals.orderedByName()
                                    .immutableSortedCopy(accessControlStore.getPrincipals().get(Principal.Type.USER));
                            userColumn.updateFrom(principals);
                        }),
                new BrowseByItem(Console.CONSTANTS.common_label_groups(), PREVIEW_CONTENT.groups(),
                        () -> {
                            columnManager.appendColumn(groupColumnWidget);
                            List<Principal> principals = Principals.orderedByName()
                                    .immutableSortedCopy(accessControlStore.getPrincipals().get(Principal.Type.GROUP));
                            groupColumn.updateFrom(principals);
                        }),
                new BrowseByItem(Console.CONSTANTS.common_label_roles(), PREVIEW_CONTENT.roles(),
                        () -> {
                            columnManager.appendColumn(roleColumnWidget);
                            List<Role> roles = Roles.orderedByType().compound(Roles.orderedByName())
                                    .immutableSortedCopy(accessControlStore.getRoles());
                            roleColumn.updateFrom(roles);
                        })
        );

        browseByColumn = new BrowseByColumn(circuit, contentFactory, event -> {
            columnManager.reduceColumnsTo(1);
            if (browseByColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(asWidget());
                browseByColumn.getSelectedItem().onSelect().execute();
            } else {
                startupContent(contentFactory);
            }
        });
        browseByColumnWidget = browseByColumn.asWidget();
        browseByColumn.updateFrom(browseByItems);


        // ------------------------------------------------------ assemble UI

        // column #1
        columnManager.addWest(browseByColumnWidget);

        // column #2
        columnManager.addWest(userColumnWidget);
        columnManager.addWest(groupColumnWidget);
        columnManager.addWest(roleColumnWidget);

        // column #3
        columnManager.addWest(assignmentAggregationColumnWidget);
        columnManager.addWest(memberAggregationColumnWidget);

        // column #4
        columnManager.addWest(assignmentColumnWidget);
        columnManager.addWest(memberColumnWidget);

        // preview
        columnManager.add(contentCanvas);

        columnManager.setInitialVisible(1);
        return layout;
    }

    @Override
    public void reload() {
        browseByColumn.updateFrom(browseByItems);
        columnManager.reduceColumnsTo(1);
        setPreview(SafeHtmlUtils.EMPTY_SAFE_HTML);
    }

    @Override
    public void reloadPrincipals(final Principal.Type type, final Iterable<Principal> principals) {
        columnManager.reduceColumnsTo(2);
        if (type == Principal.Type.USER) {
            userColumn.updateFrom(Principals.orderedByName().immutableSortedCopy(principals));
        } else if (type == Principal.Type.GROUP) {
            groupColumn.updateFrom(Principals.orderedByName().immutableSortedCopy(principals));
        }
    }

    @Override
    public void reloadRoles(final Iterable<Role> roles) {
        columnManager.reduceColumnsTo(2);
        roleColumn.updateFrom(Roles.orderedByType().compound(Roles.orderedByName()).immutableSortedCopy(roles));
    }

    @Override
    public void reloadAssignments(final Iterable<Assignment> assignments, Relation relation) {
        columnManager.reduceColumnsTo(4);
        if (relation == Relation.PRINCIPAL_TO_ROLE) {
            assignmentColumn.updateFrom(Assignments.orderedByRole().immutableSortedCopy(assignments));
        } else if (relation == Relation.ROLE_TO_PRINCIPAL) {
            memberColumn.updateFrom(Assignments.orderedByPrincipal().immutableSortedCopy(assignments));
        }
    }

    // ------------------------------------------------------ slot management

    @Override
    public void setInSlot(final Object slot, final IsWidget content) {
        if (slot == DomainDeploymentFinder.TYPE_MainContent) {
            if (content != null) { setContent(content); } else { contentCanvas.clear(); }
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }


    // ------------------------------------------------------ finder related methods

    @Override
    public void setPreview(final SafeHtml html) {
        Scheduler.get().scheduleDeferred(() -> {
            contentCanvas.clear();
            contentCanvas.add(new HTML(html));
        });
    }

    @Override
    public void toggleScrolling(final boolean enforceScrolling, final int requiredWidth) {
        columnManager.toogleScrolling(enforceScrolling, requiredWidth);
    }

    public void clearActiveSelection(final ClearFinderSelectionEvent event) {
        browseByColumnWidget.getElement().removeClassName("active");
        userColumnWidget.getElement().removeClassName("active");
        groupColumnWidget.getElement().removeClassName("active");
        roleColumnWidget.getElement().removeClassName("active");
        assignmentAggregationColumnWidget.getElement().removeClassName("active");
        memberAggregationColumnWidget.getElement().removeClassName("active");
        assignmentColumnWidget.getElement().removeClassName("active");
        memberColumnWidget.getElement().removeClassName("active");
    }

    private void startupContent(PreviewContentFactory contentFactory) {
        contentFactory.createContent(PreviewContent.INSTANCE.access_control_empty(),
                new SimpleCallback<SafeHtml>() {
                    @Override
                    public void onSuccess(SafeHtml previewContent) {
                        setPreview(previewContent);
                    }
                }
        );
    }



    // ------------------------------------------------------ helper methods

    private Principal selectedPrincipal() {
        if (browseByColumn.getSelectedItem().getTitle().equals(Console.CONSTANTS.common_label_users())) {
            return userColumn.getSelectedItem();
        } else if (browseByColumn.getSelectedItem().getTitle().equals(Console.CONSTANTS.common_label_groups())) {
            return groupColumn.getSelectedItem();
        }
        return null;
    }
}
