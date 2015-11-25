package org.jboss.as.console.client.domain.profiles;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.FormValidator;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 16/06/15
 */
public class CloneProfileWizard  {

    private final ProfileMgmtPresenter profileMgmtPresenter;
    private final ProfileRecord fromProfile;

    public CloneProfileWizard(ProfileMgmtPresenter profileMgmtPresenter, ProfileRecord fromProfile) {

        this.profileMgmtPresenter = profileMgmtPresenter;
        this.fromProfile = fromProfile;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        layout.add(new HTML("<h3>" + ((UIMessages) GWT.create(UIMessages.class)).cloneProfile(fromProfile.getName()) + "</h3>"));

        Form<ProfileRecord> form = new Form<ProfileRecord>(ProfileRecord.class);

        final TextBoxItem nameItem = new TextBoxItem("name", "Name", true);
        nameItem.setAllowWhiteSpace(true);
        form.setFields(nameItem);

        layout.add(form.asWidget());

        form.addFormValidator(new FormValidator() {
            @Override
            public void validate(List<FormItem> list, FormValidation outcome) {
                if(profileMgmtPresenter.doesExist(nameItem.getValue())) {
                    String errMessage = ((UIMessages) GWT.create(UIMessages.class))
                            .profileAlreadyExists(nameItem.getValue());
                    nameItem.setErrMessage(errMessage);
                    nameItem.setErroneous(true);
                    outcome.addError(errMessage);
                }
            }
        });

        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                FormValidation validation = form.validate();
                if (!validation.hasErrors()) {
                    profileMgmtPresenter.onSaveClonedProfile(fromProfile, form.getUpdatedEntity());
                }
            }
        };
        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                profileMgmtPresenter.closeDialogue();
            }
        };

        DialogueOptions options = new DialogueOptions(
                ((UIConstants) GWT.create(UIConstants.class)).createProfile(), submitHandler,
                Console.CONSTANTS.common_label_cancel(), cancelHandler
        );
        return new WindowContentBuilder(layout, options).build();
    }
}
