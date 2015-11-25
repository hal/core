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

package org.jboss.as.console.client.domain.groups;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.jvm.Jvm;
import org.jboss.as.console.client.shared.jvm.JvmEditor;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

/**
 * Shows an editable view of a single server group.
 *
 * @author Heiko Braun
 * @date 2/16/11
 */
public class ServerGroupView extends SuspendableViewImpl implements ServerGroupPresenter.MyView {

    private ServerGroupPresenter presenter;
    private VerticalPanel panel;

    private PropertyEditor propertyEditor;
    private JvmEditor jvmEditor;

    private DefaultCellTable<ServerGroupRecord> serverGroupTable;
    private ListDataProvider<ServerGroupRecord> serverGroupProvider;
    private ServerGroupDetails details;
    private String preselection;
    private HTML headline;

    @Override
    public void setPresenter(ServerGroupPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {

        details = new ServerGroupDetails(presenter);

        jvmEditor = new JvmEditor(presenter, true, true);
        jvmEditor.setAddressCallback(new FormHelpPanel.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = new ModelNode();
                address.add("server-group", "*");
                address.add("jvm", "*");
                return address;
            }
        });

        propertyEditor = new PropertyEditor(presenter, true);
        //propertyEditor.setOperationAddress("/server-group=*/system-property=*", "add");

        headline = new HTML("");
        headline.setStyleName("content-header-label");

        OneToOneLayout layout = new OneToOneLayout()
                .setTitle(Console.CONSTANTS.common_label_serverGroupConfigurations())
                .setHeadlineWidget(headline)
                .setDescription(Console.CONSTANTS.common_serverGroups_desc())
                .addDetail(Console.CONSTANTS.common_label_attributes(), details.asWidget())
                .addDetail(Console.CONSTANTS.common_label_virtualMachine(), jvmEditor.asWidget())
                .addDetail(Console.CONSTANTS.common_label_systemProperties(), propertyEditor.asWidget());

        return layout.build();
    }

    @Override
    public void updateFrom(ServerGroupRecord group) {
        // requires manual cleanup
        jvmEditor.clearValues();
        propertyEditor.clearValues();

        headline.setHTML("Server Group: "+ group.getName());
        details.updateFrom(group);

        presenter.loadJVMConfiguration(group);
        presenter.loadProperties(group);

    }

    @Override
    public void updateSocketBindings(List<String> result) {
        details.setSocketBindings(result);
    }

    @Override
    public void updateProfiles(List<ProfileRecord> result) {
        details.setProfiles(result);
    }

    private SingleSelectionModel<ServerGroupRecord> getSelectionModel() {
        return ((SingleSelectionModel<ServerGroupRecord>) serverGroupTable.getSelectionModel());
    }

    @Override
    public void setJvm(ServerGroupRecord group, Jvm jvm) {
        jvmEditor.setSelectedRecord(group.getName(), jvm);
    }

    @Override
    public void setProperties(ServerGroupRecord group, List<PropertyRecord> properties) {
        propertyEditor.setProperties(group.getName(), properties);
    }

}
