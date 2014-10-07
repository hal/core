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

package org.jboss.as.console.client.shared.subsys.jca.model;

import org.jboss.as.console.client.core.ApplicationProperties;

import javax.inject.Inject;

/**
 * @author Heiko Braun
 */
public class DriverRegistry {

    private DriverStrategy chosenStrategy;

    @Inject
    public DriverRegistry(
            ApplicationProperties bootstrap,
            DomainDriverStrategy domainStrategy,
            StandaloneDriverStrategy standaloneStrategy,
            ModulesDriverStrategy modulesStrategy) {
//        this.chosenStrategy = bootstrap.isStandalone() ? standaloneStrategy : domainStrategy;
        // HAL-483: ":installed-drivers-list" used in DomainDriverStrategy
        // does not scale for big domains. In order to have consistent behaviour,
        // ModulesDriverStrategy is used for both standalone and domain
        this.chosenStrategy = modulesStrategy;
    }

    public DriverStrategy create() {
        return chosenStrategy;
    }
}
