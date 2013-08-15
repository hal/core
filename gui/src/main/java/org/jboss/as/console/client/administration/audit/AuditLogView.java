package org.jboss.as.console.client.administration.audit;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.widgets.Code;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

/**
 * @author Harald Pehl
 */
public class AuditLogView extends SuspendableViewImpl implements AuditLogPresenter.MyView {

    private final BeanFactory beanFactory;

    @Inject
    public AuditLogView(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setPresenter(final AuditLogPresenter presenter) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget createWidget() {

        // table
        DefaultCellTable<AuditLogItem> table = new DefaultCellTable<AuditLogItem>(6,
                new AuditLogItemKeyProvider());
        AuditLogItemDataProvider dataProvider = new AuditLogItemDataProvider(beanFactory);
        dataProvider.addDataDisplay(table);
        final SingleSelectionModel<AuditLogItem> selectionModel = new SingleSelectionModel<AuditLogItem>();
        table.setSelectionModel(selectionModel);
        table.setRowCount(dataProvider.store.size(), true);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        // columns
        TextColumn<AuditLogItem> dateColumn = new TextColumn<AuditLogItem>() {
            @Override
            public String getValue(final AuditLogItem item) {
                return item.getDate();
            }
        };
        TextColumn<AuditLogItem> userColumn = new TextColumn<AuditLogItem>() {
            @Override
            public String getValue(final AuditLogItem item) {
                return item.getUser() == null ? "" : item.getUser();
            }
        };
        TextColumn<AuditLogItem> accessColumn = new TextColumn<AuditLogItem>() {
            @Override
            public String getValue(final AuditLogItem item) {
                return item.getAccess() == null ? "" : item.getAccess();
            }
        };
        TextColumn<AuditLogItem> remoteAddressColumn = new TextColumn<AuditLogItem>() {
            @Override
            public String getValue(final AuditLogItem item) {
                return item.getRemoteAddress() == null ? "" : item.getRemoteAddress();
            }
        };
        table.addColumn(dateColumn, Console.CONSTANTS.common_label_date());
        table.addColumn(userColumn, Console.CONSTANTS.common_label_user());
        table.addColumn(accessColumn, Console.CONSTANTS.administration_access());
        table.addColumn(remoteAddressColumn, "Remote Address");

        // basic attributes
        Form<AuditLogItem> basicsForm = new Form<AuditLogItem>(AuditLogItem.class);
        TextItem dateField = new TextItem("date", Console.CONSTANTS.common_label_date());
        TextItem userField = new TextItem("user", Console.CONSTANTS.common_label_user());
        TextItem accessField = new TextItem("access", Console.CONSTANTS.administration_access());
        TextItem domainUUIDField = new TextItem("domainUUID", "Domain UUID");
        TextItem remoteAddressField = new TextItem("remote-address", "Remote Address");
        CheckBoxItem booting = new CheckBoxItem("booting", "Booting");
        CheckBoxItem readOnly = new CheckBoxItem("r/o", "Read-only");
        CheckBoxItem success = new CheckBoxItem("success", "Success");
        basicsForm.setFields(dateField, userField, accessField, domainUUIDField, remoteAddressField, booting, readOnly,
                success);
        basicsForm.setEnabled(false);
        basicsForm.bind(table);
        VerticalPanel basicsPanel = new VerticalPanel();
        basicsPanel.setStyleName("fill-layout-width");
        basicsPanel.add(new AuditHelpPanel().asWidget());
        basicsPanel.add(basicsForm);

        // operations
        VerticalPanel operationsPanel = new VerticalPanel();
        operationsPanel.setStyleName("fill-layout-width");
        final Code code = new Code(Code.Language.JAVASCRIPT, false);
        operationsPanel.add(code);

        // form tabs
        TabPanel forms = new TabPanel();
        forms.setStyleName("default-tabpanel");
        forms.add(basicsPanel, Console.CONSTANTS.common_label_attributes());
        forms.add(operationsPanel, Console.CONSTANTS.common_label_operations());
        forms.selectTab(0);

        // update operations upon selection
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                code.clear();
                AuditLogItem item = selectionModel.getSelectedObject();
                JSONArray jsonArray = JSONParser.parseStrict(item.getOperations().getPayload()).isArray();
                if (jsonArray != null) {
                    String stringify = stringify(jsonArray.getJavaScriptObject());
                    code.setValue(SafeHtmlUtils.fromString(stringify));
                }
            }
        });

        // setup layout
        SimpleLayout layout = new SimpleLayout()
                .setTitle(Console.CONSTANTS.administration_audit())
                .setHeadline(Console.CONSTANTS.administration_audit_log())
                .setDescription(Console.CONSTANTS.administration_audit_log_desc())
                .addContent("table", table)
                .addContent("pager", pager)
                .addContent("forms", forms);
        return layout.build();
    }

    private native String stringify(JavaScriptObject json) /*-{
        return JSON.stringify(json, null, 2);
    }-*/;
}
