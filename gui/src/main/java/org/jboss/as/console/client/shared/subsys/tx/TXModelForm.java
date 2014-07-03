package org.jboss.as.console.client.shared.subsys.tx;

import static com.google.gwt.dom.client.Style.Unit.PX;

import java.util.Map;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.tx.model.TransactionManager;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 8/2/12
 */
public class TXModelForm {

    private Form<TransactionManager> form;

    private TransactionPresenter presenter;
    private FormItem[] fields;
    private Label formValidationError;

    public TXModelForm(TransactionPresenter presenter, FormItem... fields) {
        this.presenter = presenter;
        this.fields = fields;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout");


        form = new Form<TransactionManager>(TransactionManager.class) {
            @Override
            public FormValidation validate() {
                return validateTx(super.validate());
            }
        };
        form.setNumColumns(2);

        FormToolStrip<TransactionManager> toolstrip =
                new FormToolStrip<TransactionManager>(form, new FormToolStrip.FormCallback<TransactionManager>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveConfig(changeset);
                    }

                    @Override
                    public void onDelete(TransactionManager entity) {
                    }
                });
        toolstrip.providesDeleteOp(false);

        FormHelpPanel helpPanel = new FormHelpPanel(new FormHelpPanel.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = Baseadress.get();
                address.add("subsystem", "transactions");
                return address;
            }
        }, form);
        layout.add(helpPanel.asWidget());

        formValidationError = new Label("Form is invalid!");
        formValidationError.addStyleName("form-error-desc");
        formValidationError.getElement().getStyle().setLineHeight(9, PX);
        formValidationError.getElement().getStyle().setMarginBottom(5, PX);
        formValidationError.setVisible(false);
        layout.add(formValidationError.asWidget());

        form.setFields(fields);
        form.setEnabled(false);

        layout.add(form.asWidget());

        return layout;
    }

    public void edit(TransactionManager tm) {
        form.edit(tm);
    }

    public void clearValues() {
        form.clearValues();
    }

    protected FormValidation validateTx(FormValidation formValidation) {
        return formValidation;
    }

    Label getFormValidationError() {
        return formValidationError;
    }
}
