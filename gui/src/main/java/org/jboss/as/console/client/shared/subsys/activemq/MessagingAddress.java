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
package org.jboss.as.console.client.shared.subsys.activemq;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;

/**
 * @author Harald Pehl
 */
public interface MessagingAddress {

    String ROOT_ADDRESS = "{selected.profile}/subsystem=messaging-activemq";
    AddressTemplate ROOT_TEMPLATE = AddressTemplate.of(ROOT_ADDRESS);

    String PROVIDER_ADDRESS = ROOT_ADDRESS + "/server=*";
    AddressTemplate PROVIDER_TEMPLATE = AddressTemplate.of(PROVIDER_ADDRESS);

    String PATH_ADDRESS = ROOT_ADDRESS + "/server={activemq.server}/path=bindings-directory";
    AddressTemplate PATH_TEMPLATE = AddressTemplate.of(PATH_ADDRESS);
}
