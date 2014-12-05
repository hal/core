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
package org.jboss.as.console.client.shared.runtime.logging.files;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.runtime.logging.store.*;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServerInstance;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

public class LogFilesPresenter extends CircuitPresenter<LogFilesPresenter.MyView, LogFilesPresenter.MyProxy> {

    @ProxyCodeSplit
    @NameToken(NameTokens.LogFiles)
    @SearchIndex(keywords = {"log-file", "log-view", "server-log", "download"})
    @RequiredResources(resources = "/{selected.host}/{selected.server}/subsystem=logging", recursive = true)
    public interface MyProxy extends ProxyPlace<LogFilesPresenter> {}


    public interface MyView extends View, HasPresenter<LogFilesPresenter> { // @formatter:off
        void list(List<ModelNode> logFiles);
        void open(LogFile logFile);
        void refresh(LogFile logFile);
        boolean isLogFileSelected();
    } // @formatter:on


    /**
     * If log files are bigger than this threshold a confirmation dialog is displayed.
     */
    public static final int LOG_FILE_SIZE_THRESHOLD = 15000000; // bytes

    /**
     * If the streaming of a log files takes longer then this timeout,
     * the {@link StreamingProgress} modal is displayed.
     */
    public static final int SHOW_STREAM_IN_PROGRESS_TIMEOUT = 666; // ms

    private final RevealStrategy revealStrategy;
    private final Dispatcher circuit;
    private final LogStore logStore;
    private final HostStore hostStore;
    private final StreamingProgress streamingProgress;

    @Inject
    public LogFilesPresenter(EventBus eventBus, MyView view, MyProxy proxy, RevealStrategy revealStrategy,
                             Dispatcher circuit, LogStore logStore, HostStore hostStore) {
        super(eventBus, view, proxy, circuit);
        this.revealStrategy = revealStrategy;
        this.circuit = circuit;
        this.logStore = logStore;
        this.hostStore = hostStore;
        this.streamingProgress = new StreamingProgress(circuit, logStore, SHOW_STREAM_IN_PROGRESS_TIMEOUT);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(logStore);
        addChangeHandler(hostStore);
    }

    @Override
    public void onAction(Action action) {
        if (action instanceof ReadLogFiles) {
            getView().list(logStore.getLogFiles());

        } else if (action instanceof StreamLogFile) {
            streamingProgress.done();
            getView().open(logStore.getActiveLogFile());

        } else if (action instanceof SelectLogFile) {
            getView().open(logStore.getActiveLogFile());

        } else if (action instanceof SelectServerInstance) {
            onReset();
        }
    }

    @Override
    protected void onError(Action action, String reason) {
        if (action instanceof StreamLogFile) {
            Console.info(reason);
            streamingProgress.done();
        }
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        circuit.dispatch(new ReadLogFiles());
        if (getView().isLogFileSelected() && logStore.getActiveLogFile() != null) {
            circuit.dispatch(new SelectLogFile(logStore.getActiveLogFile().getName()));
        }
    }

    public void onStreamLogFile(final String logFile, final int fileSize) {
        if (logStore.isOpen(logFile)) {
            this.circuit.dispatch(new SelectLogFile(logFile));
        } else {
            if (fileSize > LOG_FILE_SIZE_THRESHOLD) {
                Feedback.confirm(
                        "Download Log File",
                        "Downloading this log file may take some time. Do you want to proceed?",
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    streamingProgress.monitor(logFile);
                                }
                            }
                        });
            } else {
                streamingProgress.monitor(logFile);
            }

        }
    }
}
