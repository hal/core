package org.jboss.as.console.client.administration.audit;

import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import org.jboss.as.console.client.shared.BeanFactory;

/**
 * @author Harald Pehl
 */
public class AuditLogItemDataProvider extends AsyncDataProvider<AuditLogItem> {

    final static Resources RESOURCES = GWT.create(Resources.class);
    final static RegExp ITEMS = RegExp.compile("^\\}", "gm");
    final static RegExp ITEM = RegExp.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) - (\\{$(.|\n)*^\\})", "m");
    static long idCounter = 0;
    final BeanFactory beanFactory;
    final List<AuditLogItem> store;

    public AuditLogItemDataProvider(BeanFactory beanFactory) {
        super(new AuditLogItemKeyProvider());
        this.beanFactory = beanFactory;
        this.store = new ArrayList<AuditLogItem>();

        parseItems();
    }

    private void parseItems() {
        idCounter = 0;
        SplitResult result = ITEMS.split(RESOURCES.auditLog().getText());
        for (int i = 0; i < result.length(); i++) {
            String nextResult = result.get(i);
            if (nextResult != null && nextResult.length() != 0) {
                String itemText = nextResult.trim() + "\n}";
                MatchResult match = ITEM.exec(itemText);
                if (match != null) {
                    store.add(parseItem(match));
                }
            }
        }
    }

    private AuditLogItem parseItem(final MatchResult match) {
        String date = match.getGroup(1);
        AutoBean<AuditLogItem> itemBean = AutoBeanCodex.decode(beanFactory, AuditLogItem.class, match.getGroup(2));
        AuditLogItem item = itemBean.as();
        item.setId(idCounter++);
        item.setDate(date);
        return item;
    }

    @Override
    protected void onRangeChanged(final HasData<AuditLogItem> display) {
        Range range = display.getVisibleRange();
        int start = min(range.getStart(), store.size() - 1);
        int end = min(start + range.getLength(), store.size());
        List<AuditLogItem> items = store.subList(start, end);
        display.setRowData(start, items);
    }

    interface Resources extends ClientBundle {

        @Source("audit-log.log")
        TextResource auditLog();
    }
}
