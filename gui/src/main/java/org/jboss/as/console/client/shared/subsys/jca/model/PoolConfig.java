package org.jboss.as.console.client.shared.subsys.jca.model;

import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 9/16/11
 */
@Address("/subsystem=datasources/{0}={1}")
public interface PoolConfig {

    @Binding(key = true)
    String getName();
    void setName(String name);

    @Binding(detypedName = "max-pool-size")
    Integer getMaxPoolSize();
    void setMaxPoolSize(Integer max);

    @Binding(detypedName = "min-pool-size")
    Integer getMinPoolSize();
    void setMinPoolSize(Integer min);

    @Binding(detypedName = "pool-prefill")
    boolean isPoolPrefill();
    void setPoolPrefill(boolean b);

    @Binding(detypedName = "pool-use-strict-min")
    boolean isPoolStrictMin();
    void setPoolStrictMin(boolean b);

    @Binding(detypedName = "flush-strategy")
    String getFlushStrategy();
    void setFlushStrategy(String name);

    @Binding(detypedName = "idle-timeout-minutes")
    long getIdleTimeout();
    void setIdleTimeout(long timeout);

    @Binding(detypedName = "track-statements")
    String getTrackStatements();
    void setTrackStatements(String name);

    @Binding(detypedName = "capacity-decrementer-class")
    String getCapacityDecrementerClass();
    void setCapacityDecrementerClass(String clazz);

    @Binding(detypedName = "capacity-decrementer-properties")
    Map<String, String> getCapacityDecrementerProperties();
    void setCapacityDecrementerProperties(Map<String, String> properties);

    @Binding(detypedName = "capacity-incrementer-class")
    String getCapacityIncrementerClass();
    void setCapacityIncrementerClass(String clazz);

    @Binding(detypedName = "capacity-incrementer-properties")
    Map<String, String> getCapacityIncrementerProperties();
    void setCapacityIncrementerProperties(Map<String, String> properties);

    // metrics below
    @Binding(skip = true)
    Integer getActiveCount();
    void setActiveCount(Integer i);

    @Binding(skip = true)
    Integer getCreatedCount();
    void setCreatedCount(Integer i);

    @Binding(skip = true)
    Integer getMaxWaitCount();
    void setMaxWaitCount(Integer i);

    @Binding(skip = true)
    Integer getAvailbleCount();
    void setAvailbleCount(Integer i);

    @Binding(skip = true)
    Integer getMaxUsedCount();
    void setMaxUsedCount(Integer i);
}

