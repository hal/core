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

import org.jboss.ballroom.client.widgets.forms.AbstractForm;
import org.jboss.ballroom.client.widgets.forms.EditListener;
import org.jboss.ballroom.client.widgets.forms.FormItem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Simple form for POJOs. Expects you to handle the form items on your own.
 *
 * @author Harald Pehl
 */
public class PojoForm<T> extends AbstractForm<T> {

    private T bean;

    @Override
    public void editTransient(T newBean) {
        isTransient = true;
        edit(newBean);
    }

    @Override
    public void edit(final T bean) {
        this.bean = bean;
        refreshPlainView();
    }

    public void setUndefined(final boolean undefined) {
        for (Map<String, FormItem> groupItems : formItems.values()) {
            for (FormItem item : groupItems.values()) {
                item.setUndefined(undefined);
            }
        }
    }

    @Override
    public void cancel() {

    }

    @Override
    public void clearValues() {
        for (Map<String, FormItem> groupItems : formItems.values()) {
            for (FormItem item : groupItems.values()) {
                item.resetMetaData();
                item.clearValue();
            }
        }
        refreshPlainView();
    }

    @Override
    public Map<String, Object> getChangedValues() {
        return Collections.emptyMap();
    }

    @Override
    public T getEditedEntity() {
        return bean;
    }

    @Override
    public T getUpdatedEntity() {
        return bean;
    }

    @Override
    public Class<?> getConversionType() {
        return null;
    }

    @Override
    public void addEditListener(final EditListener listener) {

    }

    @Override
    public void removeEditListener(final EditListener listener) {

    }

    @Override
    public Set<String> getReadOnlyNames() {
        return Collections.emptySet();
    }
}
