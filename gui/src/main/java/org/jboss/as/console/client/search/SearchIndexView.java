package org.jboss.as.console.client.search;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Heiko Braun
 * @date 16/01/14
 */
public class SearchIndexView {

    public Widget asWidget() {
        final TabPanel tabPanel = new TabPanel();
        tabPanel.setStyleName("default-tabpanel");

        tabPanel.add(new IndexBuilderView().asWidget(), "Index Builder");
        tabPanel.add(new SearchView().asWidget(), "Search");

        tabPanel.selectTab(0);
        return tabPanel;
    }
}
