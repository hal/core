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
package org.jboss.as.console.client.shared.patching.ui;

import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.PatchManagerElementId;

/**
 * @author Harald Pehl
 */
public class RestartModal extends PopupPanel implements PatchManagerElementId {

    private final DeckLayoutPanel deck;
    private int width, height;

    public RestartModal() {
        super(false, true);

        getElement().setId(asId(PREFIX, getClass()));
        setStyleName("default-window");
        deck = new DeckLayoutPanel();
        setWidget(deck);

        FlowPanel holder = new FlowPanel();
        holder.addStyleName("restart-window-content");
        Pending pending = new Pending(Console.CONSTANTS.patch_manager_restart_pending());
        holder.add(pending);
        deck.add(holder);

        holder = new FlowPanel();
        holder.addStyleName("restart-window-content");
        Label timeout = new Label(Console.CONSTANTS.patch_manager_restart_timeout());
        timeout.addStyleName("restart-error");
        holder.add(timeout);
        deck.add(holder);

        holder = new FlowPanel();
        holder.addStyleName("restart-window-content");
        Label error = new Label(Console.CONSTANTS.patch_manager_restart_error());
        error.addStyleName("restart-error");
        holder.add(error);
        deck.add(holder);

        setWidth(300);
        setHeight(150);
        setGlassEnabled(true);
    }

    public void timeout() {
        deck.showWidget(1);
    }

    public void error() {
        deck.showWidget(2);
    }

    @Override
    public void center() {
        deck.showWidget(0);
        setPopupPosition((Window.getClientWidth() / 2) - (width / 2),
                (Window.getClientHeight() / 2) - (height / 2) - 50);
        show();

        super.setWidth(width + "px");
        super.setHeight(height + "px");
    }

    public void setWidth(int width) {
        this.width = Double.valueOf(width * 1.2).intValue();
    }

    public void setHeight(int height) {
        this.height = Double.valueOf(height * 1.2).intValue();
    }
}
