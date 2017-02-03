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

        TextBoxItem user = new TextBoxItem("username", "Username", false);
        PasswordBoxItem pass = new PasswordBoxItem("password", "Password", false);
        CheckBoxItem allowMultipleUsers = new CheckBoxItem("allowMultipleUsers", "Allow Multiple Users");
        TextBoxItem authContext = new TextBoxItem("authenticationContext", "Authentication Context", false);
        CheckBoxItem elytronEnabled = new CheckBoxItem("elytronEnabled", "Elytron enabled");
        elytronEnabled.setRequired(false);

        getForm().setFields(user, pass, securityDomain, allowMultipleUsers, authContext, elytronEnabled);
        getForm().addFormValidator((formItems, formValidator) -> {

            // validates the "requires" constraint of authentication-context
            // authentication-context requires elytron-enabled
            boolean authCtxSet = !authContext.isUndefined()  && authContext.getValue().trim().length() > 0;
            boolean elytronEnabledSet = elytronEnabled.getValue();

            if (authCtxSet && !elytronEnabledSet) {
                formValidator.addError("elytronEnabled");
                elytronEnabled.setErrMessage("This field is required if Authentication Context is set.");
                elytronEnabled.setErroneous(true);
            }

            // validates the "alternatives" constraint
            boolean securityDomainSet = !securityDomain.isUndefined() && securityDomain.getValue().toString().trim().length() > 0;
            boolean userSet = !user.isUndefined()  && user.getValue().trim().length() > 0;
            if (elytronEnabledSet && (securityDomainSet || userSet)) {
                formValidator.addError("elytronEnabled");
                elytronEnabled.setErrMessage("This field must not be used in combination with Security Domain or Username.");
                elytronEnabled.setErroneous(true);
            }
            if (securityDomainSet && userSet) {
                formValidator.addError("username");
                user.setErrMessage("This field must not be used in combination with Security Domain.");
                user.setErroneous(true);
            }

            boolean passwdSet = !pass.isUndefined() && pass.getValue().trim().length() > 0;
            if (passwdSet && !userSet) {
                formValidator.addError("username");
                user.setErrMessage("This field is required if Password is set.");
                user.setErroneous(true);
            }
        });

        return super.asWidget();
    }

}
