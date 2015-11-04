package org.jboss.as.console.client.rbac;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.ballroom.client.rbac.SecurityContext;

/**
 * @author Heiko Braun
 * @date 8/7/13
 */
public class RBACUtil {


    public static SafeHtml dump(SecurityContext sc) {
        SafeHtmlBuilder html = new SafeHtmlBuilder();

        SecurityContextImpl context = (SecurityContextImpl)sc; // parent context

        html.appendHtmlConstant("<hr/><b>Child Contexts:</b> <br/>");
        html.appendHtmlConstant("<ul>");
        for (AddressTemplate address : context.getResourceAddresses()) {
            html.appendHtmlConstant("<li><i>").appendEscaped(address.toString()).appendHtmlConstant("</i></li>");
            String activeKey = context.getActiveKey(address);

            html.appendHtmlConstant("<ul>");
            for (String key : context.getConstraintsKeys(address)) {
                if(key.equals(activeKey))
                    html.appendHtmlConstant("<li><b>").appendEscaped(key).appendHtmlConstant("</b></li>");
                else
                    html.appendHtmlConstant("<li>").appendEscaped(key).appendHtmlConstant("</li>");
            }
            html.appendHtmlConstant("</ul>");

        }

        html.appendHtmlConstant("</ul>");

        writeContext(html, context);

        return html.toSafeHtml();
    }

    private static void writeContext(SafeHtmlBuilder html, SecurityContextImpl context) {
        // required resource
        html.appendHtmlConstant("<h2>Resources References for: "+context.nameToken+"</h2>");
        html.appendHtmlConstant("<h3>Required</h3>");
        html.appendHtmlConstant("<ul>");
        for(AddressTemplate ref : context.requiredResources)
        {
            if(ref.isOptional()) continue;
            html.appendHtmlConstant("<li>").appendEscaped(ref.toString()).appendHtmlConstant("</li>");
        }
        html.appendHtmlConstant("</ul><p/>");

        // optional resource
        html.appendHtmlConstant("<h3>Optional</h3>");
        html.appendHtmlConstant("<ul>");
        for(AddressTemplate ref : context.requiredResources)
        {
            if(!ref.isOptional()) continue;
            html.appendHtmlConstant("<li>").appendEscaped(ref.toString()).appendHtmlConstant("</li>");
        }
        html.appendHtmlConstant("</ul><p/>");

        html.appendHtmlConstant("<h2>Constraints</h2>");

        dumpPermissions(html, context);
        //dumpPermissions(html, context.optionalConstraints);
    }

    private static void dumpPermissions(SafeHtmlBuilder html, SecurityContextImpl securityContext) {
        for(AddressTemplate resource : securityContext.requiredResources)
        {
            html.appendHtmlConstant("<h3>").appendEscaped(resource.toString()).appendHtmlConstant("</h3>");
            html.appendHtmlConstant("<hr noshade/>");
            Constraints constraints = securityContext.getActiveConstraints(resource); // default constraints
            html.appendHtmlConstant("<ul>");
            html.appendHtmlConstant("<li>").appendEscaped("read-config:"+constraints.isReadResource()).appendHtmlConstant("</li>");
            html.appendHtmlConstant("<li>").appendEscaped("write-config:"+constraints.isWriteResource()).appendHtmlConstant("</li>");
            html.appendHtmlConstant("</ul>");


            html.appendHtmlConstant("<p/>");

            html.appendHtmlConstant("<h4>Attributes</h4>");

            html.appendHtmlConstant("<table border='0' cellpadding='5'>");
            html.appendHtmlConstant("<tr>");
            html.appendHtmlConstant("<th>");
            html.appendEscaped("Attribute Name");
            html.appendHtmlConstant("</th>");
            html.appendHtmlConstant("<th>");
            html.appendEscaped("Read");
            html.appendHtmlConstant("</th>");
            html.appendHtmlConstant("<th>");
            html.appendEscaped("Write");
            html.appendHtmlConstant("</th>");
            html.appendHtmlConstant("</tr>");

            for(String att : constraints.attributePermissions.keySet())
            {
                html.appendHtmlConstant("<tr>");
                html.appendHtmlConstant("<td>");
                html.appendEscaped(att);
                html.appendHtmlConstant("</td>");
                html.appendHtmlConstant("<td>");
                Constraints.AttributePerm attributePerm = constraints.attributePermissions.get(att);
                html.appendEscaped(String.valueOf(attributePerm.isRead()));
                html.appendHtmlConstant("</td>");
                html.appendHtmlConstant("<td>");
                html.appendEscaped(String.valueOf(attributePerm.isWrite()));
                html.appendHtmlConstant("</td>");
                html.appendHtmlConstant("</tr>");
            }
            html.appendHtmlConstant("</table>");

            html.appendHtmlConstant("<p/>");

            html.appendHtmlConstant("<h4>Operations</h4>");

            html.appendHtmlConstant("<table border='0' cellpadding='5'>");
            html.appendHtmlConstant("<tr>");
            html.appendHtmlConstant("<th>");
            html.appendEscaped("Operation Name");
            html.appendHtmlConstant("</th>");
            html.appendHtmlConstant("<th>");
            html.appendEscaped("Exec");
            html.appendHtmlConstant("</th>");
            html.appendHtmlConstant("</tr>");

           if(!constraints.execPermission.isEmpty())
            {
                for(String op : constraints.execPermission)
                {
                    html.appendHtmlConstant("<tr>");
                    html.appendHtmlConstant("<td>");
                    html.appendEscaped(op);
                    html.appendHtmlConstant("</td>");
                    html.appendHtmlConstant("<td>");
                    html.appendEscaped("true");
                    html.appendHtmlConstant("</td>");
                    html.appendHtmlConstant("</tr>");
                }
            }

            html.appendHtmlConstant("</table>");
        }
    }
}

