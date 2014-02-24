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
package org.jboss.as.console.client.shared.patching.wizard;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
* @author Harald Pehl
*/
abstract class WizardStep implements IsWidget {

    final ApplyPatchWizard wizard;
    final String title;
    final String submitText;
    final String cancelText;

    private Widget widget;
    private DialogueOptions dialogOptions;

    WizardStep(final ApplyPatchWizard wizard, final String title) {
        this(wizard, title, Console.CONSTANTS.common_label_next(), Console.CONSTANTS.common_label_cancel());
    }

    WizardStep(final ApplyPatchWizard wizard, final String title, String submitText) {
        this(wizard, title, submitText, Console.CONSTANTS.common_label_cancel());
    }

    WizardStep(final ApplyPatchWizard wizard, final String title, String submitText, String cancelText) {
        this.wizard = wizard;
        this.title = title;
        this.submitText = submitText;
        this.cancelText = cancelText;
    }

    @Override
    public final Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        layout.addStyleName("apply-patch-wizard");

        layout.add(header());
        layout.add(body());

        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNext();
            }
        };
        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onCancel();
            }
        };
        dialogOptions = new DialogueOptions(submitText, submitHandler, cancelText, cancelHandler);

        widget = new WindowContentBuilder(layout, dialogOptions).build();
        return widget;
    }

    void onShow(final WizardContext context) {}

    void onNext() {
        wizard.next();
    }

    void onCancel() {
        wizard.close();
    }

    void setEnabled(boolean submitEnabled, boolean cancelEnabled) {
        DOM.setElementPropertyBoolean((Element) dialogOptions.getSubmit(), "disabled", !submitEnabled);
        DOM.setElementPropertyBoolean((Element) dialogOptions.getCancel(), "disabled", !cancelEnabled);
    }

    IsWidget header() {
        return new HTML("<h3>" + title + "</h3>");
    }

    abstract IsWidget body();
}
