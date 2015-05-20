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
package org.jboss.as.console.client.v3.deployment;

import com.google.common.base.Function;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;

/**
 * @author Harald Pehl
 */
public class DeploymentColumn extends FinderColumn<Deployment> {

    private Widget widget;

    public DeploymentColumn(final String title, final ColumnManager columnManager,
            final SubdeploymentColumn subdeploymentColumn, final int reduceTo,
            final ColumnHtmlProvider<Deployment> columnHtml,
            final Function<Deployment, String> rowCss,
            final Function<Deployment, SafeHtml> previewHtml) {

        super(FinderColumn.FinderId.DEPLOYMENT, title,
                new FinderColumn.Display<Deployment>() {
                    @Override
                    public boolean isFolder(final Deployment data) {
                        return data.hasSubdeployments();
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Deployment data) {
                        return columnHtml.provideHtml(baseCss, data);
                    }

                    @Override
                    public String rowCss(final Deployment data) {
                        return rowCss.apply(data);
                    }
                },
                new ProvidesKey<Deployment>() {
                    @Override
                    public Object getKey(final Deployment item) {
                        return item.getName();
                    }
                }
        );

        setPreviewFactory((data, callback) -> callback.onSuccess(previewHtml.apply(data)));

        addSelectionChangeHandler(event -> {
            columnManager.reduceColumnsTo(reduceTo);
            if (hasSelectedItem()) {
                columnManager.updateActiveSelection(widget);
                Deployment deployment = getSelectedItem();
                if (deployment.hasSubdeployments()) {
                    columnManager.appendColumn(subdeploymentColumn.asWidget());
                    subdeploymentColumn.updateFrom(deployment.getSubdeployments());
                }
            }
        });
    }

    @Override
    public Widget asWidget() {
        if (widget == null) {
            widget = super.asWidget();
        }
        return widget;
    }
}
