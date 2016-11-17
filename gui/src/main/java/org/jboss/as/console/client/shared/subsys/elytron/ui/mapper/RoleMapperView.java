/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.shared.subsys.elytron.ui.mapper;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.ui.ElytronGenericResourceView;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class RoleMapperView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private ElytronGenericResourceView addPrefixRoleMapperEditor;
    private ElytronGenericResourceView addSuffixRoleMapperEditor;
    private ElytronGenericResourceView aggregateRoleMapperEditor;
    private ElytronGenericResourceView constantRoleMapperEditor;
    private ElytronGenericResourceView customRoleMapperEditor;
    private ElytronGenericResourceView logicalRoleMapperEditor;

    public RoleMapperView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }

    public Widget asWidget() {

        addPrefixRoleMapperEditor = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("add-prefix-role-mapper"), securityContext,
                "Add Prefix Role Mapper",
                ElytronStore.ADD_PREFIX_ROLE_MAPPER_ADDRESS);

        addSuffixRoleMapperEditor = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("add-suffix-role-mapper"),
                securityContext, "Add Suffix Role Mapper", ElytronStore.ADD_SUFFIX_ROLE_MAPPER_ADDRESS);

        aggregateRoleMapperEditor = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("aggregate-role-mapper"), securityContext,
                "Aggregate Role Mapper", ElytronStore.AGGREGATE_ROLE_MAPPER_ADDRESS);

        constantRoleMapperEditor = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("constant-role-mapper"), securityContext, "Constant Role Mapper",
                ElytronStore.CONSTANT_ROLE_MAPPER_ADDRESS);

        customRoleMapperEditor = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("custom-role-mapper"),
                securityContext, "Custom Role Mapper", ElytronStore.CUSTOM_ROLE_MAPPER_ADDRESS);

        logicalRoleMapperEditor = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("logical-role-mapper"), securityContext, "Logical Role Mapper",
                ElytronStore.LOGICAL_ROLE_MAPPER_ADDRESS);

        PagedView panel = new PagedView(true);
        panel.addPage("Add Prefix Role Mapper", addPrefixRoleMapperEditor.asWidget());
        panel.addPage("Add Suffix Role Mapper", addSuffixRoleMapperEditor.asWidget());
        panel.addPage("Aggregate Role Mapper", aggregateRoleMapperEditor.asWidget());
        panel.addPage("Constant Role Mapper", constantRoleMapperEditor.asWidget());
        panel.addPage("Custom Role Mapper", customRoleMapperEditor.asWidget());
        panel.addPage("Logical Role Mapper", logicalRoleMapperEditor.asWidget());
        // default page
        panel.showPage(0);

        return panel.asWidget();
    }

    public void updateLogicalRoleMapper(List<Property> models) {
        this.logicalRoleMapperEditor.update(models);
    }

    public void updateCustomRoleMapper(List<Property> models) {
        this.customRoleMapperEditor.update(models);
    }

    public void updateConstantRoleMapper(List<Property> models) {
        this.constantRoleMapperEditor.update(models);
    }

    public void updateAggregateRoleMapper(List<Property> models) {
        this.aggregateRoleMapperEditor.update(models);
    }

    public void updateAddSuffixRoleMapper(List<Property> models) {
        this.addSuffixRoleMapperEditor.update(models);
    }

    public void updateAddPrefixRoleMapper(List<Property> models) {
        this.addPrefixRoleMapperEditor.update(models);
    }
}
