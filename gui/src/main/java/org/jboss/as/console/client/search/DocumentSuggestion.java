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

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;

/**
 * A suggestion in {@link org.jboss.as.console.client.search.IndexSuggestOracle}.
 *
 * @author Harald Pehl
 */
public class DocumentSuggestion extends MultiWordSuggestOracle.MultiWordSuggestion {

    private final Document document;

    public DocumentSuggestion(final Document document, String replacementString, String displayString) {
        super(replacementString, displayString);
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }
}
