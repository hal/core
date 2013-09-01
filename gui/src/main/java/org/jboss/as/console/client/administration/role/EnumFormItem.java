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

import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.InputElementWrapper;

/**
 * @author Harald Pehl
 */
public class EnumFormItem<E extends Enum<E>> extends FormItem<E> {

    private final ListBox listBox;
    private final InputElementWrapper wrapper;
    private boolean defaultToFirst;
    private EnumSet<E> values;
    private Map<E, String> nameValuePairs;

    public EnumFormItem(final String name, final String title) {
        super(name, title);
        listBox = new ListBox();
        listBox.setTabIndex(0);
        listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                setModified(true);
                setUndefined("".equals(getSelectedValue()));

            }
        });
        wrapper = new InputElementWrapper(listBox.asWidget(), this);
        wrapper.getElement().setAttribute("style", "width:100%");
    }

    @Override
    public void setFiltered(boolean filtered) {
        super.setFiltered(filtered);
        super.toggleAccessConstraint(listBox, filtered);
        listBox.setEnabled(!filtered);
        wrapper.setConstraintsApply(filtered);
    }

    @Override
    public E getValue() {
        return asEnum(getSelectedValue());
    }

    protected E asEnum(final String selectedValue) {
        if (nameValuePairs != null && !nameValuePairs.isEmpty()) {
            for (Map.Entry<E, String> entry : nameValuePairs.entrySet()) {
                if (selectedValue.equals(entry.getValue())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    protected String getSelectedValue() {
        int selectedIndex = listBox.getSelectedIndex();
        if (selectedIndex >= 0) { return listBox.getValue(selectedIndex); } else { return ""; }
    }

    @Override
    public void setValue(E value) {
        String asString = asString();
        clearSelection();
        for (int i = 0; i < listBox.getItemCount(); i++) {
            if (listBox.getValue(i).equals(asString)) {
                selectItem(i);
                break;
            }
        }
    }

    @Override
    public void resetMetaData() {
        super.resetMetaData();
        clearSelection();
    }

    public void selectItem(int i) {
        setUndefined(listBox.getValue(i).equals(""));
        listBox.setItemSelected(i, true);
    }

    @Override
    public Widget asWidget() {
        return wrapper;
    }

    public void clearSelection() {
        this.listBox.setSelectedIndex(0);
    }

    public void setDefaultToFirstOption(boolean b) {
        this.defaultToFirst = b;
    }

    private void setValues(final EnumSet<E> values) {
        this.values = values;
        listBox.clear();
        if (values.isEmpty()) { listBox.addItem(""); }
        for (E value : values) {
            listBox.addItem(value.name(), value.name());
        }
        if (defaultToFirst) { selectItem(0); }
    }

    private void setValues(final Map<E, String> nameValuePairs) {
        this.nameValuePairs = nameValuePairs;
        listBox.clear();
        if (nameValuePairs.isEmpty()) { listBox.addItem(""); }
        for (Map.Entry<E, String> entry : nameValuePairs.entrySet()) {
            listBox.addItem(entry.getKey().name(), entry.getValue());
        }
        if (defaultToFirst) { selectItem(0); }
    }

    @Override
    public void setEnabled(boolean b) {
        listBox.setEnabled(b);
    }

    @Override
    public String getErrMessage() {
        return "missing selection";
    }

    @Override
    public boolean validate(E value) {
        return !(isRequired() && getSelectedValue().equals(""));
    }

    @Override
    public void setErroneous(boolean b) {
        super.setErroneous(b);
        wrapper.setErroneous(b);
    }

    @Override
    public void clearValue() {
        clearSelection();
        if (defaultToFirst && listBox.getItemCount() > 0) { selectItem(0); }
    }

    public void setDefaultToFirst(final boolean defaultToFirst) {
        this.defaultToFirst = defaultToFirst;
    }

    @Override
    public String asString() {
        return super.asString();
    }
}
