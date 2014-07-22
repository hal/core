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

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.domain.model.Host;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;

/**
 * Contains a {@link org.jboss.as.console.client.widgets.pages.PagedView} with all hosts and links to {@link
 * org.jboss.as.console.client.shared.patching.ui.PatchPanel}s
 *
 * @author Harald Pehl
 */
public class HostsPanel implements IsWidget {

    private final HostStore hostStore;

    public HostsPanel(final HostStore hostStore) {this.hostStore = hostStore;}

    @Override
    public Widget asWidget() {
        PagedView pagedView = new PagedView();

        DefaultCellTable<Host> table = new DefaultCellTable<Host>(8);

        return null;
    }
}
