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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;

import static org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel.PAGE_LIMIT;
import static org.jboss.as.console.client.widgets.tabs.Templates.TEMPLATES;

class Tab extends Composite {

    static final String STYLE_NAME = "hal-TabLayout-tab";

    private final static int MAX_LENGTH = 15;
    private final static String ELLIPSIS = "...";

    private final DefaultTabLayoutPanel tabLayout;
    private int index;
    private final boolean lastTab;
    private final TabLabel label;
    private OffPageSelector selector;

    Tab(final DefaultTabLayoutPanel tabLayout, final String text, final int index, final boolean truncate) {
        this.tabLayout = tabLayout;
        this.index = index;
        this.lastTab = index == PAGE_LIMIT - 1;

        Widget content;
        if (index == 0) {
            // first tab: under no circumstances closable
            label = new TabLabel(Document.get().createDivElement(), text, truncate);
            content = label;

        } else if (lastTab) {
            // last tab: contains close icon (in case closable == true) and hidden selector icon
            if (tabLayout.isCloseable()) {
                FlowPanel panel = new FlowPanel();
                label = new TabLabel(Document.get().createSpanElement(), text, truncate);
                selector = new OffPageSelector(tabLayout);
                selector.setVisible(false); // will be displayed when the first off page is added
                panel.add(label);
                panel.add(new CloseIcon());
                panel.add(selector);
                content = panel;
            } else {
                label = new TabLabel(Document.get().createDivElement(), text, truncate);
                content = label;
            }

        } else {
            // 'middle' tabs contains close icons (if closable == true)
            if (tabLayout.isCloseable()) {
                label = new TabLabel(Document.get().createSpanElement(), text, truncate);
                FlowPanel panel = new FlowPanel();
                panel.add(label);
                CloseIcon closeIcon = new CloseIcon();
                panel.add(closeIcon);
                content = panel;
            } else {
                label = new TabLabel(Document.get().createDivElement(), text, truncate);
                content = label;
            }
        }

        initWidget(content);
        setStyleName(STYLE_NAME);
    }

    void hideSelector() {
        if (lastTab) {
            selector.setVisible(false);
        }
    }

    void showSelector() {
        if (lastTab) {
            selector.setVisible(true);
        }
    }

    void setIndex(int index) {
        this.index = index;
    }

    void setText(final String text) {
        label.setText(text);
    }

    String getText() {
        return label.getText();
    }


    class TabLabel extends Label {

        private final boolean truncate;

        TabLabel(final Element element, final String text, boolean truncate) {
            super(element);
            this.truncate = truncate;

            setText(text);
            sinkEvents(Event.ONKEYDOWN);

            getElement().setTabIndex(0);
            getElement().setAttribute("role", "tab");
        }

        @Override
        public void setText(final String text) {
            if (truncate) {
                super.setText(abbreviateMiddle(text));
                setTitle(text);
            } else {
                super.setText(text);
            }
        }

        @Override
        public String getText() {
            return getTitle();
        }

        @Override
        public void onBrowserEvent(Event event) {
            int type = DOM.eventGetType(event);
            switch (type) {
                case Event.ONKEYDOWN:
                    if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
                        tabLayout.selectTab(index);
                        event.stopPropagation();
                    }
                    break;
                default:
                    break;
            }
        }

        private String abbreviateMiddle(String str) {
            if (str == null || MAX_LENGTH >= str.length()) {
                return str;
            }

            final int targetSting = MAX_LENGTH - ELLIPSIS.length();
            final int startOffset = targetSting / 2 + targetSting % 2;
            final int endOffset = str.length() - targetSting / 2;

            return str.substring(0, startOffset) + ELLIPSIS + str.substring(endOffset);
        }
    }


    class CloseIcon extends InlineHTML {
        CloseIcon() {
            super(TEMPLATES.closeIcon());
            addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    tabLayout.remove(index);
                    event.stopPropagation();
                }
            });
        }
    }
}
