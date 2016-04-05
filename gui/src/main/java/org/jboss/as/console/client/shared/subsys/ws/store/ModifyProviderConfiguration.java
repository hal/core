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
package org.jboss.as.console.client.shared.subsys.ws.store;

import java.util.Map;

import org.jboss.gwt.circuit.Action;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 * @date 3/31/2016
 */
public class ModifyProviderConfiguration implements Action {

    private final Map<String, Object> changedValues;

    public ModifyProviderConfiguration(Map<String, Object> changedValues) {
        this.changedValues = changedValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModifyProviderConfiguration)) return false;

        ModifyProviderConfiguration that = (ModifyProviderConfiguration) o;

        return changedValues.equals(that.changedValues);

    }

    @Override
    public int hashCode() {
        return changedValues.hashCode();
    }

    public Map<String, Object> getChangedValues() {
        return changedValues;
    }
}
