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

        LHSTreeSection accessLeaf = new LHSTreeSection("Access Control", true);
        LHSNavTreeItem authItem = new LHSNavTreeItem("Role Assignment", NameTokens.RoleAssignmentPresenter);
        accessLeaf.addItem(authItem);
//        LHSNavTreeItem auditLogItem = new LHSNavTreeItem("Audit Log", NameTokens.AuditLogPresenter);
//        accessLeaf.addItem(auditLogItem);

        LHSTreeSection patchingLeaf = new LHSTreeSection("Patching", true);
        patchingLeaf.addItem(new LHSNavTreeItem("Patch Management", NameTokens.PatchingPresenter));

        LHSNavTree navigation = new LHSNavTree("administration");
        navigation.getElement().setAttribute("aria-label", "Administration");
        navigation.addItem(accessLeaf);
        navigation.addItem(patchingLeaf);
        navigation.expandTopLevel();

        VerticalPanel stack = new VerticalPanel();
        stack.setStyleName("fill-layout-width");
        stack.add(navigation);

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");
        layout.add(stack);

        scroll = new ScrollPanel(layout);
    }

    public Widget asWidget() {
        return scroll;
    }
}
