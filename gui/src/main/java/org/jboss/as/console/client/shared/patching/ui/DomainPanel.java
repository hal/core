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

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.Updateable;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.PatchManagementPresenter;
import org.jboss.as.console.client.shared.patching.PatchManager;
import org.jboss.as.console.client.shared.patching.Patches;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

/**
 * Contains a {@link org.jboss.as.console.client.widgets.pages.PagedView} with a hosts table and
 * a {@link org.jboss.as.console.client.shared.patching.ui.PatchInfoPanel}s
 *
 * @author Harald Pehl
 */
public class DomainPanel implements IsWidget, HasPresenter<PatchManagementPresenter>, Updateable<List<Patches>> {

    private final Dispatcher circuit;
    private final PagedView pagedView;
    private final PatchInfoPanel patchInfoPanel;
    private final ListDataProvider<Patches> dataProvider;

    public DomainPanel(Dispatcher circuit, ProductConfig productConfig, PatchManager patchManager) {
        this.circuit = circuit;
        this.pagedView = new PagedView();
        this.patchInfoPanel = new PatchInfoPanel(productConfig, patchManager);
        this.dataProvider = new ListDataProvider<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {

        // table
        ProvidesKey<Patches> keyProvider = new ProvidesKey<Patches>() {
            @Override
            public Object getKey(Patches entry) {
                return entry.getHost();
            }
        };
        DefaultCellTable<Patches> table = new DefaultCellTable<>(8, keyProvider);
        dataProvider.addDataDisplay(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        SingleSelectionModel<Patches> selectionModel = new SingleSelectionModel<>(keyProvider);
        table.setSelectionModel(selectionModel);

        // columns
        Column nameColumn = new TextColumn<Patches>() {
            @Override
            public String getValue(Patches patches) {
                return patches.getHost();
            }
        };
        Column patchInfoColumn = new TextColumn<Patches>() {
            @Override
            public String getValue(Patches patches) {
                StringBuilder builder = new StringBuilder();
                PatchInfo latest = patches.getLatest();
                if (latest != null) {
                    builder.append(latest.getId());
                } else {
                    builder.append("n/a");
                }
                return builder.toString();
            }
        };
        ActionCell.Delegate<Patches> actionDelegate = new ActionCell.Delegate<Patches>() {
            @Override
            public void execute(Patches patches) {
                circuit.dispatch(new HostSelection(patches.getHost()));
                patchInfoPanel.update(patches);
                pagedView.showPage(1);
            }
        };
        Column<Patches, Patches> option = new Column<Patches, Patches>(
                new ViewLinkCell<Patches>(Console.CONSTANTS.common_label_view(), actionDelegate)) {
            @Override
            public Patches getValue(Patches entry) {
                return entry;
            }
        };
        table.addColumn(nameColumn, Console.CONSTANTS.common_label_host());
        table.addColumn(patchInfoColumn, Console.CONSTANTS.patch_manager_latest());
        table.addColumn(option, Console.CONSTANTS.common_label_option());

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add(table);
        wrapper.add(pager);
        SimpleLayout overviewPanel = new SimpleLayout()
                .setPlain(true)
                .setHeadline("Patch Management")
                .setDescription(Console.MESSAGES.pleaseChoseanItem())
                .addContent(Console.MESSAGES.available("Groups"), wrapper);

        VerticalPanel patchInfoWrapper = new VerticalPanel();
        patchInfoWrapper.setStyleName("rhs-content-panel");
        patchInfoWrapper.add(patchInfoPanel);

        pagedView.addPage("Hosts", overviewPanel.build());
        pagedView.addPage("Patches", new ScrollPanel(patchInfoWrapper));
        pagedView.showPage(0);

        DefaultTabLayoutPanel tabLayoutPanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutPanel.addStyleName("default-tabpanel");
        tabLayoutPanel.add(pagedView.asWidget(), "Patch Management");
        return tabLayoutPanel;
    }

    @Override
    public void setPresenter(PatchManagementPresenter presenter) {
        patchInfoPanel.setPresenter(presenter);
    }

    @Override
    public void update(List<Patches> patches) {
        dataProvider.setList(patches);
    }
}
