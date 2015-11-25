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

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;

/**
 * @author Harald Pehl
 */
public class BrowseByColumn extends FinderColumn<BrowseByItem> {

    private Widget widget;

    @SuppressWarnings("unchecked")
    public BrowseByColumn(final PreviewContentFactory contentFactory,
            final SelectionChangeEvent.Handler selectionHandler, String token) {

        super(FinderId.DEPLOYMENT,
                ((UIConstants) GWT.create(UIConstants.class)).browseBy(),
                new Display<BrowseByItem>() {
                    @Override
                    public boolean isFolder(final BrowseByItem data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final BrowseByItem data) {
                        return Templates.ITEMS.item(baseCss, data.getTitle(), data.getTitle());
                    }

                    @Override
                    public String rowCss(final BrowseByItem data) {
                        return "";
                    }
                },
                new ProvidesKey<BrowseByItem>() {
                    @Override
                    public Object getKey(final BrowseByItem item) {
                        return item.getTitle();
                    }
                }, token);

        setPreviewFactory((data, callback) -> contentFactory.createContent(data.getPreview(), callback));
        addSelectionChangeHandler(selectionHandler);
    }

    @Override
    public Widget asWidget() {
        if (widget == null) {
            widget = super.asWidget();
        }
        return widget;
    }
}
