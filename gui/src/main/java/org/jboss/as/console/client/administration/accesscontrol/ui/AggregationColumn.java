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
package org.jboss.as.console.client.administration.accesscontrol.ui;

import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignment;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;

/**
 * Column used to distinguish between the included and excluded assignments.
 *
 * @author Harald Pehl
 */
public class AggregationColumn extends FinderColumn<AggregationItem> {

    static final PreviewContent PREVIEW_CONTENT = PreviewContent.INSTANCE;

    private Widget widget;

    public AggregationColumn(final String title,
            final PreviewContentFactory contentFactory,
            final ExternalTextResource textResource,
            final ColumnManager columnManager,
            final FinderColumn<Assignment> nextColumn,
            final Widget nextColumnWidget, String token) {

        super(FinderId.ACCESS_CONTROL,
                title,
                new Display<AggregationItem>() {
                    @Override
                    public boolean isFolder(final AggregationItem data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final AggregationItem data) {
                        return Templates.aggregationItem(baseCss, data);
                    }

                    @Override
                    public String rowCss(final AggregationItem data) {
                        return "";
                    }
                },
                new ProvidesKey<AggregationItem>() {
                    @Override
                    public Object getKey(final AggregationItem item) {
                        return item.isInclude();
                    }
                }, token);

        setPreviewFactory((data, callback) -> contentFactory.createContent(textResource, callback));

        addSelectionChangeHandler(event -> {
            columnManager.reduceColumnsTo(3);
            if (hasSelectedItem()) {
                columnManager.updateActiveSelection(asWidget());
                columnManager.appendColumn(nextColumnWidget);
                nextColumn.updateFrom(getSelectedItem().getEntries());
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
