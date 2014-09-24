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
package org.jboss.as.console.client.administration.authorization;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Harald Pehl
 */
public class AuthorizationView extends SuspendableViewImpl implements AuthorizationPresenter.MyView {

    private final SecurityFramework securityFramework;
    private AuthorizationPresenter presenter;
    private AuthorizationPanel authorizationPanel;

    @Inject
    public AuthorizationView(SecurityFramework securityFramework) {
        this.securityFramework = securityFramework;
    }

    @Override
    public void setPresenter(AuthorizationPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        authorizationPanel = new AuthorizationPanel(AuthorizationPresenter.AUTHORIZATION_ADDRESS, securityContext,
                presenter);

        OneToOneLayout layout = new OneToOneLayout()
                .setTitle("Authorization")
                .setHeadline("Authorization")
                .setDescription(Console.MESSAGES.administration_authorization_desc())
                .setDetail(null, authorizationPanel);
        return layout.build();
    }

    @Override
    public void updateFrom(ModelNode data) {
        authorizationPanel.setData(data);
    }

    @Override
    public ModelNode currentValues() {
        return authorizationPanel.getCurrentData();
    }
}
