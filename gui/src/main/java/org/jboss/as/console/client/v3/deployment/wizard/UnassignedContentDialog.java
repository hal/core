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
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.deployment.Content;
import org.jboss.as.console.client.v3.deployment.DomainDeploymentFinder;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;
import java.util.Set;

/**
 * @author Harald Pehl
 */
public class UnassignedContentDialog implements IsWidget {

    private final DomainDeploymentFinder domainDeploymentFinder;
    private DefaultWindow window;
    private ListDataProvider<Content> dataProvider;

    public UnassignedContentDialog(final DomainDeploymentFinder domainDeploymentFinder) {
        this.domainDeploymentFinder = domainDeploymentFinder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel root = new VerticalPanel();
        root.setStyleName("window-content");
        root.add(new Label("The following content is not assigned to any server group. To remove the content select one or multiple items and press 'Remove'"));

        ProvidesKey<Content> keyProvider = Content::getName;
        MultiSelectionModel<Content> selectionModel = new MultiSelectionModel<>(keyProvider);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            Set<Content> selection = selectionModel.getSelectedSet();
            if (!selection.isEmpty()) {
                domainDeploymentFinder.removeContent(selection);
                close();
            }
        }));
        root.add(toolStrip);

        DefaultCellTable<Content> table = new DefaultCellTable<>(8, keyProvider);
        table.setSelectionModel(selectionModel);
        table.addColumn(new TextColumn<Content>() {
            @Override
            public String getValue(final Content item) {
                return item.getName();
            }
        }, "Name");

        dataProvider = new ListDataProvider<>(keyProvider);
        dataProvider.addDataDisplay(table);
        root.add(table);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        root.add(pager);

        DialogueOptions dialogueOptions = new DialogueOptions("Close", event -> close(), "", event -> {});
        dialogueOptions.showCancel(false);
        return new TrappedFocusPanel(new WindowContentBuilder(root, dialogueOptions.asWidget()).build());
    }

    public void open(List<Content> content) {
        if (window == null) {
            window = new DefaultWindow("Unassigned Content");
            window.setWidth(400);
            window.setHeight(450);
            window.trapWidget(asWidget());
            window.setGlassEnabled(true);
        }
        dataProvider.setList(content);
        window.center();
    }

    public void close() {
        window.hide();
    }
}
