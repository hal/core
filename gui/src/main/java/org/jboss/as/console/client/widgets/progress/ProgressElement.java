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
package org.jboss.as.console.client.widgets.progress;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.gwt.flow.client.Progress;

/**
 * @author Harald Pehl
 */
public class ProgressElement extends Widget implements Progress {

    public final static String CSS_CLASS_NAME = "hal-ProgressElement";

    private int value;
    private int max;
    private boolean determinate;

    public ProgressElement() {
        this.value = 0;
        this.max = 100;
        this.determinate = true;

        setElement(DOM.createElement("progress"));
        getElement().setId(Document.get().createUniqueId());
        setStyleName(CSS_CLASS_NAME);
        setVisible(false);
    }

    @Override
    public void reset() {
        reset(0);
    }

    @Override
    public void reset(final int mx) {
        value = 0;
        max = mx;
        determinate = max > 1; // if there's just one step, choose none-determinate state

        if (determinate) {
            getElement().setAttribute("max", String.valueOf(max));
            getElement().setAttribute("value", String.valueOf(value));
        } else {
            getElement().setAttribute("max", "1.0"); // default
            getElement().removeAttribute("value");
        }
        setVisible(true);
    }

    @Override
    public void tick() {
        if (determinate) {
            if (value < max) {
                value++;
                getElement().setAttribute("value", String.valueOf(value));
            }
        }
    }

    @Override
    public void finish() {
        setVisible(false);
    }
}
