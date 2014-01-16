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
package org.jboss.as.console.client.search;

import com.google.gwt.json.client.JSONArray;

/**
 * @author Harald Pehl
 */
public class Index {

    private static long idCounter = 0;
    private final static Index instance = new Index();

    private Index() {
        reset();
    }

    public static Index get() {
        return instance;
    }

    private static long nextId() {
        idCounter++;
        return idCounter;
    }

    /**
     * Resets the index
     */
    public void reset() {
        idCounter = 0;
        setupIndex();
    }

    private native void setupIndex()  /*-{
        var hal_idx = lunr(function () {
            this.field('desc')
        })
    }-*/;

    public void add(String token, String description) {
//        addInternal(String.valueOf(nextId()) + "#", token, description);
    }


    private native void addInternal(final String idToekn, final String description)  /*-{
        index.add({
            id: 1,
            title: 'Foo',
            body: 'Foo foo foo!'
        })
    }-*/;

    public JSONArray search(String text) {

        return null;
    }
}
