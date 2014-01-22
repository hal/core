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
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.storage.client.Storage;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import org.jboss.as.console.client.shared.BeanFactory;

/**
 * An index which builds on <a href="http://lunrjs.com/">http://lunrjs.com/</a>. The index and the related raw
 * documents are persisted in the local storage.
 *
 * @author Harald Pehl
 */
public class Index {

    private final String prefix;
    private final BeanFactory beanFactory;
    private final Storage localStorage;
    private long idCounter;
    @SuppressWarnings("UnusedDeclaration") private JavaScriptObject indexRef;

    Index(final String prefix, final BeanFactory beanFactory) {
        this.prefix = prefix;
        this.beanFactory = beanFactory;
        this.localStorage = Storage.getLocalStorageIfSupported();
        this.idCounter = 0;

        load();
    }

    public boolean isEmpty() {
        return idCounter == 0;
    }

    public void save() {
        localStorage.setItem(documentsKey(), String.valueOf(idCounter));
        saveInternal(indexKey());
        Log.info("Saved " + idCounter + " documents to index at " + indexKey());
    }

    private native void saveInternal(String indexKey)  /*-{
        var jsonIndex = JSON.stringify(this.@org.jboss.as.console.client.search.Index::indexRef);
        localStorage.setItem(indexKey, jsonIndex)
    }-*/;

    public void load() {
        String item = localStorage.getItem(documentsKey());
        if (item != null) {
            idCounter = Long.parseLong(item);
        }
        try {
            loadInternal(indexKey());
            if (indexRef == null) {
                // no index found in local storage
                reset();
            } else {
                Log.info("Loaded " + idCounter + " documents from index at " + indexKey());
            }
        } catch (JavaScriptException e) {
            // load must be fail safe, so ignore any errors and reset
            reset();
        }
    }

    private native void loadInternal(String indexKey)  /*-{
        var jsonIndex = localStorage.getItem(indexKey);
        if (jsonIndex != null) {
            this.@org.jboss.as.console.client.search.Index::indexRef = $wnd.lunr.Index.load(JSON.parse(jsonIndex));
        }
    }-*/;

    /**
     * Resets the index
     */
    public void reset() {
        for (long i = 0; i < idCounter; i++) {
            localStorage.removeItem(key(i));
        }
        localStorage.removeItem(indexKey());
        localStorage.removeItem(documentsKey());

        this.idCounter = 0;
        resetInternal();
        Log.info("Reset index to " + indexKey());
    }

    private native void resetInternal()  /*-{
        this.@org.jboss.as.console.client.search.Index::indexRef = $wnd.lunr(function () {
            this.field('token');
            this.field('desc');
        })
    }-*/;

    public void add(final String token, final String description) {
        long id = idCounter++;

        Document document = beanFactory.indexDocument().as();
        document.setId(id);
        document.setToken(token);
        document.setDescription(description);
        AutoBean<Document> autoBean = AutoBeanUtils.getAutoBean(document);
        String json = AutoBeanCodex.encode(autoBean).getPayload();
        localStorage.setItem(key(id), json);

        addInternal(String.valueOf(id), token, description);

    }

    private native void addInternal(final String id, final String token, final String description) /*-{
        this.@org.jboss.as.console.client.search.Index::indexRef.add({
            id: id,
            token: token,
            desc: description
        })
    }-*/;

    public List<Document> search(final String text) {
        List<Document> results = new ArrayList<Document>();
        JsArray jsonResult = searchInternal(text);
        if (jsonResult != null) {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject json = new JSONObject(jsonResult.get(i));
                JSONString jsonId = json.get("ref").isString();
                if (jsonId != null) {
                    long id = Long.parseLong(jsonId.stringValue());
                    String documentJson = localStorage.getItem(key(id));
                    if (documentJson != null) {
                        AutoBean<Document> autoBean = AutoBeanCodex.decode(beanFactory, Document.class, documentJson);
                        results.add(autoBean.as());
                    }
                }
            }
        }
        return results;
    }

    private native JsArray searchInternal(final String text) /*-{
        return this.@org.jboss.as.console.client.search.Index::indexRef.search(text);
    }-*/;

    private String indexKey() {return prefix + "idx";}

    private String documentsKey() {return prefix + "docs";}

    private String key(final long id) {return prefix + "doc_" + id;}

    @Override
    public String toString() {
        return indexKey() + "[" + idCounter + "]";
    }
}
