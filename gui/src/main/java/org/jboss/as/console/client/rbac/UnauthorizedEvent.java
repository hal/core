/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.rbac;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

public class UnauthorizedEvent extends GwtEvent<UnauthorizedEvent.UnauthorizedHandler> {

    public static final Type TYPE = new Type<UnauthorizedHandler>();
    private final String token;

    public UnauthorizedEvent(final String token) {this.token = token;}

    public static void fire(final HasHandlers source, String token) {
        source.fireEvent(new UnauthorizedEvent(token));
    }

    public String getToken() {
        return token;
    }

    @Override
    public Type<UnauthorizedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(UnauthorizedHandler listener) {
        listener.onUnauthorized(this);
    }

    public interface UnauthorizedHandler extends EventHandler {
        void onUnauthorized(UnauthorizedEvent event);
    }
}


