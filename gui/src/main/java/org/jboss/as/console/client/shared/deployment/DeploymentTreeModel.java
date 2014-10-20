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
package org.jboss.as.console.client.shared.deployment;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import org.jboss.as.console.client.shared.deployment.model.DeploymentData;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.shared.deployment.model.DeploymentSubsystemElement;
import org.jboss.ballroom.client.widgets.icons.Icons;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class DeploymentTreeModel implements TreeViewModel {
    static final DeploymentTemplates DEPLOYMENT_TEMPLATES = GWT.create(DeploymentTemplates.class);
    private final DeploymentNodeInfoFactory nodeInfoFactory;
    private final ListDataProvider<DeploymentRecord> deploymentDataProvider;
    private final DefaultNodeInfo<DeploymentRecord> level0;
    private final DeploymentFilter filter;


    public DeploymentTreeModel(final DeploymentBrowser deploymentBrowser, final DeploymentStore deploymentStore,
                               final SingleSelectionModel<DeploymentRecord> selectionModel) {
        this.nodeInfoFactory = new DeploymentNodeInfoFactory(deploymentBrowser, deploymentStore);
        this.deploymentDataProvider = new ListDataProvider<DeploymentRecord>(selectionModel.getKeyProvider());
        this.level0 = new DefaultNodeInfo<DeploymentRecord>(deploymentDataProvider, new MainDeploymentCell(deploymentBrowser),
                selectionModel, null);

        this.filter = new DeploymentFilter(deploymentDataProvider);

    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(final T value) {
        if (value == null) {
            return level0;
        } else {
            return nodeInfoFactory.nodeInfoFor((DeploymentData) value);
        }
    }

    @Override
    public boolean isLeaf(final Object value) {
        return value instanceof DeploymentSubsystemElement;
    }

    public void updateDeployments(List<DeploymentRecord> deployments) {
        filter.reset();
        deploymentDataProvider.getList().clear();
        deploymentDataProvider.getList().addAll(deployments);
    }

    public List<DeploymentRecord> getDeployments() {
        return deploymentDataProvider.getList();
    }

    public DeploymentFilter getFilter() {
        return filter;
    }


    interface DeploymentTemplates extends SafeHtmlTemplates {
        @Template(
                "<div title=\"{2}\" style=\"padding-right:20px;position:relative;zoom:1;\"><div>{0}</div><div title=\"{2}\" style=\"margin-top:-9px;position:absolute;top:50%;right:0;line-height:0px;\">{1}</div></div>")
        SafeHtml deployment(String deployment, SafeHtml icon, String title);
    }


    static class MainDeploymentCell extends DeploymentDataCell<DeploymentRecord> {
        MainDeploymentCell(final DeploymentBrowser deploymentBrowser) {
            super(deploymentBrowser);
        }

        @Override
        public void render(final Context context, final DeploymentRecord value, final SafeHtmlBuilder sb) {
            ImageResource res;
            String name = value.getName().length() > 30 ? value.getName().substring(0, 25) + " ..." : value.getName();
            String iconTitle = name;
            if ("FAILED".equalsIgnoreCase(value.getStatus())) {
                res = Icons.INSTANCE.status_warn();
                iconTitle += " failed to start, check log for details.";
            } else if (value.isEnabled()) {
                res = Icons.INSTANCE.status_good();
                iconTitle += " is started.";
            } else {
                res = Icons.INSTANCE.status_bad();
                iconTitle += " is disabled.";
            }
            AbstractImagePrototype proto = AbstractImagePrototype.create(res);
            SafeHtml imageHtml = SafeHtmlUtils.fromTrustedString(proto.getHTML());
            sb.append(DEPLOYMENT_TEMPLATES.deployment(name, imageHtml, iconTitle));
        }
    }
}
