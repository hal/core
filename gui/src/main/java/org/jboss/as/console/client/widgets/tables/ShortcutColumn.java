package org.jboss.as.console.client.widgets.tables;

import static org.jboss.as.console.client.StringUtils.shortenStringIfNecessary;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;

public abstract class ShortcutColumn<T> extends Column<T, SafeHtml> {

    final static Templates TEMPLATES = GWT.create(Templates.class);

    private final int maxLength;

    public ShortcutColumn(int maxLength) {
        super(new SafeHtmlCell());
        this.maxLength = maxLength;
    }

    @Override
    public SafeHtml getValue(T record) {
        return new SafeHtmlBuilder().append(asSafeHtml(getName(record))).toSafeHtml();
    }

    protected abstract String getName(T value);

    protected SafeHtml asSafeHtml(String name) {
        return (name != null && name.length() > maxLength) ? TEMPLATES
                .shortcut(shortenStringIfNecessary(name, maxLength), name) : SafeHtmlUtils.fromString(name);
    }

    interface Templates extends SafeHtmlTemplates {

        @Template("<span title=\"{1}\">{0}</span>")
        SafeHtml shortcut(String shortName, String fullName);
    }
}
