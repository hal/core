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
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.InputElementWrapper;

/**
 * @author Harald Pehl
 */
public class StandardRoleFormItem extends FormItem<StandardRole> {

    private final ListBox listBox;
    private final InputElementWrapper wrapper;

    public StandardRoleFormItem(final String name, final String title) {
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
    }

    public void setValues() {
        listBox.clear();
        for (StandardRole role : StandardRole.values()) {
            listBox.addItem(role.getId(), role.getId());
        }
        selectItem(0);
    }

    @Override
    public StandardRole getValue() {
        final int selectedIndex = listBox.getSelectedIndex();
        if (selectedIndex == -1) {
            return null;
        } else {
            final String value = listBox.getValue(selectedIndex);
            return StandardRole.fromId(value);
        }
    }

    @Override
    public void setValue(StandardRole value) {
        clearSelection();
        int index = 0;
        for (StandardRole role : StandardRole.values()) {
            if (role.equals(value)) {
                break;
            }
            index++;
        }
        selectItem(index);
    }

    @Override
    public void clearValue() {
        clearSelection();
        if (listBox.getItemCount() > 0) {
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

    @Override
    public void setEnabled(boolean b) {
        listBox.setEnabled(b);
    }

    @Override
    public String getErrMessage() {
        return "missing selection";
    }

    @Override
    public boolean validate(StandardRole value) {
        return !(isRequired() && getValue() == null);
    }

    @Override
    public void setErroneous(boolean b) {
        super.setErroneous(b);
        wrapper.setErroneous(b);
    }

    @Override
    public String asString() {
        return getValue() != null ? getValue().getId() : "";
    }
}
