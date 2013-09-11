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
package org.jboss.as.console.client.administration.role;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.ballroom.client.widgets.forms.InputElement;

/**
* @author Harald Pehl
*/
class FormItemPanelWrapper extends VerticalPanel {

    private final HTML errorText;
    private final Widget widget;

    public FormItemPanelWrapper(Widget widget, final InputElement input) {
        this.widget = widget;

        setStyleName("fill-layout-width");
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(widget);
        widget.getElement().getParentElement().setAttribute("class", "form-input");

        errorText = new HTML(input.getErrMessage());
        errorText.addStyleName("form-item-error-desc");
        DOM.setStyleAttribute(errorText.getElement(), "marginTop", "1em");

        add(panel);
        add(errorText);
        errorText.setVisible(false);
    }

    public void setErroneous(boolean hasErrors) {
        if (hasErrors) { widget.addStyleName("form-item-error"); } else {
            widget.removeStyleName("form-item-error");
        }
        errorText.setVisible(hasErrors);
    }
}
