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

import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.PatchManagerElementId;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @param <C> The context
 * @param <S> The state enum
 *
 * @author Harald Pehl
 */
public abstract class PatchWizardStep<C, S extends Enum<S>> implements IsWidget, PatchManagerElementId {

    protected final PatchWizard<C, S> wizard;
    protected String title;
    private final WizardButton submitButton;
    private final WizardButton cancelButton;
    private DialogueOptions dialogOptions;

    protected PatchWizardStep(final PatchWizard<C, S> wizard, final String title) {
        this(wizard, title, new WizardButton(Console.CONSTANTS.common_label_next()),
                new WizardButton(Console.CONSTANTS.common_label_cancel()));
    }

    protected PatchWizardStep(final PatchWizard<C, S> wizard, final String title, String submitText) {
        this(wizard, title, new WizardButton(submitText), new WizardButton(Console.CONSTANTS.common_label_cancel()));
    }

    protected PatchWizardStep(final PatchWizard<C, S> wizard, final String title, WizardButton submitButton,
            WizardButton cancelButton) {
        this.wizard = wizard;
        this.title = title;
        this.submitButton = submitButton;
        this.cancelButton = cancelButton;
    }

    @Override
    public final Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        layout.add(header(wizard.context));
        layout.add(body(wizard.context));

        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNext(wizard.context);
            }
        };
        ClickHandler cancelHandler = new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        onCancel(wizard.context);
                    }
                };

        dialogOptions = new DialogueOptions(submitButton.title, submitHandler, cancelButton.title, cancelHandler);
        if (submitButton.visible) {
            dialogOptions.getSubmit().setId(asId(PREFIX, getClass(), "_Submit"));
            DOM.setElementPropertyBoolean((Element) dialogOptions.getSubmit(), "disabled", !submitButton.enabled);
        } else {
            UIObject.setVisible(dialogOptions.getSubmit(), false);
        }
        if (cancelButton.visible) {
            dialogOptions.getCancel().setId(asId(PREFIX, getClass(), "_Cancel"));
            DOM.setElementPropertyBoolean((Element) dialogOptions.getCancel(), "disabled", !cancelButton.enabled);
        } else {
            UIObject.setVisible(dialogOptions.getCancel(), false);
        }

        Widget windowContent = new WindowContentBuilder(layout, dialogOptions).build();
        windowContent.getElement().setId(asId(PREFIX, getClass()));
        return windowContent;
    }

    public void setEnabled(boolean submitEnabled, boolean cancelEnabled) {
        DOM.setElementPropertyBoolean((Element) dialogOptions.getSubmit(), "disabled", !submitEnabled);
        DOM.setElementPropertyBoolean((Element) dialogOptions.getCancel(), "disabled", !cancelEnabled);
    }

    protected HTML header(final C context) {
        return new HTML(buildTitle());
    }

    private String buildTitle() {
        StringBuilder header = new StringBuilder();
        header.append("<h3>");
        if (title != null) {
            header.append(title);
        }
        header.append("</h3>");
        return header.toString();
    }

    protected abstract IsWidget body(final C context);

    protected void onShow(C context) {}

    protected void onNext(C context) {
        wizard.next();
    }

    @SuppressWarnings("UnusedParameters")
    protected void onCancel(C context) {
        wizard.close();
    }
}
