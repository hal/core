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

import com.google.gwt.event.shared.GwtEvent;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.Console;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.PropagatesChange;
import org.jboss.gwt.circuit.dag.DAGDispatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * A presenter which registers a {@link org.jboss.gwt.circuit.PropagatesChange.Handler} for a given store.
 * The handler delegates to {@link #onAction(org.jboss.gwt.circuit.Action)} if the presenter is visible.
 * The registered handlers are removed again in {@link #onUnbind()}.
 *
 * @author Harald Pehl
 */
public abstract class CircuitPresenter<V extends View, Proxy_ extends Proxy<?>> extends Presenter<V, Proxy_> {

    private final List<HandlerRegistration> registrations;

    protected CircuitPresenter(boolean autoBind, EventBus eventBus, V view, Proxy_ proxy, Dispatcher circuit) {
        super(autoBind, eventBus, view, proxy);
        this.registrations = new ArrayList<>();
        circuit.addDiagnostics(new ErrorHandler());
    }

    protected CircuitPresenter(EventBus eventBus, V view, Proxy_ proxy, Dispatcher circuit) {
        super(eventBus, view, proxy);
        this.registrations = new ArrayList<>();
        circuit.addDiagnostics(new ErrorHandler());
    }

    protected CircuitPresenter(EventBus eventBus, V view, Proxy_ proxy, RevealType revealType, Dispatcher circuit) {
        super(eventBus, view, proxy, revealType);
        this.registrations = new ArrayList<>();
        circuit.addDiagnostics(new ErrorHandler());
    }

    protected CircuitPresenter(EventBus eventBus, V view, Proxy_ proxy, GwtEvent.Type<RevealContentHandler<?>> slot, Dispatcher circuit) {
        super(eventBus, view, proxy, slot);
        this.registrations = new ArrayList<>();
        circuit.addDiagnostics(new ErrorHandler());
    }

    protected CircuitPresenter(EventBus eventBus, V view, Proxy_ proxy, RevealType revealType, GwtEvent.Type<RevealContentHandler<?>> slot, Dispatcher circuit) {
        super(eventBus, view, proxy, revealType, slot);
        this.registrations = new ArrayList<>();
        circuit.addDiagnostics(new ErrorHandler());
    }

    protected void addChangeHandler(PropagatesChange propagatesChange) {
        registrations.add(propagatesChange.addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Action action) {
                if (isVisible()) {
                    onAction(action);
                }
            }
        }));
    }

    @Override
    protected void onUnbind() {
        super.onUnbind();
        for (HandlerRegistration registration : registrations) {
            registration.removeHandler();
        }
    }

    /**
     * When this method is called it's guaranteed that the presenter is visible.
     */
    protected abstract void onAction(Action action);

    /**
     * When this method is called it's guaranteed that the presenter is visible.
     */
    protected void onError(Action action, String reason) {
        Console.error("Last action resulted in an error", reason);
    }

    /**
     * When this method is called it's guaranteed that the presenter is visible.
     */
    protected void onError(Action action, Throwable t) {
        Console.error("Last action resulted in an error", t.getMessage());
    }


    private class ErrorHandler implements DAGDispatcher.Diagnostics {

        @Override
        public void onDispatch(Action a) {
            // noop
        }

        @Override
        public void onLock() {
            // noop
        }

        @Override
        public void onExecute(Class<?> s, Action a) {
            // noop
        }

        @Override
        public void onAck(Class<?> s, Action a) {
            // noop
        }

        @Override
        public void onNack(Class<?> store, Action action, String reason) {
            if (isVisible()) {
                onError(action, reason);
            }
        }

        @Override
        public void onNack(Class<?> store, Action action, Throwable t) {
            if (isVisible()) {
                onError(action, t);
            }
        }

        @Override
        public void onUnlock() {
            // noop
        }
    }
}
