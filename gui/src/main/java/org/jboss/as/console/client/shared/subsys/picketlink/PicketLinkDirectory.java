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
package org.jboss.as.console.client.shared.subsys.picketlink;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;

/**
 * @author Harald Pehl
 */
public interface PicketLinkDirectory {

    // ------------------------------------------------------ addresses & templates

    String ROOT_ADDRESS = "{selected.profile}/subsystem=picketlink-federation";
    String FEDERATION_ADDRESS = ROOT_ADDRESS + "/federation=*";

    String IDENTITY_PROVIDER_ADDRESS = FEDERATION_ADDRESS + "/identity-provider=*";
    String IDENTITY_PROVIDER_HANDLER_ADDRESS = IDENTITY_PROVIDER_ADDRESS + "/handler=*";
    String IDENTITY_PROVIDER_TRUST_DOMAIN_ADDRESS = IDENTITY_PROVIDER_ADDRESS + "/trust-domain=*";

    String KEY_STORE_ADDRESS = FEDERATION_ADDRESS + "/key-store=key-store";
    String SAML_ADDRESS = FEDERATION_ADDRESS + "/saml=saml";

    String SERVICE_PROVIDER_ADDRESS = FEDERATION_ADDRESS + "/service-provider=*";
    String SERVICE_PROVIDER_HANDLER_ADDRESS = SERVICE_PROVIDER_ADDRESS + "/handler=*";

    AddressTemplate ROOT_TEMPLATE = AddressTemplate.of(ROOT_ADDRESS);
    AddressTemplate FEDERATION_TEMPLATE = AddressTemplate.of(FEDERATION_ADDRESS);

    AddressTemplate IDENTITY_PROVIDER_TEMPLATE = AddressTemplate.of(IDENTITY_PROVIDER_ADDRESS);
    AddressTemplate IDENTITY_PROVIDER_HANDLER_TEMPLATE = AddressTemplate.of(IDENTITY_PROVIDER_HANDLER_ADDRESS);
    AddressTemplate IDENTITY_PROVIDER_TRUST_DOMAIN_TEMPLATE = AddressTemplate.of(IDENTITY_PROVIDER_TRUST_DOMAIN_ADDRESS);

    AddressTemplate KEY_STORE_TEMPLATE = AddressTemplate.of(KEY_STORE_ADDRESS);
    AddressTemplate SAML_TEMPLATE = AddressTemplate.of(SAML_ADDRESS);

    AddressTemplate SERVICE_PROVIDER_TEMPLATE = AddressTemplate.of(SERVICE_PROVIDER_ADDRESS);
    AddressTemplate SERVICE_PROVIDER_HANDLER_TEMPLATE = AddressTemplate.of(SERVICE_PROVIDER_HANDLER_ADDRESS);


    // ------------------------------------------------------ place request parameters

    String FEDERATION_REQUEST_PARAM = "federation";
    String SERVICE_PROVIDER_REQUEST_PARAM = "service-provicer";
}
