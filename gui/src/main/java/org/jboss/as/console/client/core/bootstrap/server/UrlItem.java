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
package org.jboss.as.console.client.core.bootstrap.server;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.InputElement;
import org.jboss.ballroom.client.widgets.forms.InputElementWrapper;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;

/**
 * @author Harald Pehl
 */
public class UrlItem extends FormItem<String> {

    public final static String STYLE_NAME = "hal-urlItem";
    private final static String HTTP = "http";
    private final static String HTTPS = "https";
    private final static String SCHEME_SEPARATOR = "://";
    private final static String PORT_SEPARATOR = ":";

    private final UrlInputWrapper wrapper;
    private final ListBox scheme;
    private final TextBox host;
    private final TextBox port;

    public UrlItem(String name, String title) {
        super(name, title);

        scheme = new ListBox();
        scheme.addStyleName("scheme");
        scheme.addItem(HTTP);
        scheme.addItem(HTTPS);
        host = new TextBox();
        host.addStyleName("host");
        host.getElement().setAttribute("placeholder", "Hostname");
        port = new TextBox();
        port.addStyleName("port");
        port.getElement().setAttribute("placeholder", "Port");
        port.getElement().setAttribute("type", "number");
        port.getElement().setAttribute("min", "0");
        port.getElement().setAttribute("max", "65535");

        FlowPanel panel = new FlowPanel(SpanElement.TAG);
        panel.addStyleName(STYLE_NAME);
        panel.add(scheme.asWidget());
        panel.add(host.asWidget());
        panel.add(port.asWidget());

        wrapper = new UrlInputWrapper(panel, this);
    }

    @Override
    public Widget asWidget() {
        return wrapper;
    }

    @Override
    public void setEnabled(boolean b) {
        scheme.setEnabled(b);
        host.setEnabled(b);
        port.setEnabled(b);
    }

    @Override
    public boolean validate(String value) {
        String[] parts = parse(value);
        boolean validScheme = !isEmpty(parts[0]);
        boolean validHost = !isEmpty(parts[1]);
        boolean validPort = validValidity(port.getElement()) && (isEmpty(parts[2]) || isNumber(parts[2]));

        if (!validScheme) {
            wrapper.errorWidget = scheme;
            setErrMessage("Invalid scheme");
        } else if (!validHost) {
            wrapper.errorWidget = host;
            setErrMessage("Invalid hostname");
        } else if (!validPort) {
            wrapper.errorWidget = port;
            setErrMessage("Invalid port");
        }
        return validScheme && validHost && validPort;
    }

    @Override
    public void setErroneous(boolean b) {
        super.setErroneous(b);
        wrapper.setErroneous(b);
    }

    @Override
    public void clearValue() {
        scheme.setSelectedIndex(0);
        host.setValue("");
        port.setValue("");
    }

    @Override
    public String getValue() {
        StringBuilder builder = new StringBuilder();
        builder.append(scheme.getSelectedValue()).append(SCHEME_SEPARATOR).append(host.getValue());
        if (!isEmpty(port.getText())) {
            builder.append(PORT_SEPARATOR).append(port.getValue());
        }
        return builder.toString();
    }

    @Override
    public void setValue(String value) {
        if (isEmpty(value)) {
            clearValue();
        }

        String[] parts = parse(value);
        if (HTTP.equals(parts[0])) {
            scheme.setSelectedIndex(0);
        } else if (HTTPS.equals(parts[1])) {
            scheme.setSelectedIndex(1);
        }
        host.setValue(parts[1]);
        if (isNumber(parts[2])) {
            port.setValue(parts[2]);
        }
    }

    private String[] parse(String value) {
        String[] parts = new String[3];
        if (!isEmpty(value)) {
            String[] sh = value.split(SCHEME_SEPARATOR);
            if (sh.length != 0) {
                parts[0] = sh[0];
            }
            if (sh.length > 1) {
                String reminder = sh[1];
                String[] hp = reminder.split(PORT_SEPARATOR);
                if (hp.length != 0) {
                    parts[1] = hp[0];
                }
                if (hp.length > 1) {
                    parts[2] = hp[1];
                }
            }
        }
        return parts;
    }

    private boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    private boolean isNumber(String value) {
        try {
            //noinspection ResultOfMethodCallIgnored
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private native boolean validValidity(Element element) /*-{
        return element != null && element.validity != null && element.validity.valid;
    }-*/;


    class UrlInputWrapper extends InputElementWrapper {

        Widget widget;
        Widget errorWidget;

        public UrlInputWrapper(Widget widget, InputElement input) {
            super(widget, input);
            this.widget = widget;
        }

        @Override
        public void setErroneous(boolean hasErrors) {
            super.setErroneous(hasErrors);
            widget.removeStyleName("form-item-error");
            if (errorWidget != null) {
                if (hasErrors) {
                    errorWidget.addStyleName("form-item-error");
                } else {
                    errorWidget.removeStyleName("form-item-error");
                }
            }
        }
    }
}
