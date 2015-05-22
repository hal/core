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

import com.google.common.collect.Collections2;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.AccessControlFinder;
import org.jboss.as.console.client.administration.accesscontrol.store.AccessControlStore;
import org.jboss.as.console.client.administration.accesscontrol.store.AddScopedRole;
import org.jboss.as.console.client.administration.accesscontrol.store.ModifyScopedRole;
import org.jboss.as.console.client.administration.accesscontrol.store.ModifyStandardRole;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.ListItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Harald Pehl
 */
public class ScopedRoleDialog implements IsWidget {

    static class HelpPanel extends StaticHelpPanel {

        final static Templates TEMPLATES = GWT.create(Templates.class);

        HelpPanel() {
            super(new SafeHtmlBuilder().append(TEMPLATES.help(
                    Console.CONSTANTS.administration_scoped_role_base_role_desc(),
                    Console.CONSTANTS.administration_scoped_role_scope_desc(),
                    Console.CONSTANTS.administration_role_include_all_desc())).toSafeHtml());
        }

        interface Templates extends SafeHtmlTemplates {

            @Template("<table style=\"vertical-align:top\" cellpadding=\"3\">" +
                    "<tr><td>Include All</td><td>{0}</td></tr>" +
                    "</table>")
            SafeHtml help(String includeAllDesc);

            @Template("<table style=\"vertical-align:top\" cellpadding=\"3\">" +
                    "<tr><td>Base Role</td><td>{0}</td></tr>" +
                    "<tr><td>Scope</td><td>{1}</td></tr>" +
                    "<tr><td>Include All</td><td>{2}</td></tr>" +
                    "</table>")
            SafeHtml help(String baseRoleDesc, String scopeDesc, String includeAllDesc);
        }
    }


    private final BeanFactory beanFactory;
    private final AccessControlStore accessControlStore;
    private final Dispatcher circuit;
    private final AccessControlFinder presenter;
    private final boolean scoped;
    private final Role existingRole;

    public ScopedRoleDialog(final BeanFactory beanFactory,
            final AccessControlStore accessControlStore,
            final Dispatcher circuit,
            final AccessControlFinder presenter,
            final boolean scoped) {
        this(beanFactory, accessControlStore, circuit, presenter, scoped, null);
    }

    public ScopedRoleDialog(final BeanFactory beanFactory,
            final AccessControlStore accessControlStore,
            final Dispatcher circuit,
            final AccessControlFinder presenter,
            final Role existingRole) {
        this(beanFactory, accessControlStore, circuit, presenter, existingRole.isScoped(), existingRole);
    }

    public ScopedRoleDialog(final BeanFactory beanFactory,
            final AccessControlStore accessControlStore,
            final Dispatcher circuit,
            final AccessControlFinder presenter,
            final boolean scoped,
            final Role existingRole) {
        this.beanFactory = beanFactory;
        this.accessControlStore = accessControlStore;
        this.circuit = circuit;
        this.presenter = presenter;
        this.scoped = scoped;
        this.existingRole = existingRole;
    }


    public Widget asWidget() {
        TextBoxItem name = new TextBoxItem("name", "Name", true);
        ComboBoxItem baseRole = new ComboBoxItem("baseRole", "Base Role");
        baseRole.setDefaultToFirstOption(true);
        Collection<String> roleNames = Collections2.transform(StandardRole.values(), StandardRole::getId);
        baseRole.setValueMap(roleNames);

        ComboBoxItem type = new ComboBoxItem("type", "Type");
        type.setDefaultToFirstOption(true);
        type.setValueMap(new String[]{"Host", "Server Group"});

        ListItem scope = new ListItem("scope", "Scope");
        scope.setRequired(true);
        CheckBoxItem includeAll = new CheckBoxItem("includeAll", "Include All");

        Form<RoleBean> form = new Form<>(RoleBean.class);
        if (scoped) {
            form.setFields(name, baseRole, type, scope, includeAll);
        } else {
            form.setFields(name, includeAll);
        }
        if (this.existingRole != null) {
            name.setEnabled(false);
            type.setEnabled(false);
            form.edit(modelToBean(existingRole));
        }

        form.addFormValidator((formItems, outcome) -> {
            if (existingRole == null && duplicateNameAndType(beanToModel(form.getUpdatedEntity()))) {
                outcome.addError("name");
                name.setErrMessage("Role already exists");
                name.setErroneous(true);
            }
        });

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        layout.add(new HelpPanel().asWidget());
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    FormValidation validation = form.validate();
                    if (!validation.hasErrors()) {
                        if (existingRole == null) {
                            circuit.dispatch(new AddScopedRole(beanToModel(form.getUpdatedEntity())));
                        } else if (existingRole.isScoped()) {
                            circuit.dispatch(new ModifyScopedRole(beanToModel(form.getUpdatedEntity())));
                        } else {
                            circuit.dispatch(new ModifyStandardRole(beanToModel(form.getUpdatedEntity())));
                        }
                        presenter.closeWindow();
                    }
                },
                event -> presenter.closeWindow()
        );

        return new WindowContentBuilder(layout, options).build();
    }

    private boolean duplicateNameAndType(Role scopedRole) {
        for (Role role : accessControlStore.getRoles().getScopedRoles()) {
            if (role.getType() == scopedRole.getType() && role.getName().equals(scopedRole.getName())) {
                return true;
            }
        }
        return false;
    }

    private Role beanToModel(RoleBean bean) {
        Role model;
        if (scoped) {
            Role.Type roleType = "Host".equals(bean.getType()) ? Role.Type.HOST : Role.Type.SERVER_GROUP;
            if (existingRole != null) {
                model = existingRole;
                model.setBaseRole(StandardRole.fromId(bean.getBaseRole()));
                model.setScope(bean.getScope());
            } else {
                model = new Role(bean.getName(), bean.getName(), StandardRole.fromId(bean.getBaseRole()), roleType,
                        bean.getScope());
            }
            model.setIncludeAll(bean.isIncludeAll());
        } else {
            // standard roles must always exist!
            model = existingRole;
            model.setIncludeAll(bean.isIncludeAll());
        }
        return model;
    }

    private RoleBean modelToBean(Role model) {
        RoleBean bean = beanFactory.role().as();
        bean.setName(model.getName());
        bean.setIncludeAll(model.isIncludeAll());
        if (scoped) {
            bean.setBaseRole(model.getBaseRole().getId());
            bean.setType(model.getType() == Role.Type.HOST ? "Host" : "Server Group");
            bean.setScope(new ArrayList<>(model.getScope()));
        }
        return bean;
    }
}
