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
package org.jboss.as.console.client.rbac.internal;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.rbac.RolesHelpPanel;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.Preferences;
import org.jboss.as.console.client.tools.Tool;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.ListBoxItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jboss.as.console.client.shared.Preferences.Key.RUN_AS_ROLE;

/**
 * @author Harald Pehl
 */
public class RunAsRoleTool implements Tool {

    private DefaultWindow window;
    private ListBoxItem role;

    public RunAsRoleTool() {
        role = new ListBoxItem("role", "Role");
    }

    @Override
    public void launch() {
        if (window == null) {
            setupWindow();
        }

        // preselection


        window.center();
    }

    @Override
    public void dispose() {
        window.hide();
    }

    private void setupWindow() {
        initRoles(Collections.<String>emptySet(), Collections.<String>emptySet());

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("window-content");
        panel.add(new RolesHelpPanel().asWidget());
        panel.add(new ContentHeaderLabel("Select Role"));
        panel.add(new ContentDescription("Select the role you want to act on their behalf."));

        final Form<Object> form = new Form<Object>(Object.class);
        form.setFields(role);
        panel.add(form.asWidget());

        ClickHandler runAsHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                final FormValidation validation = form.validate();
                if (!validation.hasErrors()) {
                    runAs(role.getValue());
                }
            }
        };
        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                window.hide();
            }
        };
        DialogueOptions options = new DialogueOptions(
                "Run As", runAsHandler, "Cancel", cancelHandler);

        window = new DefaultWindow("Run as Role");
        window.setWidth(480);
        window.setHeight(300);
        window.trapWidget(new WindowContentBuilder(panel, options).build());
        window.setModal(true);
        window.setGlassEnabled(true);
    }

    private void initRoles(Set<String> serverGroupScoped, Set<String> hostScoped) {
        List<String> roleNames = new ArrayList<String>();
        roleNames.add("No preselection");
        for (StandardRole standardRole : StandardRole.values()) {
            roleNames.add(standardRole.getId());
        }
        roleNames.addAll(serverGroupScoped);
        roleNames.addAll(hostScoped);
        role.setChoices(roleNames, "No preselection");
    }

    private void runAs(final String role) {
        window.hide();

        String oldRole = Console.MODULES.getBootstrapContext().getRunAs();
        if ((oldRole == null && role.equals("No preselection")) || role.equalsIgnoreCase(oldRole)) {
            return;
        }

        if (role.length() != 0 && !role.equals("No preselection")) {
            Preferences.set(RUN_AS_ROLE, role); // temporary, see console bootstrap : clears the pref again
        }

        Feedback.confirm(Console.MESSAGES.restartRequired(), Console.MESSAGES.restartRequiredConfirm(),
                new Feedback.ConfirmationHandler() {
                    @Override
                    public void onConfirmation(boolean isConfirmed) {
                        if (isConfirmed) {
                            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    Window.Location.reload();
                                }
                            });
                        }
                    }
                });
    }

    public void setScopedRoles(final Set<String> serverGroupScoped, final Set<String> hostScoped) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                initRoles(serverGroupScoped, hostScoped);

                String savedRole = Preferences.get(RUN_AS_ROLE);
                if (savedRole != null) {
                    role.setValue(savedRole);
                }
            }
        });
    }
}
