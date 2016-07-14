/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.shared.hosts;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.DisposableViewImpl;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Claudio Miranda
 */
public class ConfigurationChangesView extends DisposableViewImpl implements ConfigurationChangesPresenter.MyView {

    private ConfigurationChangesEditor editor;

    public ConfigurationChangesView() {
        this.editor = new ConfigurationChangesEditor();
    }

    @Override
    public Widget createWidget() {
        return editor.asWidget();
    }

    @Override
    public void setPresenter(ConfigurationChangesPresenter presenter) {
        editor.setPresenter(presenter);
    }

    @Override
    public void setChanges(final List<ModelNode> changes) {
        editor.updateChanges(changes);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        editor.setEnabled(enabled);
    }
}
