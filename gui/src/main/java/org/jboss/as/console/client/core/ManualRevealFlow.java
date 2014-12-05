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
package org.jboss.as.console.client.core;

import com.google.gwt.user.client.Command;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;
import org.jboss.gwt.flow.client.Progress;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Harald Pehl
 */
class ManualRevealFlow {

    private final Presenter<?, ? extends ProxyPlace<?>> presenter;
    private final Progress progress;
    private final List<Function<FunctionContext>> functions;

    public ManualRevealFlow(Presenter<?, ? extends ProxyPlace<?>> presenter, Progress progress) {
        this.presenter = presenter;
        this.progress = progress;
        this.functions = new ArrayList<>();
    }

    public void addFunction(Function<FunctionContext> function) {
        functions.add(function);
    }

    @SuppressWarnings("unchecked")
    public void execute(final Command command) {
        new Async<FunctionContext>(progress).parallel(new FunctionContext(), new Outcome<FunctionContext>() {
            @Override
            public void onFailure(FunctionContext context) {
                presenter.getProxy().manualRevealFailed();
            }

            @Override
            public void onSuccess(FunctionContext context) {
                presenter.getProxy().manualReveal(presenter);
                command.execute();
            }
        }, functions.toArray(new Function[functions.size()]));
    }
}
