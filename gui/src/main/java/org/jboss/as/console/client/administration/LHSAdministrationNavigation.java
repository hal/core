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
package org.jboss.as.console.client.administration;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.ballroom.client.layout.LHSNavTree;
import org.jboss.ballroom.client.layout.LHSNavTreeItem;
import org.jboss.ballroom.client.layout.LHSTreeSection;

/**
 * @author Harald Pehl
 */
class LHSAdministrationNavigation {

    private ScrollPanel scroll;

    public LHSAdministrationNavigation() {



//        LHSNavTreeItem auditLogItem = new LHSNavTreeItem("Audit Log", NameTokens.AuditLogPresenter);
//        accessLeaf.addItem(auditLogItem);

        LHSNavTree patchingTree = new LHSNavTree("Patching");
        patchingTree.addItem(new LHSNavTreeItem("Patch Management", NameTokens.PatchingPresenter));

        LHSNavTree accessTree = new LHSNavTree("administration");
        accessTree.getElement().setAttribute("aria-label", "Administration");
        accessTree.expandTopLevel();

        LHSNavTreeItem authItem = new LHSNavTreeItem("Role Assignment", NameTokens.RoleAssignmentPresenter);
        accessTree.addItem(authItem);

        VerticalPanel stack = new VerticalPanel();
        stack.setStyleName("fill-layout-width");
        stack.getElement().getStyle().setBackgroundColor("#ffffff");

        HTML accessTitle = new HTML("Access Control");
        accessTitle.setStyleName("server-picker-section-header");

        stack.add(accessTitle);
        stack.add(accessTree);


        HTML patchingTitle = new HTML("Patching");
        patchingTitle.setStyleName("server-picker-section-header");

        stack.add(patchingTitle);
        stack.add(patchingTree);

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");
        layout.add(stack);

        scroll = new ScrollPanel(layout);
    }

    public Widget asWidget() {
        return scroll;
    }
}
