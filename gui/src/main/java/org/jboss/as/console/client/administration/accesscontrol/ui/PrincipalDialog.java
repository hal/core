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

import com.google.common.base.Strings;
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
import org.jboss.as.console.client.administration.accesscontrol.store.AddPrincipal;
import org.jboss.as.console.client.administration.accesscontrol.store.Principal;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Harald Pehl
 */
public class PrincipalDialog implements IsWidget {

    static class HelpPanel extends StaticHelpPanel {

        final static Templates TEMPLATES = GWT.create(Templates.class);

        HelpPanel() {
            super(new SafeHtmlBuilder().append(
                    TEMPLATES.help(Console.CONSTANTS.administration_assignment_user_group_desc(),
                            Console.CONSTANTS.administration_assignment_realm_desc(),
                            Console.CONSTANTS.administration_assignment_roles_desc())).toSafeHtml());
        }

        interface Templates extends SafeHtmlTemplates {

            @Template("<table style=\"vertical-align:top\" cellpadding=\"3\">" +
                    "<tr><td>Name</td><td>{0}</td></tr>" +
                    "<tr><td>Realm</td><td>{1}</td></tr>" +
                    "<tr><td>Realm</td><td>{2}</td></tr>" +
                    "</table>")
            SafeHtml help(String nameDesc, String realmDesc, String roleDesc);
        }
    }


    private final Principal.Type type;
    private final AccessControlStore accessControlStore;
    private final Dispatcher circuit;
    private final AccessControlFinder presenter;
    private final SortedMap<String, Role> roles;


    public PrincipalDialog(final Principal.Type type,
            final AccessControlStore accessControlStore,
            final Dispatcher circuit,
            final AccessControlFinder presenter) {
        this.type = type;
        this.accessControlStore = accessControlStore;
        this.circuit = circuit;
        this.presenter = presenter;

        this.roles = new TreeMap<>();
        for (Role role : accessControlStore.getRoles()) {
            roles.put(role.getName(), role);
        }
    }

    public Widget asWidget() {
        TextBoxItem name = new TextBoxItem("name", "Name", true);
        TextBoxItem realm = new TextBoxItem("realm", "Realm", false);
        ComboBoxItem role = new ComboBoxItem("role", "Role");
        role.setValueMap(roles.keySet());
        role.setDefaultToFirstOption(true);

        Form<PrincipalBean> form = new Form<>(PrincipalBean.class);
        form.setFields(name, realm, role);
        form.addFormValidator((formItems, outcome) -> {
            if (accessControlStore.getPrincipals().contains(beanToModel(form.getUpdatedEntity()))) {
                outcome.addError("name");
                name.setErrMessage(type == Principal.Type.USER ? "User already exists" : "Group already exists");
                name.setErroneous(true);
            }
        });
        form.addFormValidator((formItems, outcome) -> {
            if (!roles.containsKey(role.getValue())) {
                outcome.addError("role");
                role.setErrMessage("Please select a role");
                role.setErroneous(true);
            }
        });

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        layout.add(new HelpPanel().asWidget());
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(event -> {
            FormValidation validation = form.validate();
            if (!validation.hasErrors()) {
                Principal principal = beanToModel(form.getUpdatedEntity());
                Role matchingRole = roles.get(role.getValue());
                circuit.dispatch(new AddPrincipal(principal, matchingRole));
                presenter.closeWindow();
            }
        }, event -> presenter.closeWindow());

        return new WindowContentBuilder(layout, options).build();
    }

    private Principal beanToModel(PrincipalBean bean) {
        return Principal.transientPrincipal(type, bean.getName(), Strings.emptyToNull(bean.getRealm()));
    }
}
