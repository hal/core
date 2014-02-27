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

import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.administration.role.form.EnumFormItem;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

/**
 * @author Harald Pehl
 */
public class PatchManagerView extends SuspendableViewImpl
        implements PatchManagerPresenter.MyView, PatchManagerElementId {

    private final ProductConfig productConfig;
    private PatchManagerPresenter presenter;
    private Form<PatchInfo> latestForm;
    private PatchInfoTable table;
    private FlowPanel latestContainer;

    @Inject
    public PatchManagerView(ProductConfig productConfig) {
        this.productConfig = productConfig;
    }

    @Override
    public Widget createWidget() {

        VerticalPanel panel = new VerticalPanel();

        // header
        panel.add(new ContentHeaderLabel("Patch Management"));
        if (productConfig.getProfile() == ProductConfig.Profile.PRODUCT) {
            panel.add(new ContentDescription(
                    Console.MESSAGES.patch_manager_desc_product(PatchManagerPresenter.CUSTOMER_PORTAL)));
        } else {
            panel.add(new ContentDescription(Console.CONSTANTS.patch_manager_desc_community()));
        }
        panel.add(new ContentDescription(Console.CONSTANTS.patch_manager_toolstrip_desc()));

        // latest patch info
        latestContainer = new FlowPanel();
        latestContainer.add(new ContentGroupLabel(Console.CONSTANTS.patch_manager_patch_information()));
        latestForm = new Form<PatchInfo>(PatchInfo.class);
        latestForm.setEnabled(false);
        TextItem id = new TextItem("id", Console.CONSTANTS.patch_manager_latest());
        TextItem version = new TextItem("version", "Version");
        TextItem date = new TextItem("appliedAt", Console.CONSTANTS.patch_manager_applied_at());
        EnumFormItem<PatchType> type = new EnumFormItem<>("appliedAt", Console.CONSTANTS.patch_manager_applied_at());
        Map<PatchType, String> typeLabels = new HashMap<PatchType, String>();
        typeLabels.put(PatchType.CUMULATIVE, "Cumulutative");
        typeLabels.put(PatchType.ONE_OFF, "One-Off");
        type.setValues(typeLabels);
        latestForm.setFields(id, version, date, type);
        latestContainer.add(latestForm);
        panel.add(latestContainer);

        // tools & table
        table = new PatchInfoTable();
        ToolStrip tools = new ToolStrip();
        ToolButton apply = new ToolButton(Console.CONSTANTS.patch_manager_apply_new(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchApplyWizard();
            }
        });
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
        rollbackButton.getElement().setId(asId(PREFIX, getClass(), "_Rollback"));
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

    @Override
    public void update(final Patches patches) {
        table.update(patches);
        boolean latestAvailable = patches.getLatest() != null;
        latestContainer.setVisible(latestAvailable);
        if (latestAvailable) {
            latestForm.edit(patches.getLatest());
        }
    }
}
