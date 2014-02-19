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
package org.jboss.as.console.client.shared.patching;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

/**
 * @author Harald Pehl
 */
public class PatchManagerView extends SuspendableViewImpl implements PatchManagerPresenter.MyView {

    private final ProductConfig productConfig;
    private PatchManagerPresenter presenter;
    private Form<PatchInfo> latestForm;
    private PatchInfoTable table;

    @Inject
    public PatchManagerView(ProductConfig productConfig) {
        this.productConfig = productConfig;
    }

    @Override
    public Widget createWidget() {

        VerticalPanel panel = new VerticalPanel();

        // header
        panel.add(new ContentHeaderLabel("Patch Manager"));
        panel.add(new ContentDescription(Console.MESSAGES.patch_manager_desc(productConfig.getProductName())));

        // latest patch info
        latestForm = new Form<PatchInfo>(PatchInfo.class);
        latestForm.setEnabled(false);
        TextItem id = new TextItem("id", Console.CONSTANTS.patch_manager_latest());
        TextItem version = new TextItem("version", "Version");
        latestForm.setFields(id, version);
        panel.add(latestForm);

        // tools & table
        table = new PatchInfoTable();
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.patch_manager_apply_new(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchNewPatchWizard();
            }
        }));
        ClickHandler rollbackHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final PatchInfo currentSelection = table.getCurrentSelection();
                if (currentSelection != null) {
                    Feedback.confirm(
                            Console.CONSTANTS.patch_manager_rollback(),
                            Console.MESSAGES.patch_manager_rollback_body(currentSelection.getId()),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean isConfirmed) {
                                    if (isConfirmed) {
                                        presenter.onRollback(currentSelection);
                                    }
                                }
                            }
                    );
                }
            }
        };
        ToolButton rollbackButton = new ToolButton(Console.CONSTANTS.patch_manager_rollback());
        rollbackButton.addClickHandler(rollbackHandler);
        tools.addToolButtonRight(rollbackButton);

        panel.add(new ContentGroupLabel(Console.CONSTANTS.patch_manager_recently()));
        panel.add(tools);
        panel.add(table);

        // assemble the panels
        panel.setStyleName("rhs-content-panel");
        ScrollPanel scroll = new ScrollPanel(panel);
        LayoutPanel layout = new LayoutPanel();
        layout.add(scroll);
        layout.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 100, Style.Unit.PCT);
        return layout;
    }

    @Override
    public void setPresenter(final PatchManagerPresenter presenter) {
        this.presenter = presenter;
    }
}
