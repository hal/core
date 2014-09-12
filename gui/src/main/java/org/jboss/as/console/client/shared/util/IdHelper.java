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
package org.jboss.as.console.client.shared.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;

import static org.jboss.as.console.client.shared.util.IdHelper.WidgetType.*;

/**
 * Helper to generate somewhat unique IDs based on the class name to be used on DOM elements.
 *
 * @author Harald Pehl
 */
public final class IdHelper {

    public enum WidgetType {
        LABEL("label"),
        TEXT_BOX("text"),
        RADIO_BUTTON("radio"),
        CHECK_BOX("check"),
        BUTTON("button"),
        UNKNOWN("any");

        private final String type;

        WidgetType(String type) {
            this.type = type;
        }
    }

    public static void setId(Widget widget, String prefix, String id) {
        if (widget instanceof LabelBase) {
            setId(widget.getElement(), LABEL, prefix, id);
        } else if (widget instanceof ValueBoxBase) {
            setId(widget.getElement(), TEXT_BOX, prefix, id);
        } else if (widget instanceof RadioButton) { // must come *before* CheckBox!
            setId(widget.getElement(), RADIO_BUTTON, prefix, id);
        } else if (widget instanceof CheckBox) {
            setId(widget.getElement(), CHECK_BOX, prefix, id);
        } else if (widget instanceof Button) {
            setId(widget.getElement(), BUTTON, prefix, id);
        } else {
            setId(widget.getElement(), UNKNOWN, prefix, id);
        }
    }

    public static void setId(Element element, WidgetType widgetType, String prefix, String id) {
        element.setId(widgetType.type + "_" + prefix.toLowerCase() + "_" + id.toLowerCase());
    }

    public static <T> String asId(Class<T> clazz) {
        return asId(null, clazz, null);
    }

    public static <T> String asId(Class<T> clazz, String suffix) {
        return asId(null, clazz, suffix);
    }

    public static <T> String asId(String prefix, Class<T> clazz) {
        return asId(prefix, clazz, null);
    }

    public static <T> String asId(String prefix, Class<T> clazz, String suffix) {
        String id;
        if (clazz == null) {
            id = DOM.createUniqueId();
            Log.error("Cannot create stable ID: No class specified! Will return generated ID: " + id);
            return id;
        } else {
            id = clazz.getName();
            int lastDot = id.lastIndexOf('.');
            if (lastDot != -1 && lastDot != id.length() - 1) {
                id = id.substring(lastDot + 1);
            }
            id = id.replace('$', '_');
        }
        id = prefix != null ? prefix + id : id;
        return suffix != null ? id + suffix : id;
    }

    private IdHelper() {}
}
