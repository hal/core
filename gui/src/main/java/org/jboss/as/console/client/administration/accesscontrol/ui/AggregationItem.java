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
package org.jboss.as.console.client.administration.accesscontrol.ui;

import com.google.common.base.Supplier;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignment;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class AggregationItem {
    private final boolean include;
    private final Supplier<List<Assignment>> supplier;

    public AggregationItem(final boolean include, final Supplier<List<Assignment>> supplier) {
        this.include = include;
        this.supplier = supplier;
    }

    public boolean isInclude() {
        return include;
    }

    public String getTitle() {
        return include ? Console.CONSTANTS.common_label_include() : Console.CONSTANTS.common_label_exclude();
    }

    public List<Assignment> getEntries() {
        return supplier.get();
    }
}
