package org.jboss.as.console.client.shared.subsys.jca;

import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.widgets.forms.FormEditor;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.dmr.client.ModelNode;

import com.google.gwt.user.client.ui.Widget;

/**
 * @author Philippe Marschall
 * @date 04/06/14
 */
public class DataSourceTimeoutEditor<T extends DataSource> extends FormEditor<T>{

    private final boolean isXa;

    public DataSourceTimeoutEditor(FormToolStrip.FormCallback<T> callback, boolean isXa) {

        super(isXa ? XADataSource.class : DataSource.class);
        this.isXa = isXa;

        ModelNode helpAddress = Baseadress.get();
        helpAddress.add("subsystem", "datasources");
        if (isXa) {
            helpAddress.add("xa-data-source", "*");
        } else {
            helpAddress.add("data-source", "*");
        }

        setCallback(callback);
        setHelpAddress(helpAddress);
    }

    @Override
    public Widget asWidget() {

        NumberBoxItem useTryLock = new NumberBoxItem("useTryLock", "Use tryLock()") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };

        NumberBoxItem blockingTimeoutMillis  = new NumberBoxItem("blockingTimeoutWaitMillis", "Blocking Timeout Millis") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };

        NumberBoxItem idleTimeoutMinutes   = new NumberBoxItem("idleTimeoutMinutes", "Idle Timeout Minutes") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };

        CheckBoxItem setTxQueryTimeout = new CheckBoxItem("setTxQueryTimeout", "Set Tx Query Timeout") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };

        NumberBoxItem queryTimeout = new NumberBoxItem("queryTimeout", "Query Timeout") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };

        NumberBoxItem allocationRetry  = new NumberBoxItem("allocationRetry", "Allocation Retry") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };

        NumberBoxItem allocationRetryWaitMillis  = new NumberBoxItem("allocationRetryWaitMillis", "Allocation Retry Wait Millis") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };

        if (this.isXa) {

            NumberBoxItem xaResourceTimeout   = new NumberBoxItem("xaResourceTimeout", "XA Resource Timeout") {
                @Override
                public boolean isRequired() {
                    return false;
                }
            };
            getForm().setFields(
                    useTryLock,
                    blockingTimeoutMillis, idleTimeoutMinutes,
                    setTxQueryTimeout, queryTimeout,
                    allocationRetry, allocationRetryWaitMillis, xaResourceTimeout);

        } else {
            getForm().setFields(
                    useTryLock,
                    blockingTimeoutMillis, idleTimeoutMinutes,
                    setTxQueryTimeout, queryTimeout,
                    allocationRetry, allocationRetryWaitMillis);
        }

        return super.asWidget();
    }

}
