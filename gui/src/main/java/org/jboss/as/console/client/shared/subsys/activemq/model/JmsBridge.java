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
package org.jboss.as.console.client.shared.subsys.activemq.model;

import org.jboss.as.console.client.widgets.forms.Binding;

/**
 * @author Claudio Miranda
 */
public interface JmsBridge {

    // type OBJECT
    // source-context
    // target-context
    
    @Binding(detypedName = "add-messageID-in-header", skip = true)
    boolean isMessageIDHeader();
    void setMessageIDHeader(final boolean messageIDHeader);

    @Binding(detypedName = "client-id", skip = true)
    String getClientId();
    void setClientId(final String clientId);

    @Binding(detypedName = "failure-retry-interval")
    Long getFailureRetryInterval();
    void setFailureRetryInterval(final Long failureRetryInterval);

    @Binding(detypedName = "max-batch-size")
    Integer getMaxBatchSize();
    void setMaxBatchSize(final Integer maxBatchSize);

    @Binding(detypedName = "max-batch-time")
    Long getMaxBatchTime();
    void setMaxBatchTime(final Long maxBatchTime);

    @Binding(detypedName = "max-retries")
    Integer getMaxRetries();
    void setMaxRetries(final Integer maxRetries);

    @Binding(detypedName = "module", skip=true)
    String getModule();
    void setModule(final String module);

    @Binding(detypedName = "quality-of-service")
    String getQualityOfService();
    void setQualityOfService(final String qualityOfService);

    @Binding(detypedName = "selector", skip = true)
    String getSelector();
    void setSelector(final String selector);

    @Binding(detypedName = "subscription-name", skip = true)
    String getSubscriptionName();
    void setSubscriptionName(final String subscriptionName);

    @Binding(detypedName = "source-connection-factory")
    String getSourceConnectionFactory();
    void setSourceConnectionFactory(final String sourceConnectionFactory);

    @Binding(detypedName = "source-destination")
    String getSourceDestination();
    void setSourceDestination(final String sourceDestination);

    @Binding(detypedName = "source-destination", skip = true)
    String getSourcePassword();
    void setSourcePassword(final String sourcePassword);

    @Binding(detypedName = "source-user", skip = true)
    String getSourceUser();
    void setSourceUser(final String sourceUser);

    @Binding(detypedName = "target-connection-factory")
    String getTargetConnectionFactory();
    void setTargetConnectionFactory(final String targetConnectionFactory);

    @Binding(detypedName = "target-destination")
    String getTargetDestination();
    void setTargetDestination(final String targetDestination);

    @Binding(detypedName = "target-password", skip = true)
    String getTargetPassword();
    void setTargetPassword(final String targetPassword);

    @Binding(detypedName = "target-user", skip = true)
    String getTargetUser();
    void setTargetUser(final String targetUser);
}
