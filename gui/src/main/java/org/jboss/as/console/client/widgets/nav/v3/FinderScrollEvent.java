package org.jboss.as.console.client.widgets.nav.v3;

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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;


/**
 * Fired bu {@link org.jboss.as.console.client.widgets.nav.v3.ColumnManager}'s to indicate the parent container
 * to enforce scrolling. Th parent container is the top most view of a finder presenter hierarchy.
 */
public class FinderScrollEvent extends GwtEvent<FinderScrollEvent.Handler> {

    public static final Type TYPE = new Type<Handler>();

    private boolean enforceScrolling = false;
    private int requiredWidth;

    public FinderScrollEvent(boolean enforceScrolling, int requiredWidth) {

        this.enforceScrolling = enforceScrolling;
        this.requiredWidth = requiredWidth;

    }


    public boolean isEnforceScrolling() {
        return enforceScrolling;
    }

    public static void fire(final HasHandlers source, boolean enforceScrolling, int requiredWidth) {
        source.fireEvent(new FinderScrollEvent(enforceScrolling, requiredWidth));
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler listener) {
        listener.onToggleScrolling(this);
    }

    public int getRequiredWidth() {
        return requiredWidth;
    }

    public interface Handler extends EventHandler {
        void onToggleScrolling(FinderScrollEvent event);
    }
}


