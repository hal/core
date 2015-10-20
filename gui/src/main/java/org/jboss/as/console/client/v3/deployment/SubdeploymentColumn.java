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
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.as.console.client.widgets.nav.v3.MenuDelegate.Role.Navigation;

/**
 * @author Harald Pehl
 */
public class SubdeploymentColumn extends FinderColumn<Subdeployment> {

    private Widget widget;

    @SuppressWarnings("unchecked")
    public SubdeploymentColumn(final PlaceManager placeManager, Dispatcher circuit,
            final ColumnManager columnManager, final int reduceTo, final String token) {

        super(FinderColumn.FinderId.DEPLOYMENT, "Nested Deployment",
                new FinderColumn.Display<Subdeployment>() {
                    @Override
                    public boolean isFolder(final Subdeployment data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Subdeployment data) {
                        return Templates.ITEMS.item(baseCss, data.getName(), data.getName());
                    }

                    @Override
                    public String rowCss(final Subdeployment data) {
                        return "";
                    }
                },
                new ProvidesKey<Subdeployment>() {
                    @Override
                    public Object getKey(final Subdeployment item) {
                        return item.getName();
                    }
                },
                token
        );

        setPreviewFactory((data, callback) -> callback.onSuccess(Templates.subdeploymentPreview(data)));

        setMenuItems(new MenuDelegate<>("View",
                item -> placeManager.revealRelativePlace(
                        new PlaceRequest.Builder().nameToken(NameTokens.DeploymentDetails).build()),
                Navigation));

        addSelectionChangeHandler(event -> {
            columnManager.reduceColumnsTo(reduceTo);
            if (hasSelectedItem()) {
                columnManager.updateActiveSelection(asWidget());
                circuit.dispatch(new SelectDeploymentAction(getSelectedItem()));
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
