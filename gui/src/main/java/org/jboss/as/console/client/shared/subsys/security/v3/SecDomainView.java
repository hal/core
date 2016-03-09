package org.jboss.as.console.client.shared.subsys.security.v3;

import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.dmr.client.Property;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 19/05/15
 */
public class SecDomainView extends SuspendableViewImpl implements SecDomainPresenter.MyView {

    private SecDomainPresenter presenter;
    private PagedView panel;

    private static final AddressTemplate AUTHENTICATION =
            AddressTemplate.of("{selected.profile}/subsystem=security/security-domain=*/authentication=classic/login-module=*");

    private static final AddressTemplate AUTHORIZATION =
            AddressTemplate.of("{selected.profile}/subsystem=security/security-domain=*/authorization=classic/policy-module=*");

    private static final AddressTemplate AUDIT =
            AddressTemplate.of("{selected.profile}/subsystem=security/security-domain=*/audit=classic/provider-module=*");

    private static final AddressTemplate ACL =
            AddressTemplate.of("{selected.profile}/subsystem=security/security-domain=*/acl=classic/acl-module=*");

    private static final AddressTemplate MAPPING =
            AddressTemplate.of("{selected.profile}/subsystem=security/security-domain=*/mapping=classic/mapping-module=*");

    private static final AddressTemplate TRUST =
            AddressTemplate.of("{selected.profile}/subsystem=security/security-domain=*/identity-trust=classic/trust-module=*");


    private SecModule auditHandler;
    private SecModule authorizationHandler;
    private SecModule authenticationHandler;
    private SecModule aclHandler;
    private SecModule mappingHandler;
    private SecModule trustHandler;

    @Override
    public void updateSubResource(SecDomainPresenter.SubResource resource, List<Property> modules) {
        switch (resource)
        {
            case AUTHENTICATION:
                authenticationHandler.setData(modules);
                break;
            case AUTHORIZATION:
                authorizationHandler.setData(modules);
                break;
            case AUDIT:
                auditHandler.setData(modules);
                break;
            case MAPPING:
                mappingHandler.setData(modules);
                break;
            case TRUST:
                trustHandler.setData(modules);
                break;
            case ACL:
                aclHandler.setData(modules);
                break;
        }
    }

    @Override
    public void reset() {
        authenticationHandler.setData(Collections.EMPTY_LIST);
        authorizationHandler.setData(Collections.EMPTY_LIST);
        auditHandler.setData(Collections.EMPTY_LIST);
        aclHandler.setData(Collections.EMPTY_LIST);
        mappingHandler.setData(Collections.EMPTY_LIST);
        trustHandler.setData(Collections.EMPTY_LIST);
    }

    @Override
    public void setPresenter(SecDomainPresenter presenter) {

        this.presenter = presenter;
    }

    @Override
    public void setPreview(SafeHtml html) {

    }

    @Override
    public Widget createWidget() {
        panel = new PagedView(true);


        List<String> auditModules = new LinkedList<>();
        auditModules.add("LogAuditProvider");

        final List<String> authModules  = new LinkedList<>();
        authModules.add("RealmDirect");
        authModules.add("Client");
        authModules.add("Remoting");
        authModules.add("Certificate");
        authModules.add("CertificateRoles");
        authModules.add("Database");
        authModules.add("DatabaseCertificate");
        authModules.add("Identity");
        authModules.add("Ldap");
        authModules.add("LdapExtended");
        authModules.add("RoleMapping");
        authModules.add("RunAs");
        authModules.add("Simple");
        authModules.add("ConfiguredIdentity");
        authModules.add("SecureIdentity");
        authModules.add("PropertiesUsers");
        authModules.add("SimpleUsers");
        authModules.add("LdapUsers");
        authModules.add("Kerberos");
        authModules.add("SPNEGO");
        authModules.add("AdvancedLdap");
        authModules.add("AdvancedADLdap");
        authModules.add("UsersRoles");
        Collections.sort(authModules);

        final List<String> policyModules  = new LinkedList<>();
        policyModules.add("DenyAll");
        policyModules.add("PermitAll");
        policyModules.add("Delegating");
        policyModules.add("Web");
        policyModules.add("JACC");
        policyModules.add("XACML");

        List<String> mappingModules = new LinkedList<>();
        mappingModules.add("PropertiesRoles");
        mappingModules.add("SimpleRoles");
        mappingModules.add("DeploymentRoles");
        mappingModules.add("DatabaseRoles");
        mappingModules.add("LdapRoles");
        mappingModules.add("LdapAttributes");

        authenticationHandler = new SecModule(presenter, AUTHENTICATION, "Authentication Modules", authModules);
        authorizationHandler = new SecModule(presenter, AUTHORIZATION, "Authorization Modules", policyModules);
        auditHandler = new SecModule(presenter, AUDIT, "Audit Modules", auditModules);
        aclHandler = new SecModule(presenter, ACL, "ACL Modules", Collections.EMPTY_LIST);
        mappingHandler = new SecModule(presenter, MAPPING, "Mapping Modules", mappingModules);
        trustHandler = new SecModule(presenter, TRUST, "Trust Modules", Collections.EMPTY_LIST);

        panel.addPage("Authentication", authenticationHandler.asWidget());
        panel.addPage("Authorization", authorizationHandler.asWidget());
        panel.addPage("Audit", auditHandler.asWidget());
        panel.addPage("ACL", aclHandler.asWidget());
        panel.addPage("Mapping", mappingHandler.asWidget());
        panel.addPage("Identity Trust", trustHandler.asWidget());

        // default page
        panel.showPage(0);

        DefaultTabLayoutPanel tabs = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabs.addStyleName("default-tabpanel");
        tabs.add(panel.asWidget(), "Security Domain");
        tabs.selectTab(0);

        return tabs.asWidget();
    }
}
