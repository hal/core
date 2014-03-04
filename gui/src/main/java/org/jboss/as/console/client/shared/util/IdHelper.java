/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.shared.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.DOM;

/**
 * Helper to generate somewhat unique IDs based on the class name to be used on DOM elements.
 *
 * @author Harald Pehl
 */
public final class IdHelper {

    public static <T> String asId(Class<T> clazz) {
        return asId(null, clazz, null);
    }

    public static <T> String asId(Class<T> clazz, String suffix) {
        return asId(null, clazz, suffix);
    }

    public static <T> String asId(String prefix, Class<T> clazz) {
        return asId(prefix, clazz, null);
    }

    public static <T> String asId(String prefix, Class<T> clazz, String suffix) {
        String id;
        if (clazz == null) {
            id = DOM.createUniqueId();
            Log.error("Cannot create stable ID: No class specified! Will return generated ID: " + id);
            return id;
        } else {
            id = clazz.getName();
            int lastDot = id.lastIndexOf('.');
            if (lastDot != -1 && lastDot != id.length() - 1) {
                id = id.substring(lastDot + 1);
            }
            id = id.replace('$', '_');
        }
        id = prefix != null ? prefix + id : id;
        return suffix != null ? id + suffix : id;
    }

    private IdHelper() {}
}
