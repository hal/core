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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.bootstrap.GlobalShortcuts;

/**
 * The global search tool. Some kind of 'presenter' which takes care of both on demand indexing and searching.
 *
 * @author Harald Pehl
 */
public class SearchTool extends Composite {

    private final Index index;
    private final HTML root;
    private final SearchPopup popup;

    public SearchTool(final Harvest harvest, final Index index, PlaceManager placeManager) {
        this.index = index;
        this.root = new HTML("<i class=\"icon-search\"></i>");
        if (Window.Navigator.getPlatform().toLowerCase().contains("mac")) {
            this.root.setTitle(Console.CONSTANTS.search_tooltip_osx());
        } else {
            this.root.setTitle(Console.CONSTANTS.search_tooltip_other());
        }
        this.root.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                showPopup();
            }
        });
        this.popup = new SearchPopup(harvest, index, placeManager);

        GlobalShortcuts.bind("mod+.", new Command() {
            @Override
            public void execute() {
                showPopup();
            }
        });

        initWidget(root);
        setStyleName("hal-searchTool");
    }

    public void showPopup() {
        int width = 300;
        int offset = 28;

        popup.setPopupPosition(root.getAbsoluteLeft() - width + root.getOffsetWidth() + offset,
                root.getAbsoluteTop() + 25);
        popup.setWidth(width + "px");

        if (index.isEmpty()) {
            popup.index();
        } else {
            popup.showSearchPage();
        }
    }
}
