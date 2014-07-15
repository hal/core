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
package org.jboss.as.console.client.administration.audit;

import com.google.web.bindery.autobean.shared.Splittable;

import static com.google.web.bindery.autobean.shared.AutoBean.PropertyName;

/**
 * @author Harald Pehl
 */
public interface AuditLogItem {

    Object getId();
    void setId(Object id);

    String getDate();
    void setDate(String date);

    @PropertyName("r/o")
    boolean isReadOnly();
    void setReadOnly(boolean readOnly);

    boolean isBooting();
    void setBooting(boolean booting);

    String getUser();
    void setUser(String userId);

    String getDomainUUID();
    void setDomainUUID(String domainUUID);

    String getAccess();
    void setAccess(String access);

    @PropertyName("remote-address")
    String getRemoteAddress();
    void setRemoteAddress(String remoteAddress);

    boolean isSuccess();
    void setSuccess(boolean success);

    @PropertyName("ops")
    Splittable getOperations();
    void setOperations(Splittable operations);
}
