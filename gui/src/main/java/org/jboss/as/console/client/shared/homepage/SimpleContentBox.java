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
package org.jboss.as.console.client.shared.homepage;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple content box with a header, a static html body and a link.
 *
 * @author Harald Pehl
 */
public class SimpleContentBox implements ContentBox {

    private final String title;
    private final SafeHtml body;
    private final String linkTarget;
    private final String linkTitle;


    public SimpleContentBox(final String title, final SafeHtml body, final String linkTitle, final String linkTarget) {

        this.title = title;
        this.body = body;
        this.linkTitle = linkTitle;
        this.linkTarget = linkTarget;
    }

    @Override
    public Widget asWidget() {
        FlowPanel content = new FlowPanel();
        content.add(new Label(title));
        content.add(new HTML(body));
        content.add(new Hyperlink(linkTitle, linkTarget));
        return content;
    }
}
