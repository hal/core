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

import java.util.Iterator;
import java.util.Stack;

class Tabs implements Iterable<Tab> {

    private final DefaultTabLayoutPanel tabLayout;
    private final Stack<Tab> tabs;

    Tabs(DefaultTabLayoutPanel tabLayout) {
        this.tabLayout = tabLayout;
        this.tabs = new Stack<>();
    }

    Tab add(String text, int index) {
        Tab t = new Tab(tabLayout, text, index);
        tabs.push(t);
        return t;
    }

    void remove(int index) {
        if (index < 0 || index > tabs.size() - 1) {
            return;
        }
        tabs.remove(index);
        reindex();
    }

    private void reindex() {
        int index = 0;
        for (Tab tab : tabs) {
            tab.setIndex(index);
            index++;
        }
    }

    void hideSelector() {
        if (!tabs.isEmpty()) {
            tabs.peek().hideSelector();
        }
    }

    void showSelector() {
        if (!tabs.isEmpty()) {
            tabs.peek().showSelector();
        }
    }

    Tab lastTab() {
        return tabs.peek();
    }

    public Tab get(int index) {
        return tabs.get(index);
    }

    public int size() {
        return tabs.size();
    }

    @Override
    public Iterator<Tab> iterator() {
        return tabs.iterator();
    }
}
