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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;

/**
 * @author Harald Pehl
 */
public class Index {

    private final static Index instance = new Index();

    public static Index get() {
        return instance;
    }

    private long idCounter = 0;
    private final Map<Long, Document> idCache;

    private Index() {
        idCache = new HashMap<Long, Document>();
        reset();
    }

    /**
     * Resets the index
     */
    public void reset() {
        idCounter = 0;
        idCache.clear();
        setupIndex();
    }

    private native void setupIndex()  /*-{
        var hal_idx = $wnd.lunr(function () {
            this.field('token')
            this.field('desc')
        })
    }-*/;

    public void add(final String token, final String description) {
        long id = nextId();
        idCache.put(id, new Document(id, token, description));
        addInternal(String.valueOf(id), token, description);
    }

    private long nextId() {
        idCounter++;
        return idCounter;
    }

    private native void addInternal(final String id, final String idToken, final String description) /*-{
        $wnd.hal_idx.add({
            id: id,
            token: token,
            desc: description
        })
    }-*/;

    public List<Document> search(final String text) {
        ArrayList<Document> results = new ArrayList<Document>();
        JsArray jsonResult = searchInternal(text);
        for (int i = 0; i < jsonResult.length(); i++) {
            JSONObject json = new JSONObject(jsonResult.get(i));
            long id = (long) json.get("ref").isNumber().doubleValue();
            Document document = idCache.get(id);
            if (document != null) {
                results.add(document);
            }
        }
        return results;
    }

    private native JsArray searchInternal(final String text) /*-{
        return $wnd.hal_idx.search(text);
    }-*/;


    public static class Document {
        private final long id;
        private final String token;
        private final String description;

        public Document(final long id, final String token, final String description) {
            this.id = id;
            this.token = token;
            this.description = description;
        }

        public long getId() {
            return id;
        }

        public String getToken() {
            return token;
        }

        public String getDescription() {
            return description;
        }
    }
}
