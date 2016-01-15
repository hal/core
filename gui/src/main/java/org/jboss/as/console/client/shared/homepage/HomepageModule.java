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

import elemental.dom.Element;
import org.jboss.as.console.client.v3.elemento.Elements;
import org.jboss.as.console.client.v3.elemento.IsElement;

public class HomepageModule implements IsElement {

    static final HomepageModule EMPTY = new HomepageModule("#empty", "", "n/a", "n/a", null);

    private final Element root;

    public HomepageModule(final String token, final String image, final String header,
            final String subHeader, final HomepageSection... sections) {
        // @formatter:off
        Elements.Builder builder = new Elements.Builder()
            .div().css("eap-home-col")
                .div().css("eap-home-module")
                    .div().css("eap-home-module-icon")
                        .add("img").attr("src", image)
                    .end()
                    .div().css("eap-home-module-container").rememberAs("container")
                        .div().css("eap-home-module-header")
                            .h(2).a().attr("href", "#" + token).textContent(header).end().end()
                            .p().textContent(subHeader).end()
                        .end()
                    .end()
                .end()
            .end();
        // @formatter:on

        if (sections != null) {
            Element container = builder.referenceFor("container");
            for (HomepageSection section : sections) {
                container.appendChild(section.asElement());
            }
        }
        this.root = builder.build();
    }

    @Override
    public Element asElement() {
        return root;
    }
}
