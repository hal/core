/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.administration.audit;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.widgets.Code;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
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
        table.addColumn(accessColumn, "Access");
        table.addColumn(remoteAddressColumn, "Remote Address");

        // basic attributes
        Form<AuditLogItem> basicsForm = new Form<AuditLogItem>(AuditLogItem.class);
        TextItem dateField = new TextItem("date", Console.CONSTANTS.common_label_date());
        TextItem userField = new TextItem("user", Console.CONSTANTS.common_label_user());
        TextItem accessField = new TextItem("access", "Access");
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
        forms.addStyleName("master_detail-detail");
        forms.add(basicsPanel, Console.CONSTANTS.common_label_attributes());
        forms.add(operationsPanel, Console.CONSTANTS.common_label_operations());
        forms.selectTab(0);

        // update operations upon selection
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                code.clear();
                AuditLogItem item = selectionModel.getSelectedObject();
                if (item != null) {
                    JSONArray jsonArray = JSONParser.parseStrict(item.getOperations().getPayload()).isArray();
                    if (jsonArray != null) {
                        String stringify = stringify(jsonArray.getJavaScriptObject());
                        code.setValue(SafeHtmlUtils.fromString(stringify));
                    }
                }
            }
        });

        // setup layout
        VerticalPanel main = new VerticalPanel();
        main.setStyleName("rhs-content-panel");
        main.add(new ContentHeaderLabel("Audit Log"));
        main.add(new ContentDescription(Console.CONSTANTS.administration_audit_log_desc()));
        main.add(table);
        main.add(pager);
        main.add(forms);

        ScrollPanel scroll = new ScrollPanel(main);
        LayoutPanel layout = new LayoutPanel();
        layout.add(scroll);
        layout.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 100, Style.Unit.PCT);

        DefaultTabLayoutPanel root = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        root.addStyleName("default-tabpanel");
        root.add(layout, "Audit Log");
        root.selectTab(0);
        return root;
    }

    private native String stringify(JavaScriptObject json) /*-{
        return JSON.stringify(json, null, 2);
    }-*/;
}
