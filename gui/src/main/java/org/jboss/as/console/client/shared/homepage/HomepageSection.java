/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.homepage;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import org.jboss.as.console.client.v3.elemento.Elements;
import org.jboss.as.console.client.v3.elemento.IsElement;

/**
 * @author Harald Pehl
 */
public class HomepageSection implements IsElement {

    private final Element root;
    private final Element icon;
    private final Element body;
    private boolean collapsed;

    public HomepageSection(final String token, final String header, final String intro, final String... steps) {
        // @formatter:off
        Elements.Builder builder = new Elements.Builder()
            .div()
                .div().css("eap-toggle-controls")
                    .a().css("clickable").rememberAs("toggle")
                        .start("i").css("icon-angle-down").rememberAs("icon").end()
                        .span().textContent(" " + header).end()
                    .end()
                    .a().attr("href", "#" + token)
                        .span().textContent("Start ").end()
                        .start("i").css("icon-circle-arrow-right").end()
                    .end()
                .end()
                .div().css("eap-toggle-container").rememberAs("body")
                    .p().textContent(intro).end()
                    .ol().rememberAs("steps").end()
                .end()
            .end();
        // @formatter:on
        wireToggle(builder.referenceFor("toggle"));

        Document document = Browser.getDocument();
        Element ol = builder.referenceFor("steps");
        for (String step : steps) {
            Element li = document.createLIElement();
            li.setInnerHTML(step);
            ol.appendChild(li);
        }

        this.icon = builder.referenceFor("icon");
        this.body = builder.referenceFor("body");
        this.root = builder.build();
        this.collapsed = false;
    }

    public void toggle() {
        if (collapsed) {
            Elements.setVisible(body, true);
            icon.getClassList().remove("icon-angle-right");
            icon.getClassList().add("icon-angle-down");
        } else {
            Elements.setVisible(body, false);
            icon.getClassList().remove("icon-angle-down");
            icon.getClassList().add("icon-angle-right");
        }
        this.collapsed = !collapsed;
    }

    @Override
    public Element asElement() {
        return root;
    }

    native void wireToggle(Element element) /*-{
        var that = this;
        element.onclick = function() {
            that.@org.jboss.as.console.client.shared.homepage.HomepageSection::toggle()();
        };
    }-*/;
}
