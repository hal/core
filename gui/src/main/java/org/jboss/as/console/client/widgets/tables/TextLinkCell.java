package org.jboss.as.console.client.widgets.tables;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityService;
import org.jboss.ballroom.client.spi.Framework;

/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class TextLinkCell<T> extends ActionCell<T> {

    private final boolean visible;
    private String title;

    static Framework FRAMEWORK  = GWT.create(Framework.class);
    static SecurityService SECURITY_SERVICE = FRAMEWORK.getSecurityService();

    public TextLinkCell(String title, Delegate<T> delegate) {
        super(title, delegate);
        this.title = title;

         // access control
        SecurityContext securityContext = SECURITY_SERVICE.getSecurityContext();

        visible = securityContext.getWritePriviledge().isGranted();
    }

    @Override
    public void render(Context context, T value, SafeHtmlBuilder sb) {


        String css = !visible ? "textlink-cell rbac-suppressed" : "textlink-cell";
        SafeHtml html = new SafeHtmlBuilder()
                .appendHtmlConstant("<a href='javascript:void(0)' tabindex=\"-1\" class='"+css+"'>")
                .appendHtmlConstant(title)
                .appendHtmlConstant("</a>")
                .toSafeHtml();


        sb.append(html);
    }

}

