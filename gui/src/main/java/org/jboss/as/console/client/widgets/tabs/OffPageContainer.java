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
package org.jboss.as.console.client.widgets.tabs;

import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import java.util.*;

import static java.util.Map.Entry;

/**
 * Keeps the texts and the widgets in sync
 */
class OffPageContainer implements IsWidget, Iterable<Entry<OffPageText, Widget>> {

    private final DeckPanel deck;
    private final List<OffPageText> texts;

    OffPageContainer() {
        this.deck = new DeckPanel();
        this.texts = new ArrayList<>();
    }

    void add(final OffPageText text, final Widget widget) {
        texts.add(text);
        deck.add(widget);
    }

    void remove(final int index) {
        texts.remove(index);
        deck.remove(index);
        reindex();
    }

    private void reindex() {
        List<OffPageText> reindexed = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            reindexed.add(new OffPageText(i, texts.get(i).getText()));
        }
        texts.clear();
        texts.addAll(reindexed);
    }

    void selectDeck(final int index) {
        deck.showWidget(index);
    }

    int getSelectedDeck() {
        return deck.getVisibleWidget();
    }

    Widget getDeck(int index) {
        return deck.getWidget(index);
    }

    OffPageText getText(int index) {
        return texts.get(index);
    }

    boolean contains(String text) {
        for (OffPageText opt : texts) {
            if (text.equals(opt.getText())) {
                return true;
            }
        }
        return false;
    }

    boolean isEmpty() {
        return texts.isEmpty();
    }

    int size() {
        return texts.size();
    }

    void clear() {
        texts.clear();
        deck.clear();
    }

    OffPageContainer copy() {
        OffPageContainer copy = new OffPageContainer();
        for (Entry<OffPageText, Widget> entry : this) {
            copy.add(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    @Override
    public Widget asWidget() {
        return deck;
    }

    public List<OffPageText> getTexts() {
        return texts;
    }

    @Override
    public Iterator<Entry<OffPageText, Widget>> iterator() {
        Map<OffPageText, Widget> map = new LinkedHashMap<>();
        for (int i = 0; i < deck.getWidgetCount(); i++) {
            map.put(texts.get(i), deck.getWidget(i));
        }
        return map.entrySet().iterator();
    }
}
