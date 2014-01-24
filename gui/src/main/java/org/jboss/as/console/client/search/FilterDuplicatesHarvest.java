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

import java.util.HashSet;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;

/**
* @author Harald Pehl
*/
class FilterDuplicatesHarvest implements Harvest.Handler {

    private final Index index;
    private final SearchPopup searchPopup;
    private final Set<Entry> entries;

    FilterDuplicatesHarvest(final Index index, final SearchPopup searchPopup) {
        this.index = index;
        this.searchPopup = searchPopup;
        this.entries = new HashSet<Entry>();
    }

    @Override
    public void onStart() {
        entries.clear();
        searchPopup.showIndexPage();
    }

    @Override
    public boolean shouldHarvest(final String token, final String address, final String description) {
        return !entries.contains(new Entry(token, description));
    }

    @Override
    public void onHarvest(String token, String address, String description) {
        entries.add(new Entry(token, description));
    }

    @Override
    public void onFinish() {
        index.save();
        searchPopup.showSearchPage();
    }

    @Override
    public void onError(Throwable t) {
        // Don't show an error message as this would overlay with the search popup.
        Log.error("Failed to index resource: " + t.getMessage());
    }

    private static class Entry {
        final String token;
        final String description;

        private Entry(final String token, final String description) {
            this.token = token;
            this.description = description;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (!(o instanceof Entry)) { return false; }

            Entry entry = (Entry) o;

            return description.equals(entry.description) && token.equals(entry.token);

        }

        @Override
        public int hashCode() {
            int result = token.hashCode();
            result = 31 * result + description.hashCode();
            return result;
        }
    }
}
