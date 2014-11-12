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

import com.google.gwt.user.client.ui.SuggestOracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An suggest oracle which uses the {@link org.jboss.as.console.client.search.Index} for lookup.
 * @author Harald Pehl
 */
public class IndexSuggestOracle extends SuggestOracle {

    private final Index index;

    public IndexSuggestOracle(final Index index) {this.index = index;}

    @Override
    public void requestSuggestions(final Request request, final Callback callback) {
        String query = request.getQuery().trim();
        if (query.length() != 0) {
            List<Document> hits = index.search(query);
            Map<String, List<Document>> keywords = extractAndFilterKeywords(query, hits);
            List<KeywordSuggestion> suggestions = new ArrayList<KeywordSuggestion>();
            for (Map.Entry<String, List<Document>> entry : keywords.entrySet()) {
                KeywordSuggestion suggestion = new KeywordSuggestion(entry.getValue(), entry.getKey(), entry.getKey());
                suggestions.add(suggestion);
            }
//            for (Document hit : hits) {
//                String description = hit.getDescription();
//                boolean tooLong = description.length() > 125;
//                String shortDesc = tooLong ? description.substring(0, 125) + "..." : description;
//                String display = tooLong ? "<span title=\"" + description + "\">" + shortDesc + "</span>" : description;
//                KeywordSuggestion suggestion = new KeywordSuggestion(hit, description,
//                        display + " <span class=\"hit-token\">(#" + hit.getToken() + ")</span>");
//                suggestions.add(suggestion);
//            }
            callback.onSuggestionsReady(request, new Response(suggestions));
        }
    }

    @Override
    public boolean isDisplayStringHTML() {
        return true;
    }

    private Map<String, List<Document>> extractAndFilterKeywords(String query, List<Document> hits) {
        Map<String, List<Document>> keywords = new HashMap<>();
        for (Document document : hits) {
            for (String keyword : document.getKeywords()) {

                if (keyword.contains(query)/* || document.getDescription().contains(query)*/) {
                    List<Document> documents = keywords.get(keyword);
                    if (documents == null) {
                        documents = new ArrayList<>();
                        keywords.put(keyword, documents);
                    }
                    documents.add(document);
                }
            }
            // TODO Take ordering of 'hits' into account
        }
        return keywords;
    }
}
