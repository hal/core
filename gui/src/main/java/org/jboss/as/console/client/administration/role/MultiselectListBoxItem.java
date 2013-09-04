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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.InputElementWrapper;

/**
 * @author Harald Pehl
 */
public class MultiselectListBoxItem extends FormItem<List<String>> {

    private final ListBox listBox;
    private final InputElementWrapper wrapper;
    private final ChangeHandler valueChangeHandler;

    public MultiselectListBoxItem(final String name, final String title) {
        this(name, title, 5);
    }

    public MultiselectListBoxItem(final String name, final String title, final int visibleItemCount) {
        super(name, title);

        listBox = new ListBox(true);
        listBox.setName(name);
        listBox.setTitle(title);
        listBox.setVisibleItemCount(visibleItemCount);
        listBox.setTabIndex(0);
        listBox.addStyleName("multiselect");

        valueChangeHandler = new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                setModified(true);
            }
        };
        listBox.addChangeHandler(valueChangeHandler);

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
    public Widget asWidget() {
        return wrapper;
    }


    @Override
    public void setEnabled(boolean b) {
        listBox.setEnabled(b);
    }

    @Override
    public boolean validate(List<String> value) {
        return !(isRequired && getValue().isEmpty());
    }

    @Override
    public void setErroneous(boolean b) {
        super.setErroneous(b);
        wrapper.setErroneous(b);
    }

    @Override
    public String getErrMessage() {
        return super.getErrMessage() + ": Select an item";
    }

    @Override
    public void clearValue() {
        // We could implement this but not sure when it would be useful
    }

    @Override
    public List<String> getValue() {
        List<String> value = new ArrayList<String>();
        for (int i = 0; i < listBox.getItemCount(); i++) {
            if (listBox.isItemSelected(i)) {
                value.add(listBox.getItemText(i));
            }
        }
        return value;
    }

    @Override
    public void setValue(final List<String> value) {
        for (int i = 0; i < listBox.getItemCount(); i++) {
            String itemText = listBox.getItemText(i);
            listBox.setItemSelected(i, value.contains(itemText));
        }
    }

    public void setChoices(List<String> values) {
        listBox.clear();
        for (String value : values) {
            listBox.addItem(value, value);
        }
    }
}
