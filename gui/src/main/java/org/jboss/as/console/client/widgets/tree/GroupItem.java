package org.jboss.as.console.client.widgets.tree;

import com.google.gwt.user.client.ui.TreeItem;

public class GroupItem extends TreeItem {

    private final static String ID_PREFIX = "nav_group_item_";

    public GroupItem(String title) {
        super();

        setText(title);
        getElement().setId(ID_PREFIX + title.replace(' ', '_').toLowerCase());
        getElement().setAttribute("style", "cursor:pointer;");
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
    }
}
