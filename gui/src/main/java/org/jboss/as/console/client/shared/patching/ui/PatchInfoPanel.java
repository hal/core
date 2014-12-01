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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.administration.role.form.EnumFormItem;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.shared.patching.*;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.console.client.shared.patching.PatchType.CUMULATIVE;
import static org.jboss.as.console.client.shared.patching.PatchType.ONE_OFF;
import static org.jboss.as.console.client.shared.patching.Patches.STANDALONE_HOST;
import static org.jboss.as.console.client.shared.patching.ui.PatchManagementElementId.PREFIX;
import static org.jboss.as.console.client.shared.util.IdHelper.asId;

/**
 * Panel which deals with patches of a standalone server or a single host.
 * @author Harald Pehl
 */
public class PatchInfoPanel implements IsWidget, HasPresenter<PatchManagementPresenter> {

    private final ProductConfig productConfig;
    private final PatchManager patchManager;

    private String host;
    private PatchManagementPresenter presenter;
    private ContentHeaderLabel header;
    private Form<PatchInfo> latestForm;
    private TextItem id;
    private PatchInfoTable table;
    private FlowPanel latestContainer;

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
            latestForm.edit(patches.getLatest());
            if (patches.isRestartRequired()) {
                id.setValue(id.getValue() + " (" + Console.CONSTANTS.patch_manager_restart_required() + ")");
            }
        }
    }
}
