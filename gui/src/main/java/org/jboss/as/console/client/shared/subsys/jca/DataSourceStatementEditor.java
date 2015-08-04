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
package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.widgets.forms.FormEditor;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Harald Pehl
 */
public class DataSourceStatementEditor<T extends DataSource> extends FormEditor<T> {

    public DataSourceStatementEditor(FormToolStrip.FormCallback<T> callback, boolean isXa) {
        super(isXa ? XADataSource.class : DataSource.class);

        ModelNode helpAddress = Baseadress.get();
        helpAddress.add("subsystem", "datasources");
        if (isXa) {
            helpAddress.add("xa-data-source", "*");
        } else {
            helpAddress.add("data-source", "*");
        }
        setCallback(callback);
        setHelpAddress(helpAddress);
    }

    @Override
    public Widget asWidget() {
        ComboBoxItem trackStatements = new ComboBoxItem("trackStatements", "Track Statements");
        trackStatements.setValueMap(new String[]{"true", "false", "nowarn"});
        CheckBoxItem shareStatements = new CheckBoxItem("sharePreparedStatements", "Share Prepared Statements");

        NumberBoxItem statementCacheSize = new NumberBoxItem("prepareStatementCacheSize", "Statement Cache Size");
        statementCacheSize.setRequired(false);

        getForm().setFields(trackStatements, shareStatements, statementCacheSize);

        return super.asWidget();
    }
}
