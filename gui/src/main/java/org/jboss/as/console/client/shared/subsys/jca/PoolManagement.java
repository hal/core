package org.jboss.as.console.client.shared.subsys.jca;

import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 9/20/11
 */
public interface PoolManagement {

    void onSavePoolConfig(final String parentName, Map<String, Object> changeset);

    void onResetPoolConfig(final String parentName, PoolConfig entity);

    void onDoFlush(String editedName, String flushOp);
}
