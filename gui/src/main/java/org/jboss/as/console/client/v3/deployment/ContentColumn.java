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

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;

import static org.jboss.as.console.client.widgets.nav.v3.FinderColumn.FinderId.DEPLOYMENT;

/**
 * @author Harald Pehl
 */
public class ContentColumn extends FinderColumn<Content> {

    private Widget widget;

    @SuppressWarnings("unchecked")
    public ContentColumn(final String title, final ColumnManager columnManager,
            final MenuDelegate<Content> topMenuItem, final MenuDelegate<Content>... contextMenuItems) {
        super(DEPLOYMENT, title,
                new Display<Content>() {
                    @Override
                    public boolean isFolder(final Content data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Content data) {
                        return Templates.ITEMS.item(baseCss, data.getName(), data.getName());
                    }

                    @Override
                    public String rowCss(final Content data) {
                        return "";
                    }
                },
                new ProvidesKey<Content>() {
                    @Override
                    public Object getKey(final Content item) {
                        return item.getName();
                    }
                },
                NameTokens.DomainDeploymentFinder,
                999);

        setShowSize(true);
        setPreviewFactory((data, callback) -> callback.onSuccess(Templates.contentPreview(data)));

        if (topMenuItem != null) {
            setTopMenuItems(topMenuItem);
        }
        if (contextMenuItems != null) {
            setMenuItems(contextMenuItems);
        }

        addSelectionChangeHandler(event -> {
            columnManager.reduceColumnsTo(2);
            if (hasSelectedItem()) {
                columnManager.updateActiveSelection(asWidget());
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
