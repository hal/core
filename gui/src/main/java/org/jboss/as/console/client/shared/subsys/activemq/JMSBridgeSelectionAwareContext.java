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
package org.jboss.as.console.client.shared.subsys.activemq;

import org.useware.kernel.gui.behaviour.FilteringStatementContext;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * Special Filtering for the case when given a table with selectable itens, each item may contain resolveable action
 * at later time (after the widget is created)
 * 
 * @author Claudio Miranda
 */
class JMSBridgeSelectionAwareContext extends FilteringStatementContext {

    private final static String SELECTED_ENTITY_KEY = "selected.entity";
    final static String SELECTED_ENTITY = "{" + SELECTED_ENTITY_KEY + "}";

    public JMSBridgeSelectionAwareContext(final StatementContext delegate, final JMSBridgeList jmsBridge) {
        super(delegate, new Filter() {
            @Override
            public String filter(String key) {
                if (SELECTED_ENTITY_KEY.equals(key) && jmsBridge.getSelectedEntity() != null) {
                    return jmsBridge.getSelectedEntity().getName();
                }
                return "*";
            }

            @Override
            public String[] filterTuple(String key) {
                return null;
            }
        });
    }
    
}
