/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.administration.accesscontrol.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.tools.SSOUtils;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class SSOAccessControlView extends SuspendableViewImpl implements SSOAccessControlPresenter.MyView {

    @Override
    public Widget createWidget() {

        VerticalPanel panel = new VerticalPanel();
        panel.add(new ContentHeaderLabel("Access Control"));
        
        ContentDescription description = new ContentDescription(Console.CONSTANTS.sso_access_control_description());

        String text = "<span title=\"" + Console.CONSTANTS.sso_access_control_service_title()
                + "\">" + Console.CONSTANTS.sso_access_control_service_title() + "</span>: <a href=\"" 
                + BootstrapContext.retrieveSsoAuthUrl() + "\" target=\"_blank\">" + BootstrapContext.retrieveSsoAuthUrl() 
                + "</a>";

        Anchor userProfileAnchor = new Anchor(Console.CONSTANTS.sso_access_control_user_profile());
        userProfileAnchor.addClickHandler(clickEvent -> {
            String userMgmt = SSOUtils.getSsoUserManagementUrl();
            Window.Location.replace(userMgmt);
        });
        
        ContentDescription ssoLink = new ContentDescription(text);
        panel.add(description);
        panel.add(userProfileAnchor);
        panel.add(ssoLink);

        VerticalPanel outerPanel = new VerticalPanel();
        outerPanel.setStyleName("rhs-content-panel");
        outerPanel.add(panel);

        DefaultTabLayoutPanel tabLayoutPanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutPanel.addStyleName("default-tabpanel");
        tabLayoutPanel.add(outerPanel, "Access Control");
        return tabLayoutPanel;
    }

}
