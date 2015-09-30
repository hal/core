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

import org.jboss.as.console.client.rbac.Constraints;
import org.jboss.as.console.client.rbac.SecurityContextImpl;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Harald Pehl
 * @author Heiko Braun
 */
public class RequiredResourcesContext {

    private Throwable error;
    private final String token;

    private Map<AddressTemplate, ResourceDescription> descriptions = new HashMap<>();
    private Map<AddressTemplate, Constraints> parentConstraints = new HashMap<>();
    private Map<AddressTemplate, Map<String,Constraints>> childConstraints = new HashMap<>();

    public RequiredResourcesContext(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setError(final Throwable error) {
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }


    public void addDescriptionResult(AddressTemplate addressTemplate, ResourceDescription resourceDescription) {
        descriptions.put(addressTemplate, resourceDescription);
    }

    public void addConstraintResult(AddressTemplate addressTemplate, Constraints constraints) {
        parentConstraints.put(addressTemplate, constraints);
    }

    public void addConstraintResult(AddressTemplate addressTemplate, String resolvedKey, Constraints constraints) {
        if(!childConstraints.containsKey(addressTemplate))
            childConstraints.put(addressTemplate, new HashMap<>());
        childConstraints.get(addressTemplate).put(resolvedKey, constraints);
    }

    public Map<AddressTemplate, ResourceDescription> getDescriptions() {
        return descriptions;
    }

    public Map<AddressTemplate, Constraints> getParentConstraints() {
        return parentConstraints;
    }

    public Map<AddressTemplate, Map<String, Constraints>> getChildConstraints() {
        return childConstraints;
    }

    public void mergeWith(SecurityContextImpl ctx) {
        Map<AddressTemplate, Constraints> parentConstraints = getParentConstraints();
        for (AddressTemplate addressTemplate : parentConstraints.keySet()) {
            ctx.addConstraints(addressTemplate, parentConstraints.get(addressTemplate));
        }

        Map<AddressTemplate, Map<String, Constraints>> childConstraints = getChildConstraints();
        for (AddressTemplate addressTemplate : childConstraints.keySet()) {
            Map<String, Constraints> resolved = childConstraints.get(addressTemplate);
            for (String key : resolved.keySet()) {
                ctx.addChildContext(addressTemplate, key, resolved.get(key));
            }
        }
    }
}
