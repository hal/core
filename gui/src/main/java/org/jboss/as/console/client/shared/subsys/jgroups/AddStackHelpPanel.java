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
package org.jboss.as.console.client.shared.subsys.jgroups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class AddStackHelpPanel {

    final static Resources RESOURCES = GWT.create(Resources.class);

    public static StaticHelpPanel helpStep1() {
        return new StaticHelpPanel(new SafeHtmlBuilder().appendHtmlConstant(RESOURCES.helpStep1().getText()).toSafeHtml());
    }

    public static StaticHelpPanel helpStep2() {
        return new StaticHelpPanel(new SafeHtmlBuilder().appendHtmlConstant(RESOURCES.helpStep2().getText()).toSafeHtml());
    }



    interface Resources extends ClientBundle {

        @Source("help_step1.html")
        TextResource helpStep1();

        @Source("help_step2.html")
        TextResource helpStep2();
    }
}