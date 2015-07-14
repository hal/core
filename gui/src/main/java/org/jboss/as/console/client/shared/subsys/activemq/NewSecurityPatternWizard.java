/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.shared.subsys.activemq;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqSecurityPattern;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 5/10/11
 */
public class NewSecurityPatternWizard {

    private MsgDestinationsPresenter presenter;

    public NewSecurityPatternWizard(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        final Form<ActivemqSecurityPattern> form = new Form<>(ActivemqSecurityPattern.class);
        form.setNumColumns(1);

        TextBoxItem pattern = new TextBoxItem("pattern", "Pattern");
        TextBoxItem principal = new TextBoxItem("role", "Role");
        CheckBoxItem send = new CheckBoxItem("send", "Send?");
        CheckBoxItem consume = new CheckBoxItem("consume", "Consume?");
        CheckBoxItem manage = new CheckBoxItem("manage", "Manage?");

        form.setFields(pattern, principal, send, consume, manage);

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "*");
            address.add("security-setting", "*");
            address.add("role", "*");
            return address;
        }, form);

        layout.add(helpPanel.asWidget());

        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                //submit
                event -> {
                    FormValidation validation = form.validate();
                    if (!validation.hasErrors()) {
                        presenter.onCreateSecPattern(form.getUpdatedEntity());
                    }
                },
                // cancel
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
