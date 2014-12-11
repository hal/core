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
package org.jboss.as.console.client.core;

import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtplatform.common.client.IndirectProvider;
import com.gwtplatform.mvp.client.Presenter;
import org.jboss.as.console.client.Console;

/**
 * @author Harald Pehl
 */
public class RequiredResourcesProvider<P extends Presenter<?, ?>> implements IndirectProvider<P> {

    private final AsyncProvider<P> provider;
    private final RequiredResourcesLoader loader;

    public RequiredResourcesProvider(AsyncProvider<P> provider) {
        this.provider = provider;
        this.loader = Console.MODULES.getRequiredResourcesLoader();
    }

    @Override
    public void get(AsyncCallback<P> callback) {
        loader.loadRequiredResources(provider, callback);
    }
}
