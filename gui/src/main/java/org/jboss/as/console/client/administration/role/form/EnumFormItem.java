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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
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
    private final Lookup lookup;
    private boolean defaultToFirst;

    public EnumFormItem(final String name, final String title) {
        super(name, title);

        listBox = new ListBox();
        listBox.setTabIndex(0);
        listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                setModified(true);
                setUndefined(getValue() == null);
            }
        });
        wrapper = new InputElementWrapper(listBox.asWidget(), this);
        wrapper.getElement().setAttribute("style", "width:100%");

        lookup = new Lookup();
    }

    public void setValues(final EnumSet<E> values) {
        lookup.setValues(values);
        updateValues();
    }

    public void setValues(final Map<E, String> values) {
        lookup.setValues(values);
        updateValues();
    }

    private void updateValues() {
        listBox.clear();
        for (EnumData ed : lookup) {
            listBox.addItem(ed.itemText, ed.stringValue);
        }
        if (defaultToFirst) {
            selectItem(0);
        }
    }

    @Override
    public E getValue() {
        return lookup.byIndex(listBox.getSelectedIndex()).value;
    }

    @Override
    public void setValue(E value) {
        clearSelection();
        int index = lookup.byValue(value).index;
        if (index >= 0) {
            selectItem(index);
        }
    }

    @Override
    public void clearValue() {
        clearSelection();
        if (defaultToFirst && listBox.getItemCount() > 0) {
            selectItem(0);
        }
    }

    private void clearSelection() {
        listBox.setSelectedIndex(0);
    }

    private void selectItem(int index) {
        if (index < listBox.getItemCount()) {
            listBox.setItemSelected(index, true);
        }
        setModified(true);
        setUndefined(getValue() == null);
    }


    @Override
    public void setFiltered(boolean filtered) {
        super.setFiltered(filtered);
        super.toggleAccessConstraint(listBox, filtered);
        listBox.setEnabled(!filtered);
        wrapper.setConstraintsApply(filtered);
    }

    @Override
    public void resetMetaData() {
        super.resetMetaData();
        clearSelection();
    }

    @Override
    public Widget asWidget() {
        return wrapper;
    }

    public void setDefaultToFirstOption(boolean b) {
        this.defaultToFirst = b;
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
        return !(isRequired() && getValue() == null);
    }

    @Override
    public void setErroneous(boolean b) {
        super.setErroneous(b);
        wrapper.setErroneous(b);
    }

    public void setDefaultToFirst(final boolean defaultToFirst) {
        this.defaultToFirst = defaultToFirst;
    }

    @Override
    public String asString() {
        return lookup.byIndex(listBox.getSelectedIndex()).itemText;
    }

    public HandlerRegistration addChangeHandler(final ChangeHandler handler) {return listBox.addChangeHandler(handler);}

    class Lookup implements Iterable<EnumData> {

        final Map<Integer, EnumData> index;
        final Map<E, EnumData> value;

        Lookup() {
            index = new TreeMap<Integer, EnumData>();
            value = new HashMap<E, EnumData>();
        }

        void setValues(final EnumSet<E> values) {
            clear();
            int index = 0;
            for (E e : values) {
                add(new EnumData(index, e, e.name(), e.name()));
                index++;
            }
        }

        public void setValues(final Map<E, String> values) {
            clear();
            int index = 0;
            for (Map.Entry<E, String> entry : values.entrySet()) {
                add(new EnumData(index, entry.getKey(), entry.getKey().name(), entry.getValue()));
                index++;
            }
        }

        private void clear() {
            index.clear();
            value.clear();
        }

        private void add(final EnumData ed) {
            index.put(ed.index, ed);
            value.put(ed.value, ed);
        }

        EnumData byIndex(int index) {
            EnumData enumData = this.index.get(index);
            return enumData == null ? empty() : enumData;
        }

        EnumData byValue(E value) {
            EnumData enumData = this.value.get(value);
            return enumData == null ? empty() : enumData;
        }

        EnumData empty() {
            return new EnumData(-1, null, "_no_value_", "");
        }

        @Override
        public Iterator<EnumData> iterator() {
            return index.values().iterator();
        }
    }

    class EnumData {

        final int index;
        final E value;
        final String stringValue;
        final String itemText;

        EnumData(final int index, final E value, final String stringValue, final String itemText) {
            this.index = index;
            this.value = value;
            this.stringValue = stringValue;
            this.itemText = itemText;
        }

        @Override
        @SuppressWarnings({"unchecked", "RedundantIfStatement"})
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (!(o instanceof EnumFormItem.EnumData)) { return false; }

            EnumData enumData = (EnumData) o;

            if (index != enumData.index) { return false; }
            if (itemText != null ? !itemText.equals(enumData.itemText) : enumData.itemText != null) { return false; }
            if (stringValue != null ? !stringValue.equals(enumData.stringValue) : enumData.stringValue != null) { return false; }
            if (!value.equals(enumData.value)) { return false; }

            return true;
        }

        @Override
        public int hashCode() {
            int result = value.hashCode();
            result = 31 * result + index;
            result = 31 * result + (stringValue != null ? stringValue.hashCode() : 0);
            result = 31 * result + (itemText != null ? itemText.hashCode() : 0);
            return result;
        }
    }
}
