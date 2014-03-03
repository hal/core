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
package org.jboss.as.console.client.shared.homepage;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Harald Pehl
 */
public class SectionData {
    private final String title;
    private final String intro;
    private boolean open;
    private final List<String> contentBoxIds;

    public SectionData(final String title, final String intro, final boolean open, final String... contentBoxIds) {
        this.title = title;
        this.intro = intro;
        this.open = open;
        this.contentBoxIds = new LinkedList<String>();
        if (contentBoxIds != null) {
            this.contentBoxIds.addAll(Arrays.asList(contentBoxIds));
        }
    }

    public String getTitle() {
        return title;
    }

    public String getIntro() {
        return intro;
    }

    public boolean isOpen() {
        return open;
    }

    public List<String> getContentBoxIds() {
        return contentBoxIds;
    }
}
