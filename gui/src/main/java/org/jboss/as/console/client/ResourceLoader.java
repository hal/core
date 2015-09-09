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
package org.jboss.as.console.client;

import com.google.gwt.core.client.ScriptInjector;
import com.google.inject.Inject;
import org.jboss.as.console.client.widgets.progress.ProgressPolyfill;

/**
 * @author Harald Pehl
 */
public class ResourceLoader {
    @Inject
    public ResourceLoader(ConsoleResources resources, ProductConfig productConfig) {
        resources.css().ensureInjected();
        if (productConfig.getProfile().equals(ProductConfig.Profile.COMMUNITY)) {
            resources.communityStyles().ensureInjected();
        } else {
            resources.productStyles().ensureInjected();
        }
        //resources.verticalTabPanelStyles().ensureInjected();

        resources.prettifyCss().ensureInjected();
        ProgressPolyfill.inject();
        ScriptInjector.fromString(resources.prettifyJs().getText()).setWindow(ScriptInjector.TOP_WINDOW)
                .inject();
        ScriptInjector.fromString(resources.lunrJs().getText()).setWindow(ScriptInjector.TOP_WINDOW)
                .inject();
        ScriptInjector.fromString(resources.mousetrapJs().getText()).setWindow(
                ScriptInjector.TOP_WINDOW).inject();
        ScriptInjector.fromString(resources.protovis().getText()).setWindow(ScriptInjector.TOP_WINDOW)
                .inject();
    }
}
