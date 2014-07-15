package org.jboss.as.console.client.shared.subsys.tx;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.subsys.tx.model.TransactionManager;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;

import static org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter.JacorbState;
import static org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter.JacorbState.VALID;

/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class TransactionView extends SuspendableViewImpl implements TransactionPresenter.MyView {

    private TransactionPresenter presenter = null;
    private TXModelForm defaultForm;
    private TXModelForm pathForm;
    private TXModelForm processIDForm;
    private TXModelForm recoveryForm;


    @Override
    public void setPresenter(TransactionPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel("Transactions");
        layout.add(titleBar);

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("rhs-content-panel");

        ScrollPanel scroll = new ScrollPanel(panel);
        layout.add(scroll);

        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(scroll, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        panel.add(new ContentHeaderLabel("Transaction Manager"));
        panel.add(new ContentDescription(Console.CONSTANTS.subys_tx_desc()));

        // -----

        NumberBoxItem defaultTimeout = new NumberBoxItem("defaultTimeout", "Default Timeout");
        CheckBoxItem enableStatistics = new CheckBoxItem("enableStatistics", "Enable Statistics");
        CheckBoxItem enableTsm = new CheckBoxItem("enableTsmStatus", "Enable TSM Status");

        final CheckBoxItem jts = new CheckBoxItem("jts", "Enable JTS") {
            @Override
            public String getErrMessage() {
                return super.getErrMessage();
            }
        };
        TextBoxItem nodeId = new TextBoxItem("nodeIdentifier", "Node Identifier");

        final CheckBoxItem processIdUUID = new CheckBoxItem("processIdUUID", "Process ID UUID?");
        final TextBoxItem processIdSocket = new TextBoxItem("processIdSocketBinding", "Process ID Socket") {
            @Override
            public String getErrMessage() {
                return "Invalid input: no whitespace, no special, required if UUID is un-checked";
            }
        };
        NumberBoxItem processIdPortMax = new NumberBoxItem("processIdMaxPorts", "Max Ports");

        CheckBoxItem useHornetq = new CheckBoxItem("hornetqStore", "Use HornetQ Store?");

        TextBoxItem path = new TextBoxItem("path", "Path");
        TextBoxItem relativeTo = new TextBoxItem("relativeTo", "Relative To");
        TextBoxItem objectStorePath = new TextBoxItem("objectStorePath", "Object Store Path");
        TextBoxItem objectStorePathRelativeTo = new TextBoxItem("objectStoreRelativeTo", "Object Store Relative To");

        CheckBoxItem recoveryListener = new CheckBoxItem("recoveryListener", "Recovery Listener");
        TextBoxItem socketBinding = new TextBoxItem("socketBinding", "Socket Binding");
        TextBoxItem statusSocketBinding = new TextBoxItem("statusSocketBinding", "Status Socket Binding");

        //  ---

        defaultForm = new TXModelForm(presenter, enableStatistics, enableTsm, jts, useHornetq, defaultTimeout, nodeId) {
            @Override
            protected FormValidation validateTx(final FormValidation formValidation) {
                JacorbState jacorbState = presenter.getJacorbState();

                if (jts.getValue() && jacorbState != VALID) {
                    getFormValidationError().setText(jacorbState.getMessage());
                    getFormValidationError().setVisible(true);
                    formValidation.addError(jts.getName());
                    return formValidation;
                }
                getFormValidationError().setVisible(false);
                return super.validateTx(formValidation);
            }
        };
        pathForm = new TXModelForm(presenter, path, relativeTo, objectStorePath, objectStorePathRelativeTo);
        processIDForm = new TXModelForm(presenter, processIdUUID, processIdSocket, processIdPortMax) {
            @Override
            protected FormValidation validateTx(final FormValidation formValidation) {
                if (!processIdUUID.getValue() && (processIdSocket.getValue() == null || processIdSocket.getValue()
                        .equals(""))) {
                    processIdSocket.setErroneous(true);
                    formValidation.addError(processIdSocket.getName());
                    return formValidation;
                } else {
                    return super.validateTx(formValidation);
                }
            }
        };
        recoveryForm = new TXModelForm(presenter, socketBinding, statusSocketBinding, recoveryListener);

        //panel.add(defaultForm.asWidget());

        TabPanel tabs = new TabPanel();
        tabs.setStyleName("default-tabpanel");
        tabs.getElement().setAttribute("style", "margin-top:15px;");

        tabs.add(defaultForm.asWidget(), "Common");
        tabs.add(processIDForm.asWidget(), "Process ID");
        tabs.add(recoveryForm.asWidget(), "Recovery");
        tabs.add(pathForm.asWidget(), "Path");

        tabs.selectTab(0);

        panel.add(tabs);

        return layout;
    }

    @Override
    public void setTransactionManager(TransactionManager tm) {
        defaultForm.edit(tm);
        pathForm.edit(tm);
        processIDForm.edit(tm);
        recoveryForm.edit(tm);
    }
}
