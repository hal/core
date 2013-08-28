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

import static java.util.Arrays.asList;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.view.client.AbstractDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.PrincipalType;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentTreeModel implements TreeViewModel {

    static final RoleAssignmentTemplate ROLE_ASSIGNMENT_TEMPLATE = GWT.create(RoleAssignmentTemplate.class);
    static final PrincipalTemplate PRINCIPAL_TEMPLATES = GWT.create(PrincipalTemplate.class);
    private final PrincipalType principalType;
    private final BeanFactory beanFactory;
    private final DispatchAsync dispatcher;
    private final DefaultNodeInfo<StandardRole> level0;
    private RoleAssignmentNodeInfo level1;
    private PrincipalNodeInfo level2;
    private final SingleSelectionModel<StandardRole> roleSelectionModel;
    private final SingleSelectionModel<RoleAssignment> roleAssignmentSelectionModel;
    private final SingleSelectionModel<Principal> principalSelectionModel;

    public RoleAssignmentTreeModel(final PrincipalType principalType, final BeanFactory beanFactory,
            final DispatchAsync dispatcher) {
        this.principalType = principalType;
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;

        this.roleSelectionModel = new SingleSelectionModel<StandardRole>(new ProvidesKey<StandardRole>() {
            @Override
            public Object getKey(final StandardRole item) {
                return item.name();
            }
        });
        this.roleAssignmentSelectionModel = new SingleSelectionModel<RoleAssignment>(new ProvidesKey<RoleAssignment>() {
            @Override
            public Object getKey(final RoleAssignment item) {
                return null; //item.getRole().getName();
            }
        });
        this.principalSelectionModel = new SingleSelectionModel<Principal>(new ProvidesKey<Principal>() {
            @Override
            public Object getKey(final Principal item) {
                return item.getName();
            }
        });

        ListDataProvider<StandardRole> dataProvider = new ListDataProvider<StandardRole>(asList(StandardRole.values()));
        AbstractCell<StandardRole> cell = new AbstractCell<StandardRole>() {
            @Override
            public void render(final Context context, final StandardRole value, final SafeHtmlBuilder sb) {
                sb.appendEscaped(value.name());
            }
        };
        this.level0 = new DefaultNodeInfo<StandardRole>(dataProvider, cell, roleSelectionModel, null);
    }

    public <T> NodeInfo<?> getNodeInfo(final T value) {
        if (value == null) {
            // Level 0: Roles
            return level0;
        } else if (value instanceof StandardRole) {
            // Level 1: Exclude / include assignments
            level1 = new RoleAssignmentNodeInfo(new RoleAssignmentDataProvider((StandardRole) value),
                    new RoleAssignmentCell(), roleAssignmentSelectionModel);
            return level1;
        } else if (value instanceof RoleAssignment) {
            // Level 2: Principals (group or users)
            List<Principal> filtered = filterPrincipals(((RoleAssignment) value).getExcludes());
            level2 = new PrincipalNodeInfo(new ListDataProvider<Principal>(filtered),
                    new PrincipalCell(), principalSelectionModel);
            return level2;
        }
        return null;
    }

    @Override
    public boolean isLeaf(final Object value) {
        return value instanceof Principal;
    }

    private List<Principal> filterPrincipals(List<Principal> principals) {
        List<Principal> filtered = new ArrayList<Principal>();
        for (Principal principal : principals) {
            if (principal.getType() == principalType) {
                filtered.add(principal);
            }
        }
        return filtered;
    }

    public SingleSelectionModel<StandardRole> getRoleSelectionModel() {
        return roleSelectionModel;
    }

    public SingleSelectionModel<RoleAssignment> getRoleAssignmentSelectionModel() {
        return roleAssignmentSelectionModel;
    }

    public SingleSelectionModel<Principal> getPrincipalSelectionModel() {
        return principalSelectionModel;
    }

    public void refreshRoleAssignments() {
        if (level1 != null) {
            level1.getDataProvider().refresh(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    if (level2 != null) {
                        level2.getDataProvider().refresh();
                    }
                }
            });
        }
    }

    interface RoleAssignmentTemplate extends SafeHtmlTemplates {

        @Template("<div>{0} <span>({1})</span></div>")
        SafeHtml roleAssignment(String name, int size);
    }

    interface PrincipalTemplate extends SafeHtmlTemplates {

        @Template("<div>{0}</div>")
        SafeHtml principal(String name);

        @Template("<div>{0}</div><div style=\"color:#666\">{1}</div>")
        SafeHtml principalWithRealm(String name, String realm);
    }

    static class PrincipalCell extends AbstractCell<Principal> {

        @Override
        public void render(final Context context, final Principal value, final SafeHtmlBuilder sb) {
            if (value.getRealm() != null) {
                sb.append(PRINCIPAL_TEMPLATES.principalWithRealm(value.getName(), value.getRealm()));
            } else {
                sb.append(PRINCIPAL_TEMPLATES.principal(value.getName()));
            }
        }
    }

    class RoleAssignmentNodeInfo extends DefaultNodeInfo<RoleAssignment> {

        private final RoleAssignmentDataProvider dataProvider;

        public RoleAssignmentNodeInfo(
                final RoleAssignmentDataProvider dataProvider,
                final RoleAssignmentCell cell,
                final SelectionModel<? super RoleAssignment> selectionModel) {
            super(dataProvider, cell, selectionModel, null);
            this.dataProvider = dataProvider;
        }

        RoleAssignmentDataProvider getDataProvider() {
            return dataProvider;
        }
    }

    class RoleAssignmentCell extends AbstractCell<RoleAssignment> {

        @Override
        public void render(final Context context, final RoleAssignment value, final SafeHtmlBuilder sb) {
            String name = /*value.isInclude() ?*/ "Includes" /*: "Excludes"*/;
            sb.append(ROLE_ASSIGNMENT_TEMPLATE.roleAssignment(name, filterPrincipals(value.getExcludes()).size()));
        }
    }

    class RoleAssignmentDataProvider extends AbstractDataProvider<RoleAssignment> {

        private final StandardRole selectedRole;

        public RoleAssignmentDataProvider(final StandardRole selectedRole) {
            this.selectedRole = selectedRole;
        }

        @Override
        protected void onRangeChanged(final HasData<RoleAssignment> display) {
            refresh(null);
        }

        public void refresh(final Scheduler.ScheduledCommand afterRefresh) {
            // TODO Same address in domain mode?
            ModelNode operation = new ModelNode();
            operation.get(ADDRESS).add("core-service", "management");
            operation.get(ADDRESS).add("access", "authorization");
            operation.get(ADDRESS).add("role-mapping", selectedRole.name());
            operation.get(OP).set(READ_RESOURCE_OPERATION);
            operation.get(RECURSIVE).set(true);

            final RoleAssignment excludes = beanFactory.roleAssignment().as();
//            excludes.setInclude(false);
            excludes.setExcludes(new ArrayList<Principal>());
            final RoleAssignment includes = beanFactory.roleAssignment().as();
//            includes.setInclude(true);
            includes.setExcludes(new ArrayList<Principal>());

            dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(DMRResponse result) {
                    ModelNode response = result.get();
                    if (response.isFailure()) {
                        Console.error(Console.MESSAGES.failed("Principals"),
                                response.getFailureDescription());
                    } else {
                        ModelNode payload = response.get(RESULT);
                        if (payload.hasDefined("exclude")) {
                            excludes.setExcludes(readPrincipals(payload.get("exclude").asList(), false));
                        }
                        if (payload.hasDefined("include")) {
                            includes.setExcludes(readPrincipals(payload.get("include").asList(), true));
                        }
                        updateRowCount(2, true);
                        updateRowData(0, asList(excludes, includes));
                        if (afterRefresh != null) {
                            Scheduler.get().scheduleDeferred(afterRefresh);
                        }
                    }
                }

                @Override
                public void onFailure(final Throwable caught) {
                    // assuming this is due to a missing role definition, we still need empty excludes and includes
                    updateRowCount(2, true);
                    updateRowData(0, asList(excludes, includes));
                    if (afterRefresh != null) {
                        Scheduler.get().scheduleDeferred(afterRefresh);
                    }
                }
            });
        }

        private List<Principal> readPrincipals(final List<ModelNode> nodes, final boolean include) {
            List<Principal> principals = new ArrayList<Principal>();
            for (ModelNode node : nodes) {
                ModelNode principalNode = node.get(0);
                Principal principal = beanFactory.principal().as();
                principal.setName(principalNode.get("name").asString());
                if (principalNode.get("realm").isDefined()) {
                    principal.setRealm(principalNode.get("realm").asString());
                }
                principal.setType(PrincipalType.valueOf(principalNode.get("type").asString()));
//                principal.setInclude(include);
                principals.add(principal);
            }
            return principals;
        }
    }

    class PrincipalNodeInfo extends DefaultNodeInfo<Principal> {

        private final ListDataProvider<Principal> dataProvider;

        PrincipalNodeInfo(
                final ListDataProvider<Principal> dataProvider,
                final PrincipalCell cell,
                final SelectionModel<? super Principal> selectionModel) {
            super(dataProvider, cell, selectionModel, null);
            this.dataProvider = dataProvider;
        }

        ListDataProvider<Principal> getDataProvider() {
            return dataProvider;
        }
    }
}