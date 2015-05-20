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
package org.jboss.as.console.client.v3.deployment.wizard;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.v3.deployment.Content;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Harald Pehl
 */
public class ContentRepositoryStep extends
        WizardStep<Context, State> {

    static class SelectableContent implements CheckboxColumn.Selectable {

        private final Content content;
        private boolean selected;

        SelectableContent(final Content content) {
            this.content = content;
            this.selected = false;
        }

        @Override
        public boolean isSelected() {
            return selected;
        }

        @Override
        public void setSelected(final boolean selected) {
            this.selected = selected;
        }

        public Content getContent() {
            return content;
        }
    }


    private DefaultCellTable<SelectableContent> table;
    private ListDataProvider<SelectableContent> contentProvider;
    private SingleSelectionModel<SelectableContent> selection;

    public ContentRepositoryStep(final DeploymentWizard wizard) {super(wizard, "Uploaded Content");}

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        FlowPanel panel = new FlowPanel();

        ProvidesKey<SelectableContent> keyProvider = item -> item.getContent().getName();

        selection = new SingleSelectionModel<>(keyProvider);

        table = new DefaultCellTable<>(5, keyProvider);
        table.setSelectionModel(selection);
        table.addColumn(new TextColumn<SelectableContent>() {
            @Override
            public String getValue(final SelectableContent item) {
                return item.getContent().getName();
            }
        }, "Name");
        panel.add(table);
        table.addColumn(new CheckboxColumn<>(), "Enable");

        contentProvider = new ListDataProvider<>(keyProvider);
        contentProvider.addDataDisplay(table);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);

        return panel;
    }

    @Override
    public void reset() {
        selection.clear();
    }

    @Override
    protected void onShow(final Context context) {
        List<SelectableContent> data = new ArrayList<>();
        for (Content content : context.contentRepository) {
            data.add(new SelectableContent(content));
        }

        if (data.isEmpty()) {
            wizard.showError("All uploaded content is already assigned to this server group!");
        } else {
            wizard.clearError();
        }
        contentProvider.setList(data);
        table.selectDefaultEntity();
    }

    @Override
    protected boolean onNext(final Context context) {
        if (selection.getSelectedObject() == null) {
            wizard.showError("Please choose an entry!");
            return false;
        }
        wizard.clearError();
        context.existingContent = selection.getSelectedObject().getContent();
        context.enableExistingContent = selection.getSelectedObject().isSelected();
        return true; // actual upload is done in AddDomainDeploymentWizard.finish()
    }
}
