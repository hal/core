package org.jboss.as.console.client.rbac;

import org.jboss.as.console.client.shared.help.StaticHelpPanel;


/**
 * @author Harald Pehl
 * @date 07/25/2013
 */
public class RolesHelpPanel extends StaticHelpPanel {

    public RolesHelpPanel() {
        super("<table style='vertical-align:top' cellpadding=3>\n" +
                "<tr>\n" +
                "<td>\n" +
                "Monitor\n" +
                "</td>\n" +
                "<td>\n" +
                "The monitor role has the fewest permissions and restricts the user to viewing the configuration and the current state. The monitor role does not have permission to view sensitive data.\n" +
                "</td></tr>\n" +
                "\n" +
                "<tr>\n" +
                "<td>\n" +
                "Operator\n" +
                "</td>\n" +
                "<td>\n" +
                "The operator role has monitor permissions and can also change the runtime state but not the persistent configuration. For example, the operator can start or stop servers.The operator role does not have permission to view sensitive data.\n" +
                "</td></tr>\n" +
                "\n" +
                "<tr>\n" +
                "<td>\n" +
                "Maintainer\n" +
                "</td>\n" +
                "<td>\n" +
                "The Maintainer role has the same permissions as the Operator role, and additionally can change the persistent configuration. For example, the Maintainer can deploy an application. The maintainer role does not have permission to view or modify sensitive data.\n" +
                "</td></tr>\n" +
                "\n" +
                "<tr>\n" +
                "<td>\n" +
                "Deployer\n" +
                "</td>\n" +
                "<td>\n" +
                "The Deployer role has the permissions of the Maintainer, but with those permissions constrained to operating on application resources.\n" +
                "</td></tr>\n" +
                "\n" +
                "<tr>\n" +
                "<td>\n" +
                "Administrator\n" +
                "</td>\n" +
                "<td>\n" +
                "The Administrator role has the permissions of the Maintainer. This role also has permission to view and modify sensitive data, including passwords, but excluding the management security auditing system. The Administrator role can modify administrative users and roles. \n" +
                "</td></tr>\n" +
                "\n" +
                "<tr>\n" +
                "<td>\n" +
                "Auditor\n" +
                "</td>\n" +
                "<td>\n" +
                "The Auditor role can view and modify the configuration settings for the management security auditing system. The Auditor role includes the Monitor role, and also allows the Auditor to view but not change the rest of the security configuration.\n" +
                "</td></tr>\n" +
                "\n" +
                "<tr>\n" +
                "<td>\n" +
                "SuperUser\n" +
                "</td>\n" +
                "<td>\n" +
                "The SuperUser role has the combined permissions of the Administrator and Auditor roles. This role has all available permissions.\n" +
                "</td></tr>\n" +
                "</table>\n");
    }
}
