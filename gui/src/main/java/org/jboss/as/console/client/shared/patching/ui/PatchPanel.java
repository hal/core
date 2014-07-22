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
package org.jboss.as.console.client.shared.patching.ui;

import static org.jboss.as.console.client.shared.patching.PatchManagerElementId.PREFIX;
import static org.jboss.as.console.client.shared.patching.PatchType.CUMULATIVE;
import static org.jboss.as.console.client.shared.patching.PatchType.ONE_OFF;
import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.administration.role.form.EnumFormItem;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.PatchManagerPresenter;
import org.jboss.as.console.client.shared.patching.PatchType;
import org.jboss.as.console.client.shared.patching.Patches;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

/**
 * Panel for patching a standalone server or a single host.
 * @author Harald Pehl
 */
public class PatchPanel implements IsWidget {

    private class RestartHandler implements ClickHandler {

        @Override
        public void onClick(final ClickEvent event) {
            String message;
            if (bootstrapContext.isStandalone()) {
                message = Console.CONSTANTS.patch_manager_restart_verify();
            } else {
                message = Console.MESSAGES.patch_manager_restart_verify(hostStore.getSelectedHost());
            }
            Feedback.confirm(Console.CONSTANTS.common_label_restart(), message,
                    new Feedback.ConfirmationHandler() {
                        @Override
                        public void onConfirmation(boolean isConfirmed) {
                            if (isConfirmed) {
                                presenter.restart();
                            }
                        }
                    }
            );
        }
    }


    private final ProductConfig productConfig;
    private final BootstrapContext bootstrapContext;
    private final HostStore hostStore;

    private PatchManagerPresenter presenter;
    private Form<PatchInfo> latestForm;
    private TextItem id;
    private PatchInfoTable table;
    private FlowPanel latestContainer;

    public PatchPanel(final ProductConfig productConfig, final BootstrapContext bootstrapContext,
            final HostStore hostStore) {
        this.productConfig = productConfig;
        this.bootstrapContext = bootstrapContext;
        this.hostStore = hostStore;
    }

    @Override
    public Widget asWidget() {

        VerticalPanel panel = new VerticalPanel();

        // header
        panel.add(new ContentHeaderLabel("Patch Management"));
        if (productConfig.getProfile() == ProductConfig.Profile.PRODUCT) {
            panel.add(new ContentDescription(
                    Console.MESSAGES.patch_manager_desc_product()));
        } else {
            panel.add(new ContentDescription(Console.CONSTANTS.patch_manager_desc_community()));
        }
        panel.add(new ContentDescription(Console.CONSTANTS.patch_manager_toolstrip_desc()));

        // latest patch info
        latestContainer = new FlowPanel();
        latestContainer.add(new ContentGroupLabel(Console.CONSTANTS.patch_manager_patch_information()));
        latestForm = new Form<PatchInfo>(PatchInfo.class);
        latestForm.setEnabled(false);
        id = new TextItem("id", Console.CONSTANTS.patch_manager_latest());
        TextItem version = new TextItem("version", "Version");
        TextItem date = new TextItem("appliedAt", Console.CONSTANTS.patch_manager_applied_at());
        EnumFormItem<PatchType> type = new EnumFormItem<PatchType>("type", Console.CONSTANTS.common_label_type());
        Map<PatchType, String> values = new HashMap<PatchType, String>();
        values.put(CUMULATIVE, CUMULATIVE.label());
        values.put(ONE_OFF, ONE_OFF.label());
        type.setValues(values);
        latestForm.setFields(id, version, date, type);
        latestContainer.add(latestForm);
        panel.add(latestContainer);

        // tools & table
        ToolStrip tools = new ToolStrip();
        ToolButton apply = new ToolButton(Console.CONSTANTS.patch_manager_apply_new(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchApplyWizard();
            }
        });
        apply.setOperationAddress("/{selected.host}/core-service=patching", "patch");
        apply.getElement().setId(asId(PREFIX, getClass(), "_Apply"));
        tools.addToolButtonRight(apply);
        ClickHandler rollbackHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final PatchInfo currentSelection = table.getCurrentSelection();
                if (currentSelection != null) {
                    presenter.launchRollbackWizard(currentSelection);
                }
            }
        };
        ToolButton rollbackButton = new ToolButton(Console.CONSTANTS.patch_manager_rollback());
        rollbackButton.setOperationAddress("/{selected.host}/core-service=patching", "rollback");
        rollbackButton.getElement().setId(asId(PREFIX, getClass(), "_Rollback"));
        rollbackButton.addClickHandler(rollbackHandler);
        tools.addToolButtonRight(rollbackButton);

        ToolButton restartButton = new ToolButton(Console.CONSTANTS.common_label_restart());
        restartButton.setOperationAddress("/{selected.host}", "shutdown");
        restartButton.getElement().setId(asId(PREFIX, getClass(), "_Restart"));
        restartButton.addClickHandler(new RestartHandler());
        tools.addToolButtonRight(restartButton);

        panel.add(new ContentGroupLabel(Console.CONSTANTS.patch_manager_recently()));
        panel.add(tools);
        table = new PatchInfoTable();
        panel.add(table);

        // assemble the panels
        panel.setStyleName("rhs-content-panel");
        ScrollPanel scroll = new ScrollPanel(panel);
        FakeTabPanel titleBar = new FakeTabPanel("Patch Management");
        titleBar.add(scroll);
        LayoutPanel layout = new LayoutPanel();
        layout.add(titleBar);
        layout.add(scroll);
        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(scroll, 40, Style.Unit.PX, 100, Style.Unit.PCT);
        return layout;
    }

    public void setPresenter(final PatchManagerPresenter presenter) {
        this.presenter = presenter;
    }

    public void update(final Patches patches) {
        table.update(patches);
        boolean latestAvailable = patches.getLatest() != null;
        latestContainer.setVisible(latestAvailable);
        if (latestAvailable) {
            latestForm.edit(patches.getLatest());
            if (patches.isRestartRequired()) {
                id.setValue(id.getValue() + " (" + Console.CONSTANTS.patch_manager_restart_required() + ")");
            }
        }
    }
}
