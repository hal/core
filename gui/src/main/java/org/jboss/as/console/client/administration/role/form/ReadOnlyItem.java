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
package org.jboss.as.console.client.administration.role.form;

import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.ballroom.client.widgets.forms.FormItem;

/**
 * @author Harald Pehl
 */
public class ReadOnlyItem<T> extends FormItem<T> {

    private T value;
    private final InlineLabel label;

    public ReadOnlyItem(final String name, final String title) {
        super(name, title);
        this.label = new InlineLabel("");
    }

    @Override
    public Widget asWidget() {
        return label;
    }

    @Override
    public void setEnabled(final boolean b) {

    }

    @Override
    public boolean validate(final T value) {
        return true;
    }

    @Override
    public void clearValue() {
        value = null;
        label.setText("");
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setValue(final T value) {
        this.value = value;
        label.setText(asString());
    }
}
