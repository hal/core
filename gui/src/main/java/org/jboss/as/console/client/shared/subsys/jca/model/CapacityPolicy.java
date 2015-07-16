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
package org.jboss.as.console.client.shared.subsys.jca.model;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

/**
 * @author Harald Pehl
 */
public enum CapacityPolicy {

    // ------------------------------------------------------ incrementer

    MAX_POOL_SIZE_INCREMENTER(
            "org.jboss.jca.core.connectionmanager.pool.capacity.MaxPoolSizeIncrementer",
            true,
            Collections.<String, String>emptyMap()),

    SIZE_INCREMENTER(
            "org.jboss.jca.core.connectionmanager.pool.capacity.SizeIncrementer",
            true,
            ImmutableMap.of("Size", "1")),

    WATERMARK_INCREMENTER(
            "org.jboss.jca.core.connectionmanager.pool.capacity.WatermarkIncrementer",
            true,
            ImmutableMap.of("Watermark", "1")),

    // ------------------------------------------------------ decrementer

    MIN_POOL_SIZE_DECREMENTER(
            "org.jboss.jca.core.connectionmanager.pool.capacity.MinPoolSizeDecrementer",
            false,
            Collections.<String, String>emptyMap()),

    SIZE_DECREMENTER("org.jboss.jca.core.connectionmanager.pool.capacity.SizeDecrementer",
            false,
            ImmutableMap.of("Size", "1")),

    TIMED_OUT_DECREMENTER("org.jboss.jca.core.connectionmanager.pool.capacity.TimedOutDecrementer",
            false,
            Collections.<String, String>emptyMap()),

    TIMED_OUT_FIFO_DECREMENTER("org.jboss.jca.core.connectionmanager.pool.capacity.TimedOutFIFODecrementer",
            false,
            Collections.<String, String>emptyMap()),

    WATERMARK_DECREMENTER("org.jboss.jca.core.connectionmanager.pool.capacity.WatermarkDecrementer",
            false,
            ImmutableMap.of("Watermark", "1"));


    // ------------------------------------------------------ lookup

    public static CapacityPolicy lookup(String className) {
        for (CapacityPolicy cp : CapacityPolicy.values()) {
            if (cp.className().equals(className)) {
                return cp;
            }
        }
        return null;
    }


    // ------------------------------------------------------ instance data

    private final String className;
    private final Map<String, String> properties;
    private final boolean increment;

    CapacityPolicy(final String className, final boolean increment, final Map<String, String> properties) {
        this.className = className;
        this.increment = increment;
        this.properties = properties;
    }

    public String className() {
        return className;
    }

    public boolean isIncrement() {
        return increment;
    }

    public Map<String, String> properties() {
        return properties;
    }

    @Override
    public String toString() {
        return className() + properties;
    }
}
