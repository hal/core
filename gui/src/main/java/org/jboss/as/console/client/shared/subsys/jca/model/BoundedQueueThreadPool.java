package org.jboss.as.console.client.shared.subsys.jca.model;

import org.jboss.as.console.client.shared.subsys.jca.ThreadPool;
import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;
import org.jboss.as.console.client.widgets.forms.FormItem;

/**
 * Model for a Bounded Queue Thread Pool
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2011 Red Hat Inc.
 */
@Address("/subsystem=threads/bounded-queue-thread-pool={0}")
public interface BoundedQueueThreadPool extends ThreadPool {

    @Override
    @Binding(detypedName="name", key=true)
    @FormItem(defaultValue="",
            localLabel="common_label_name",
            required=true,
            formItemTypeForEdit="TEXT",
            formItemTypeForAdd="TEXT_BOX",
            order=0)
    public String getName();
    @Override
    public void setName(String name);

    @Binding(detypedName="core-threads")
    @FormItem(defaultValue="2",
            required=true,
            label="Core Threads",
            formItemTypeForAdd="NUMBER_BOX",
            formItemTypeForEdit="NUMBER_BOX")
    Integer getCoreThreads();
    void setCoreThreads(Integer coreThreads);

    @Binding(detypedName="keepalive-time/time")
    @FormItem(defaultValue="60",
            required=true,
            label="Keepalive Time",
            formItemTypeForAdd="NUMBER_BOX",
            formItemTypeForEdit="NUMBER_BOX")
    Long getKeepaliveTime();
    void setKeepaliveTime(Long timeout);

    @Binding(detypedName="keepalive-time/unit")
    @FormItem(defaultValue="SECONDS",
            label="Keepalive Time Unit",
            required=true,
            formItemTypeForEdit="TIME_UNITS",
            formItemTypeForAdd="TIME_UNITS")
    String getKeepaliveTimeUnit();
    void setKeepaliveTimeUnit(String unit);

    @Binding(detypedName="thread-factory")
    @FormItem(defaultValue="",
            label="Thread Factory",
            required=false,
            formItemTypeForEdit="COMBO_BOX",
            formItemTypeForAdd="COMBO_BOX")
    String getThreadFactory();
    void setThreadFactory(String threadFactory);

    @Binding(detypedName="queue-length")
    @FormItem(defaultValue="2",
            required=true,
            label="Queue Length",
            formItemTypeForAdd="NUMBER_BOX",
            formItemTypeForEdit="NUMBER_BOX")
    Integer getQueueLength();
    void setQueueLength(Integer queueLength);

    @Binding(detypedName="handoff-executor")
    @FormItem(defaultValue="",
            label="Handoff Executor",
            required=false,
            formItemTypeForEdit="TEXT_BOX",
            formItemTypeForAdd="TEXT_BOX")
    String getHandoffExecutor();
    void setHandoffExecutor(String handoffExecutor);

    @Binding(detypedName="max-threads")
    @FormItem(defaultValue="2",
            required=true,
            label="Max Threads",
            formItemTypeForAdd="NUMBER_BOX",
            formItemTypeForEdit="NUMBER_BOX")
    @Override
    Integer getMaxThreads();
    void setMaxThreads(Integer maxThreads);

    @Binding(detypedName="allow-core-timeout")
    @FormItem(defaultValue="true",
            label="Allow Core Timeout",
            required=false,
            formItemTypeForEdit="CHECK_BOX",
            formItemTypeForAdd="CHECK_BOX")
    boolean isAllowCoreTimeout();
    void setAllowCoreTimeout(boolean allowCoreTimeout);

}