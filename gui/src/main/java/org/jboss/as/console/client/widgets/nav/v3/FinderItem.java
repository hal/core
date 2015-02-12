package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.user.client.Command;

public class FinderItem {

    private String title;
    private Command cmd;
    private boolean isFolder;

    public FinderItem(String title, Command cmd, boolean isFolder) {
        this.title = title;
        this.cmd = cmd;
        this.isFolder = isFolder;
    }

    public String getTitle() {
        return title;
    }

    public Command getCmd() {
        return cmd;
    }

    public boolean isFolder() {
        return isFolder;
    }
}