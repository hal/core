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

package org.jboss.as.console.client.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.rbac.StandardRole;

/**
 * @author Heiko Braun
 * @date 2/11/11
 */
public class BootstrapContext implements ApplicationProperties {

    private Map<String,String> ctx = new HashMap<String,String>();
    private String initialPlace = null;
    private Throwable lastError;
    private String serverName;
    private String productName;
    private String productVersion;
    private String principal;
    private boolean sameOrigin;
    private boolean hostManagementDisabled;
    private boolean groupManagementDisabled;
    private Set<String> roles;

    private Set<String> addressableHosts = Collections.emptySet();
    private Set<String> addressableGroups = Collections.emptySet();
    private String runAs;
    private List<ProfileRecord> initialProfiles;
    private long majorVersion;
    private boolean ssoEnabled;

    @Inject
    public BootstrapContext() {
        // Default values
        this.productName = "Management Console";
        this.productVersion = "";
        this.sameOrigin = true;
    }

    @Override
    public void setProperty(String key, String value)
    {
        ctx.put(key, value);
    }

    @Override
    public String getProperty(String key)
    {
        return ctx.get(key);
    }

    @Override
    public boolean hasProperty(String key)
    {
        return getProperty(key)!=null;
    }

    public PlaceRequest getDefaultPlace() {
        PlaceRequest.Builder builder = new PlaceRequest.Builder().nameToken(NameTokens.HomepagePresenter);
        return builder.build();
    }

    @Override
    public void removeProperty(String key) {

        ctx.remove(key);
    }

    @Override
    public boolean isStandalone() {
        return getProperty(BootstrapContext.STANDALONE).equals("true");
    }

    public void setInitialPlace(String nameToken) {
        this.initialPlace = nameToken;
    }

    public String getInitialPlace() {
        return initialPlace;
    }

    public String getLogoutUrl() {
        String url = getProperty(LOGOUT_API);

        if(!GWT.isScript())
            url += "?gwt.codesvr=" + Window.Location.getParameter("gwt.codesvr");
        return url;
    }

    public void setlastError(Throwable caught) {
        this.lastError = caught;
    }

    public Throwable getLastError() {
        return lastError;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(final String productName)
    {
        this.productName = productName;
    }

    public String getProductVersion()
    {
        return productVersion;
    }

    public void setProductVersion(final String productVersion)
    {
        this.productVersion = productVersion;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setHostManagementDisabled(boolean b) {
        this.hostManagementDisabled = b;
    }

    public boolean isHostManagementDisabled() {
        return hostManagementDisabled;
    }

    public void setGroupManagementDisabled(boolean b) {
        this.groupManagementDisabled = b;
    }

    public boolean isGroupManagementDisabled() {
        return groupManagementDisabled;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean isSuperUser() {
        boolean match = false;
        for(String role : roles)
        {
            if(StandardRole.SUPER_USER.equalsIgnoreCase(role))
            {
                match = true;
                break;
            }
        }
        return match;
    }

    public boolean isAdmin() {
        boolean match = false;
        for(String role : roles)
        {
            if(StandardRole.ADMINISTRATOR.equalsIgnoreCase(role))
            {
                match = true;
                break;
            }
        }
        return match;
    }


    public void setRunAs(final String runAs) {
        this.runAs = runAs;
    }

    public String getRunAs() {
        return runAs;
    }

    public void setInitialProfiles(final List<ProfileRecord> initialProfiles) {
        this.initialProfiles = initialProfiles;
    }

    public List<ProfileRecord> getInitialProfiles() {
        return initialProfiles;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean isSameOrigin() {
        return sameOrigin;
    }

    public void setSameOrigin(boolean sameOrigin) {
        this.sameOrigin = sameOrigin;
    }

    public void setMajorVersion(long majorVersion) {
        this.majorVersion = majorVersion;
    }

    /**
     * management model major version
     * @return
     */
    public long getMajorVersion() {
        return majorVersion;
    }

    public boolean isSsoEnabled() {
        return ssoEnabled;
    }

    public void setSsoEnabled(final boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    /**
     * Obtains the authentication server url (keycloak admin console) from keycloak object attached to the window.
     *
     * @return The auth-server-url parameter extracted from <pre>/keycloak/adapter/wildfly-console/</pre>
     */
    public static native String retrieveSsoAuthUrl()/*-{
        // keycloak object is created at App.html
        var keycloak = $wnd.keycloak
        if (keycloak != null && $wnd.keycloak.authServerUrl != null) {
            return $wnd.keycloak.authServerUrl;
        }

        return null;
    }-*/;

}
