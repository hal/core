/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.v3.widgets;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.Property;

/**
 * @author Harald Pehl
 */
public interface PropertyManager {

    AddressTemplate getAddress();

    void onSelect(final Property property);
    void onDeselect();

    void openAddDialog(final AddPropertyDialog addDialog);
    void closeAddDialog(final AddPropertyDialog addDialog);

    String getAddOperationName();
    void onAdd(final Property property, AddPropertyDialog window);
    void onAddSuccess(final Property property);
    void onAddFailed(final Property property, Throwable t);
    
    void onModify(final Property property);
    void onModifySuccess(final Property property);
    void onModifyFailed(final Property property, Throwable t);

    String getRemoveOperationName();
    void onRemove(final Property property);
    void onRemoveSuccess(final Property property);
    void onRemoveFailed(final Property property, Throwable t);
}
