package org.jboss.as.console.client.shared.general;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;

/**
 * @author Heiko Braun
 * @date 2/27/12
 */
public class EnvironmentProperties {

    private ListDataProvider<PropertyRecord> propertyProvider;
    private DefaultCellTable<PropertyRecord> propertyTable;
    private List<PropertyRecord> origValues = Collections.EMPTY_LIST;

    public Widget asWidget() {
        propertyTable = new DefaultCellTable<PropertyRecord>(8, new ProvidesKey<PropertyRecord>() {
            @Override
            public Object getKey(PropertyRecord item) {
                return item.getKey();
            }
        });

        propertyProvider = new ListDataProvider<PropertyRecord>();
        propertyProvider.addDataDisplay(propertyTable);


        ColumnSortEvent.ListHandler<PropertyRecord> sortHandler =
                new ColumnSortEvent.ListHandler<PropertyRecord>(propertyProvider.getList());

        // Create columns
        Column<PropertyRecord, String> keyColumn = new Column<PropertyRecord, String>(
                new TextCell()) {

            @Override
            public String getValue(PropertyRecord object) {
                return object.getKey();
            }

        };
        keyColumn.setSortable(true);
        sortHandler.setComparator(keyColumn, new Comparator<PropertyRecord>() {
            @Override
            public int compare(PropertyRecord o1, PropertyRecord o2) {
                return o1.getKey().toLowerCase().compareTo(o2.getKey().toLowerCase());
            }
        });


        Column<PropertyRecord, SafeHtml> valueColumn = new Column<PropertyRecord, SafeHtml>(
                new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(PropertyRecord object) {
                String val = object.getValue();
                return new SafeHtmlBuilder().appendHtmlConstant("<span title='" +
                        new SafeHtmlBuilder().appendEscaped(val).toSafeHtml().asString() + "'>" + val + "</span>").toSafeHtml();
            }
        };

        // Add the columns.
        propertyTable.addColumn(keyColumn, Console.CONSTANTS.common_label_key());
        propertyTable.addColumn(valueColumn, Console.CONSTANTS.common_label_value());

        propertyTable.addColumnSortHandler(sortHandler);
        propertyTable.getColumnSortList().push(keyColumn);

        // --

        Form<PropertyRecord> form = new Form<PropertyRecord>(PropertyRecord.class);

        TextItem name = new TextItem("key", "Name");
        TextAreaItem value = new TextAreaItem("value", "Value");
        value.setEnabled(false);

        form.setFields(name, value);
        form.setEnabled(false);

        form.bind(propertyTable);

        // --


        final TextBox filter = new TextBox();
        filter.setMaxLength(30);
        filter.setVisibleLength(20);
        filter.getElement().setAttribute("style", "float:right; width:120px;");
        filter.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                String prefix = filter.getText();
                if(prefix!=null && !prefix.equals(""))
                {
                    // filter by prefix
                    filterByPrefix(prefix);
                }
                else
                {
                    clearFilter();
                }
            }
        });

        ToolStrip toolStrip = new ToolStrip();
        final HTML label = new HTML(Console.CONSTANTS.commom_label_filter()+":&nbsp;");
        label.getElement().setAttribute("style", "padding-top:8px;");
        toolStrip.addToolWidget(label);
        toolStrip.addToolWidget(filter);

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setTitle("Environment")
                .setHeadline("Environment Properties")
                .setDescription(Console.MESSAGES.environment_description())
                .setMaster("", propertyTable)
                .setMasterTools(toolStrip.asWidget())
                .addDetail("Attributes", form.asWidget());

        return layout.build();
    }

    public void setProperties(List<PropertyRecord> environment) {

        origValues = environment;
        propertyTable.setRowCount(environment.size(), true);

        List<PropertyRecord> propList = propertyProvider.getList();
        propList.clear(); // cannot call setList() as that breaks the sort handler
        Collections.sort(environment, new Comparator<PropertyRecord>() {
            @Override
            public int compare(PropertyRecord o1, PropertyRecord o2) {
                return o1.getKey().toLowerCase().compareTo(o2.getKey().toLowerCase());
            }
        });
        propList.addAll(environment);

        // Make sure the new values are properly sorted
        ColumnSortEvent.fire(propertyTable, propertyTable.getColumnSortList());


    }

    public DefaultCellTable<PropertyRecord> getPropertyTable() {
        return propertyTable;
    }

    public void clearValues() {
        propertyProvider.setList(new ArrayList<PropertyRecord>());
    }

    public void filterByPrefix(String prefix) {

        final List<PropertyRecord> next  = new ArrayList<PropertyRecord>();
        for(PropertyRecord prop : origValues)
        {
            if(prop.getKey().toLowerCase().contains(prefix.toLowerCase()))
                next.add(prop);
        }

        List<PropertyRecord> propList = propertyProvider.getList();
        propList.clear(); // cannot call setList() as that breaks the sort handler
        propList.addAll(next);

    }

    public void clearFilter() {
        List<PropertyRecord> propList = propertyProvider.getList();
        propList.clear(); // cannot call setList() as that breaks the sort handler
        propList.addAll(origValues);
    }
}
