package org.jboss.as.console.client.widgets.tables;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class IconCell extends AbstractCell<String> {

    public IconCell() {
        super();
    }

    @Override
    public void render(Cell.Context context, String icon, SafeHtmlBuilder safeHtmlBuilder)
    {
        SafeHtml render;

        boolean hasValue = icon!=null && !icon.equals("");

        if (hasValue)
        {
            SafeHtmlBuilder builder = new SafeHtmlBuilder();
            builder.appendHtmlConstant("<i class='" + icon + "'></i>");
            render = builder.toSafeHtml();
        }
        else
        {
            render = new SafeHtmlBuilder().toSafeHtml();
        }

        safeHtmlBuilder.append(render);
    }
}