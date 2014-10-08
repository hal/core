package org.jboss.as.console.client.shared.subsys.jca.wizard;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.jca.ResourceAdapterPresenter;
import org.jboss.as.console.client.shared.subsys.jca.model.ResourceAdapter;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.FormValidator;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 7/20/11
 */
public class NewAdapterWizard {


    private ResourceAdapterPresenter presenter;
    private final ResourceAdapter bean;

    public NewAdapterWizard(ResourceAdapterPresenter presenter, ResourceAdapter bean) {
        this.presenter = presenter;

        this.bean = bean;
    }

    public Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        final Form<ResourceAdapter> form = new Form(ResourceAdapter.class);

        TextBoxItem name = new TextBoxItem("name", "Name");
        final TextBoxItem archiveItem = new TextBoxItem("archive", "Archive", false);
        final TextBoxItem moduleItem = new TextBoxItem("module", "Module", false);

        ComboBoxItem txItem = new ComboBoxItem("transactionSupport", "TX");
        txItem.setDefaultToFirstOption(true);
        txItem.setValueMap(new String[]{"NoTransaction", "LocalTransaction", "XATransaction"});

        form.setFields(name,archiveItem, moduleItem, txItem);

        form.addFormValidator(new FormValidator() {
            @Override
            public void validate(List<FormItem> items, FormValidation outcome) {
                TextBoxItem archive = (TextBoxItem)findItem(items, "archive");
                TextBoxItem module = (TextBoxItem)findItem(items, "module");

                boolean archiveIsSet = archive.getValue()==null || !archive.getValue().equals("");
                boolean moduleIsSet = module.getValue()==null || !module.getValue().equals("");
                if(archiveIsSet && moduleIsSet)
                {
                    String msg = "Either archive or module can be set";
                    outcome.addError(msg);
                    archive.setErrMessage(msg);
                    archive.setErroneous(true);
                    module.setErrMessage(msg);
                    module.setErroneous(true);
                }
                else if(!archiveIsSet && !moduleIsSet)
                {
                    String msg = "One of archive or module has to be specified";
                    outcome.addError(msg);
                    archive.setErrMessage(msg);
                    archive.setErroneous(true);
                    module.setErrMessage(msg);
                    module.setErroneous(true);
                }


            }

            private FormItem findItem(List<FormItem> items, String name) {
                for (FormItem item : items) {
                    if (name.equals(item.getName())) {
                        return item;
                    }
                }
                return null;
            }
        });
        final FormHelpPanel helpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = Baseadress.get();
                        address.add("subsystem", "resource-adapters");
                        address.add("resource-adapter", "*");
                        return address;
                    }
                }, form
        );

        layout.add(helpPanel.asWidget());

        layout.add(form.asWidget());

        form.editTransient(bean);

        DialogueOptions options = new DialogueOptions(

                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        FormValidation validation = form.validate();
                        if(!validation.hasErrors())
                            presenter.onCreateAdapter(form.getUpdatedEntity());
                    }
                },
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.closeDialoge();
                    }
                }

        );

        // ----------------------------------------

        return new WindowContentBuilder(layout, options).build();
    }

    ResourceAdapterPresenter getPresenter() {
        return presenter;
    }
}
