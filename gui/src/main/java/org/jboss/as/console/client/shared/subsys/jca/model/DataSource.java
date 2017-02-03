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

package org.jboss.as.console.client.shared.subsys.jca.model;

import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;

/**
 * @author Heiko Braun
 * @date 3/29/11
 */
@Address("/subsystem=datasources/data-source={0}")
public interface DataSource {

    @Binding(key = true)
    String getName();
    void setName(String name);

    @Binding(detypedName = "jndi-name")
    String getJndiName();
    void setJndiName(String name);

    boolean isEnabled();
    void setEnabled(boolean isEnabled);

    @Binding(detypedName = "pool-name")
    String getPoolName();
    void setPoolName(String name);

    @Binding(detypedName = "min-pool-size")
    Integer getMinPoolSize();
    void setMinPoolSize(Integer i);

    @Binding(detypedName = "initial-pool-size")
    Integer getInitialPoolSize();
    void setInitialPoolSize(Integer i);

    @Binding(detypedName = "max-pool-size")
    Integer getMaxPoolSize();
    void setMaxPoolSize(Integer i);

    // --

    @Binding(detypedName = "transaction-isolation")
    String getTransactionIsolation();
    void setTransactionIsolation(String isolationLevel);

    @Binding(detypedName = "new-connection-sql")
    String getConnectionSql();
    void setConnectionSql(String sql);

    // --

    @Binding(detypedName = "security-domain")
    String getSecurityDomain();
    void setSecurityDomain(String domain);

    @Binding(detypedName = "user-name")
    String getUsername();
    void setUsername(String user);

    @Binding(detypedName = "authentication-context")
    String getAuthenticationContext();
    void setAuthenticationContext(String ctx);

    @Binding(detypedName = "elytron-enabled")
    Boolean isElytronEnabled();
    void setElytronEnabled(Boolean b);

    String getPassword();
    void setPassword(String password);

    @Binding(skip=true)
    CredentialReference getCredentialReference();
    void setCredentialReference(CredentialReference credential);

    // regular DS attributes below

    @Binding(detypedName = "connection-url")
    String getConnectionUrl();
    void setConnectionUrl(String url);

    @Binding(detypedName = "driver-class")
    String getDriverClass();
    void setDriverClass(String driverClass);

    @Binding(detypedName = "datasource-class")
    String getDatasourceClass();
    void setDatasourceClass(String datasourceClass);

    @Binding(detypedName = "driver-name")
    String getDriverName();
    void setDriverName(String driver);

    // --

    @Binding(detypedName = "valid-connection-checker-class-name")
    String getValidConnectionChecker();
    void setValidConnectionChecker(String name);

    @Binding(detypedName = "check-valid-connection-sql")
    String getCheckValidSql();
    void setCheckValidSql(String sql);

    @Binding(detypedName = "background-validation")
    Boolean isBackgroundValidation();
    void setBackgroundValidation(Boolean b);

    @Binding(detypedName = "background-validation-millis")
    Long getBackgroundValidationMillis();
    void setBackgroundValidationMillis(Long millis);

    @Binding(detypedName = "validate-on-match")
    Boolean isValidateOnMatch();
    void setValidateOnMatch(Boolean b);

    @Binding(detypedName = "stale-connection-checker-class-name")
    String getStaleConnectionChecker();
    void setStaleConnectionChecker(String name);

    @Binding(detypedName = "exception-sorter-class-name")
    String getExceptionSorter();
    void setExceptionSorter(String name);

    // --

    @Binding(detypedName = "prepared-statements-cache-size")
    Long getPrepareStatementCacheSize();
    void setPrepareStatementCacheSize(Long size);

    @Binding(detypedName = "share-prepared-statements")
    Boolean isSharePreparedStatements();
    void setSharePreparedStatements(Boolean b);

    // --

    boolean isJta();
    void setJta(boolean b);

    @Binding(detypedName = "use-ccm")
    boolean isCcm();
    void setCcm(boolean b);

    @Binding(skip = true)
    int getMajorVersion();
    void setMajorVersion(int major);

    @Binding(skip = true)
    int getMinorVersion();
    void setMinorVersion(int minor);

    @Binding(skip = true)
    String getTestConnection();
    void setTestConnection(String ignore);

    // --

    @Binding(detypedName = "use-try-lock")
    Long getUseTryLock();
    void setUseTryLock(Long l);

    @Binding(detypedName = "blocking-timeout-wait-millis")
    Long getBlockingTimeoutWaitMillis();
    void setBlockingTimeoutWaitMillis(Long l);

    @Binding(detypedName = "idle-timeout-minutes")
    Long getIdleTimeoutMinutes();
    void setIdleTimeoutMinutes(Long l);

    @Binding(detypedName = "set-tx-query-timeout")
    Boolean isSetTxQueryTimeout();
    void setSetTxQueryTimeout(Boolean b);

    @Binding(detypedName = "query-timeout")
    Long getQueryTimeout();
    void setQueryTimeout(Long l);

    @Binding(detypedName = "allocation-retry")
    Integer getAllocationRetry();
    void setAllocationRetry(Integer i);

    @Binding(detypedName = "allocation-retry-wait-millis")
    Long getAllocationRetryWaitMillis();
    void setAllocationRetryWaitMillis(Long l);

    @Binding(detypedName = "statistics-enabled")
    Boolean isStatisticsEnabled();
    void setStatisticsEnabled(Boolean b);

    @Binding(detypedName = "track-statements")
    String getTrackStatements();
    void setTrackStatements(String name);

    @Binding(detypedName = "use-fast-fail")
    Boolean isUseFastFail();
    void setUseFastFail(Boolean b);

    @Binding(detypedName = "allow-multiple-users")
    Boolean isAllowMultipleUsers();
    void setAllowMultipleUsers(Boolean b);

    @Binding(detypedName = "spy")
    Boolean isSpy();
    void setSpy(Boolean b);

}
