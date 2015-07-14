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
package org.jboss.as.console.client.shared.subsys.activemq;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSEndpoint;

import java.util.Iterator;
import java.util.List;

/**
 * @author Harald Pehl
 */
public class JMSEndpointJndiColumn<T extends ActivemqJMSEndpoint> extends Column<T, SafeHtml> {

    interface Templates extends SafeHtmlTemplates {

        @Template("<span title=\"{1}\">{0}</span>")
        SafeHtml shortcut(String shortName, String fullName);
    }


    final static Templates TEMPLATES = GWT.create(Templates.class);
    final static int DEFAULT_LENGTH = 60;

    private final int maxLength;

    public JMSEndpointJndiColumn() {
        this(DEFAULT_LENGTH);
    }

    public JMSEndpointJndiColumn(int maxLength) {
        super(new SafeHtmlCell());
        this.maxLength = maxLength;
    }

    @Override
    public SafeHtml getValue(final T endpoint) {
        String fullName = "";
        String shortName = "";
        StringBuilder builder = new StringBuilder();
        List<String> jndiNames = endpoint.getEntries();
        if (!jndiNames.isEmpty()) {
            builder.append("[");
            for (Iterator<String> iterator = jndiNames.iterator(); iterator.hasNext(); ) {
                String jndiName = iterator.next();
                builder.append(jndiName);
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append("]");

            fullName = builder.toString();
            if (fullName.length() > maxLength) {
                shortName = fullName.substring(0, maxLength - 4) + "...]";
            } else {
                shortName = fullName;
            }
        }
        return TEMPLATES.shortcut(shortName, fullName);
    }
}
