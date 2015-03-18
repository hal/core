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
package org.jboss.as.console.client.core.gin;

import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import org.jboss.as.console.client.core.presenter.PresenterA;
import org.jboss.as.console.client.core.presenter.PresenterB;
import org.jboss.as.console.client.core.presenter.PresenterC;
import org.jboss.as.console.client.core.presenter.PresenterD;
import org.jboss.as.console.client.core.presenter.PresenterE;
import org.jboss.as.console.client.core.presenter.PresenterF;
import org.jboss.as.console.client.core.presenter.PresenterG;
import org.jboss.as.console.spi.GinExtensionBinding;

/**
 * @author Harald Pehl
 */
@GinExtensionBinding
public class PresenterModule extends AbstractPresenterModule {

    @Override
    protected void configure() {
        bindPresenter(PresenterA.class, PresenterA.MyProxy.class);
        bindPresenter(PresenterB.class, PresenterB.MyProxy.class);
        bindPresenter(PresenterC.class, PresenterC.MyProxy.class);
        bindPresenter(PresenterD.class, PresenterD.MyProxy.class);
        bindPresenter(PresenterE.class, PresenterE.MyProxy.class);
        bindPresenter(PresenterF.class, PresenterF.MyProxy.class);
        bindPresenter(PresenterG.class, PresenterG.MyProxy.class);
    }
}
