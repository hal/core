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
package org.jboss.as.console.client.v3.behaviour;

import org.useware.kernel.gui.behaviour.FilteringStatementContext;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class SelectionFilteringStatementContext extends FilteringStatementContext {

    public static final String SELECTED_ENTITY =  "selected.entity";

    public SelectionFilteringStatementContext(final StatementContext delegate, final SelectionAwareContext viewEditor) {
        this(null, delegate, viewEditor);
    }
    
    public SelectionFilteringStatementContext(String SELECTED_ENTITY, final StatementContext delegate, final SelectionAwareContext viewEditor) {
        super(delegate, new Filter() {
            @Override
            public String filter(String key) {
                if (SELECTED_ENTITY.equals(key) && viewEditor.getSelection() != null) {
                    return viewEditor.getSelection();
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
