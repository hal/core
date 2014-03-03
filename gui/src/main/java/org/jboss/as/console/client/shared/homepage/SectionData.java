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
    private final String id;
    private final String title;
    private final String intro;
    private boolean open;
    private final List<ContentBox> contentBoxes;

    public SectionData(final String id, final String title, final String intro, final boolean open,
            final ContentBox... contentBoxes) {
        this.id = id;
        this.title = title;
        this.intro = intro;
        this.open = open;
        this.contentBoxes = new LinkedList<ContentBox>();
        if (contentBoxes != null) {
            this.contentBoxes.addAll(Arrays.asList(contentBoxes));
        }
    }

    public String getId() {
        return id;
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

    public List<ContentBox> getContentBoxes() {
        return contentBoxes;
    }
}
