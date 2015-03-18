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
package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.inject.Inject;

/**
 * Simple wrapper around an ordered array of HAL's bootstrap steps.
 *
 * @author Harald Pehl
 */
public class BootstrapSteps {

    private final LoadGoogleViz loadGoogleViz;
    private final ExecutionMode executionMode;
    private final TrackExecutionMode trackExecutionMode;
    private final LoadCompatMatrix loadCompatMatrix;
    private final RegisterSubsystems registerSubsystems;
    private final EagerLoadProfiles eagerLoadProfiles;
    private final HostStoreInit hostStoreInit;
    private final ServerStoreInit serverStoreInit;
    private final EagerLoadGroups eagerLoadGroups;

    @Inject
    public BootstrapSteps(LoadGoogleViz loadGoogleViz,
                          ExecutionMode executionMode,
                          TrackExecutionMode trackExecutionMode,
                          LoadCompatMatrix loadCompatMatrix,
                          RegisterSubsystems registerSubsystems,
                          EagerLoadProfiles eagerLoadProfiles,
                          HostStoreInit hostStoreInit,
                          ServerStoreInit serverStoreInit,
                          EagerLoadGroups eagerLoadGroups) {

        this.loadGoogleViz = loadGoogleViz;
        this.executionMode = executionMode;
        this.trackExecutionMode = trackExecutionMode;
        this.loadCompatMatrix = loadCompatMatrix;
        this.registerSubsystems = registerSubsystems;
        this.eagerLoadProfiles = eagerLoadProfiles;
        this.hostStoreInit = hostStoreInit;
        this.serverStoreInit = serverStoreInit;
        this.eagerLoadGroups = eagerLoadGroups;
    }

    public BootstrapStep[] steps() {
        return new BootstrapStep[] {
                loadGoogleViz,
                executionMode,
                trackExecutionMode,
                loadCompatMatrix,
                registerSubsystems,
                eagerLoadProfiles,
                hostStoreInit,
                serverStoreInit,
                eagerLoadGroups
        };
    }
}
