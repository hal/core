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
package org.jboss.as.console.client.shared.homepage.content;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry for {@link ContentBox} instances.
 *
 * @author Harald Pehl
 */
public class ContentBoxRegistry {

    private final Map<String, ContentBox> registry;

    public ContentBoxRegistry() {registry = new HashMap<String, ContentBox>();}

    public void register(ContentBox contentBox) throws IllegalAccessException {
        if (contentBox != null) {
            if (registry.containsKey(contentBox.getId())) {
                throw new IllegalAccessException("Content box #" + contentBox.getId() + " already registered!");
            }
            registry.put(contentBox.getId(), contentBox);
        }
    }

    public ContentBox get(String id) {
        return registry.get(id);
    }
}
