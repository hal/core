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
package org.jboss.as.console.client.rbac.internal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.jboss.ballroom.client.rbac.SecurityContextAware;
import org.jboss.ballroom.client.rbac.SecurityService;
import org.jboss.ballroom.client.spi.Framework;

/**
 * Helper class to be used by complex composite like widgets which need support for {@code SecurityContextAware}, but
 * aren't widgets by themselves.
 *
 * @author Harald Pehl
 */
public abstract class SecurityContextAwareVerticalPanel extends VerticalPanel implements SecurityContextAware {

    static Framework FRAMEWORK = GWT.create(Framework.class);
    static SecurityService SECURITY_SERVICE = FRAMEWORK.getSecurityService();

    private final String id;
    private String filter;

    public SecurityContextAwareVerticalPanel() {
        this.id = Document.get().createUniqueId();
        getElement().setId(id);
        SECURITY_SERVICE.registerWidget(id, this);
    }

    @Override
    protected void onLoad() {
        SECURITY_SERVICE.registerWidget(id, this);
    }

    @Override
    protected void onUnload() {
        SECURITY_SERVICE.unregisterWidget(id);
    }

    @Override
    public void setFilter(final String resourceAddress) {
        this.filter = resourceAddress;
    }

    @Override
    public String getFilter() {
        return filter;
    }
}
