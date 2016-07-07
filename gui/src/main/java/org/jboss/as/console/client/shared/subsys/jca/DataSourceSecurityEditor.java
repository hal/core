package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.client.widgets.forms.FormEditor;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.SECURITY_DOMAIN;

/**
 * @author Heiko Braun
 * @date 12/13/11
 */
public class DataSourceSecurityEditor extends FormEditor<DataSource>{

    public DataSourceSecurityEditor(FormToolStrip.FormCallback<DataSource> callback) {

        super(DataSource.class);

        ModelNode helpAddress = Baseadress.get();
        helpAddress.add("subsystem", "datasources");
        helpAddress.add("data-source", "*");

        setCallback(callback);
        setHelpAddress(helpAddress);
    }

    @Override
    public Widget asWidget() {

        SuggestionResource suggestionResource = new SuggestionResource("securityDomain", "Security Domain", false,
                Console.MODULES.getCapabilities().lookup(SECURITY_DOMAIN));
        FormItem securityDomain = suggestionResource.buildFormItem();

        TextBoxItem user = new TextBoxItem("username", "Username") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };
        PasswordBoxItem pass = new PasswordBoxItem("password", "Password") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };
        CheckBoxItem allowMultipleUsers = new CheckBoxItem("allowMultipleUsers", "Allow Multiple Users");

        getForm().setFields(user, pass, securityDomain, allowMultipleUsers);

        return super.asWidget();
    }
}
