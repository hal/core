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
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Harald Pehl
 */
public class ServerGroupDeploymentView implements ServerGroupDeploymentPresenter.MyView {

    private ServerGroupDeploymentPresenter presenter;

    // ------------------------------------------------------ view lifecycle

    @Override
    public Widget asWidget() {
        return null;
    }


    @Override
    public void setPresenter(final ServerGroupDeploymentPresenter presenter) {
        this.presenter = presenter;
    }


    // ------------------------------------------------------ slot management

    @Override
    public void addToSlot(final Object slot, final IsWidget content) {

    }

    @Override
    public void removeFromSlot(final Object slot, final IsWidget content) {

    }

    @Override
    public void setInSlot(final Object slot, final IsWidget content) {

    }


    // ------------------------------------------------------ finder related methods

    @Override
    public void preview(final SafeHtml html) {

    }

    @Override
    public void toggleScrolling(final boolean enforceScrolling, final int requiredWidth) {

    }
}
