package org.jboss.as.console.client.administration.audit;

import static com.google.web.bindery.autobean.shared.AutoBean.PropertyName;

import com.google.web.bindery.autobean.shared.Splittable;

/**
 * @author Harald Pehl
 * @date 08/13/2013
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
