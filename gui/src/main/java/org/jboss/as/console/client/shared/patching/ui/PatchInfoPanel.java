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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.*;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.PatchManagementPresenter;
import org.jboss.as.console.client.shared.patching.PatchManager;
import org.jboss.as.console.client.shared.patching.Patches;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static org.jboss.as.console.client.shared.patching.Patches.STANDALONE_HOST;
import static org.jboss.as.console.client.shared.patching.ui.PatchManagementElementId.PREFIX;
import static org.jboss.as.console.client.shared.util.IdHelper.asId;

/**
 * Panel which deals with patches of a standalone server or a single host.
 * @author Harald Pehl
 */
public class PatchInfoPanel implements IsWidget, HasPresenter<PatchManagementPresenter> {

    private final static Template TEMPLATE = GWT.create(Template.class);

    private final ProductConfig productConfig;
    private final PatchManager patchManager;

    private String host;
    private PatchManagementPresenter presenter;
    private ContentHeaderLabel header;
    private PatchInfoTable table;
    private FlowPanel latestContainer;
    private HTML latestAppliedPatch;

    public PatchInfoPanel(ProductConfig productConfig, PatchManager patchManager) {
        this.productConfig = productConfig;
        this.patchManager = patchManager;
    }

    @Override
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();

        // header
        header = new ContentHeaderLabel("Patch Management");
        panel.add(header);
        if (productConfig.getProfile() == ProductConfig.Profile.PRODUCT) {
            panel.add(new ContentDescription(Console.MESSAGES.patch_manager_desc_product()));
        } else {
            panel.add(new ContentDescription(Console.CONSTANTS.patch_manager_desc_community()));
        }
        panel.add(new ContentDescription(Console.CONSTANTS.patch_manager_toolstrip_desc()));

        // latest patch info
        latestContainer = new FlowPanel();
        latestAppliedPatch = new HTML();
        latestAppliedPatch.getElement().getStyle().setMarginBottom(20, PX);
        latestContainer.add(new HTML("<h3 class=\"metric-label-embedded\">" + Console.CONSTANTS.patch_manager_latest() + "</h3>"));
        latestContainer.add(latestAppliedPatch);
        panel.add(latestContainer);

        // tools & table
        ToolStrip tools = new ToolStrip();
        ToolButton applyButton = new ToolButton(Console.CONSTANTS.patch_manager_apply_new(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchApplyWizard();
            }
        });
        applyButton.setOperationAddress("/{selected.host}/core-service=patching", "patch");
        applyButton.getElement().setId(asId(PREFIX, getClass(), "_Apply"));
        tools.addToolButtonRight(applyButton);
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
        restartButton.addClickHandler(new RestartHandler(host, presenter));
        tools.addToolButtonRight(restartButton);

        panel.add(new ContentGroupLabel(Console.CONSTANTS.patch_manager_recently()));
        panel.add(tools);
        table = new PatchInfoTable(patchManager);
        panel.add(table);

        return panel;
    }

    @Override
    public void setPresenter(final PatchManagementPresenter presenter) {
        this.presenter = presenter;
    }

    public void update(Patches patches) {
        host = patches.getHost();
        if (host != null && !host.equals(STANDALONE_HOST)) {
            header.setText("Patch Management / Host " + host);
        }

        table.update(patches);
        boolean latestAvailable = patches.getLatest() != null;
        latestContainer.setVisible(latestAvailable);
        if (latestAvailable) {
            String id = patches.getLatest().getId();
            if (patches.isRestartRequired()) {
                latestAppliedPatch.setHTML(TEMPLATE.latestAppliedPatchPendingRestart(id,
                        Console.CONSTANTS.patch_manager_restart_required()));
            } else {
                latestAppliedPatch.setHTML(TEMPLATE.latestAppliedPatch(id));
            }
        }
    }


    interface Template extends SafeHtmlTemplates {

        @Template("<b>{0}</b>")
        SafeHtml latestAppliedPatch(String patchId);

        @Template("<b>{0}</b> ({1})")
        SafeHtml latestAppliedPatchPendingRestart(String patchId, String pending);
    }
}
