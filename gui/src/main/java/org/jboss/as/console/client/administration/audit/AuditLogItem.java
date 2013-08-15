package org.jboss.as.console.client.administration.audit;

import java.net.InetAddress;
import java.util.List;

import org.jboss.dmr.client.ModelNode;

/**
 * @author Harald Pehl
 * @date 08/13/2013
 */
public interface AuditLogItem {

    boolean isReadOnly();

    void setReadOnly(boolean readOnly);

    boolean isBooting();

    void setBooting(boolean booting);

    String getUserId();

    void setUserId(String userId);

    String getDomainUUID();

    void setDomainUUID(String domainUUID);

    String getAccessMechanism();

    void setAccessMechanism(String accessMechanism);

    String getRemoteAddress();

    void setRemoteAdress(String remoteAdress);

    boolean isSuccess();

    void setSuccess(boolean success);

    List<ModelNode> getOperations();

    void setOperations(List<ModelNode> operations);
}
