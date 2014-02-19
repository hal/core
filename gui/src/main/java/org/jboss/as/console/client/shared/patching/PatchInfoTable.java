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

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

/**
 * @author Harald Pehl
 */
public class PatchInfoTable implements IsWidget {

    private static final int PAGE_SIZE = 8;
    private DefaultCellTable<PatchInfo> table;
    private ListDataProvider<PatchInfo> dataProvider;
    private SingleSelectionModel<PatchInfo> selectionModel;

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        ProvidesKey<PatchInfo> keyProvider = new ProvidesKey<PatchInfo>() {
            @Override
            public Object getKey(PatchInfo item) {
                return item.getId();
            }
        };
        table = new DefaultCellTable<PatchInfo>(PAGE_SIZE, keyProvider);
        selectionModel = new SingleSelectionModel(keyProvider);
        table.setSelectionModel(selectionModel);
        dataProvider = new ListDataProvider<PatchInfo>();
        dataProvider.addDataDisplay(table);

        TextColumn<PatchInfo> idColumn = new TextColumn<PatchInfo>() {
            @Override
            public String getValue(PatchInfo record) {
                return record.getId();
            }
        };
        TextColumn<PatchInfo> versionColumn = new TextColumn<PatchInfo>() {
            @Override
            public String getValue(PatchInfo record) {
                return record.getVersion();
            }
        };
        table.addColumn(idColumn, "ID");
        table.addColumn(versionColumn, "Version");

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        layout.add(table);
        layout.add(pager);
        return layout;
    }

    PatchInfo getCurrentSelection() {
        return selectionModel.getSelectedObject();
    }
}
