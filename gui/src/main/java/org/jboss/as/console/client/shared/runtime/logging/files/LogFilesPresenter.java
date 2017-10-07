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

import static org.jboss.as.console.client.shared.runtime.logging.store.LogStore.FILE_NAME;
import static org.jboss.as.console.client.shared.runtime.logging.store.LogStore.FILE_SIZE;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.runtime.logging.store.LogFile;
import org.jboss.as.console.client.shared.runtime.logging.store.LogStore;
import org.jboss.as.console.client.shared.runtime.logging.store.ReadLogFiles;
import org.jboss.as.console.client.shared.runtime.logging.store.ReadLogFilesForRefresh;
import org.jboss.as.console.client.shared.runtime.logging.store.RefreshLogFile;
import org.jboss.as.console.client.shared.runtime.logging.store.SelectLogFile;
import org.jboss.as.console.client.shared.runtime.logging.store.StreamLogFile;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServerInstance;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

public class LogFilesPresenter extends CircuitPresenter<LogFilesPresenter.MyView, LogFilesPresenter.MyProxy> {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.LogFiles)
    @SearchIndex(keywords = {"log-file", "log-view", "server-log", "download"})
    @AccessControl(resources = "/{implicit.host}/{selected.server}/subsystem=logging", recursive = true)
    public interface MyProxy extends Proxy<LogFilesPresenter>, Place {}

    public interface MyView extends View, HasPresenter<LogFilesPresenter> {
        void list(List<ModelNode> logFiles);
        void reset();
        void open(LogFile logFile);
        void refresh(LogFile logFile);
    }
    // @formatter:on


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
        this.streamingProgress = new StreamingProgress(logStore, SHOW_STREAM_IN_PROGRESS_TIMEOUT);
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

        } else if (action instanceof ReadLogFilesForRefresh) {
            getView().list(logStore.getLogFiles());
            String name = ((ReadLogFilesForRefresh) action).getName();
            refreshLogFile(name);

        } else if (action instanceof RefreshLogFile) {
            streamingProgress.done();
            getView().refresh(logStore.getActiveLogFile());

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
        getView().reset();
        circuit.dispatch(new ReadLogFiles());
    }

    public void onStreamLogFile(final String logFile, final int fileSize) {
        if (logStore.isOpen(logFile)) {
            circuit.dispatch(new SelectLogFile(logFile));
        } else {
            if (fileSize > LOG_FILE_SIZE_THRESHOLD) {
                Feedback.confirm(
                        Console.CONSTANTS.downloadLogFile(),
                        Console.CONSTANTS.downloadingLogFileConfirmation(),
                        isConfirmed -> {
                            if (isConfirmed) {
                                this.circuit.dispatch(new StreamLogFile(logFile));
                                streamingProgress.monitor(logFile);
                            }
                        });
            } else {
                this.circuit.dispatch(new StreamLogFile(logFile));
                streamingProgress.monitor(logFile);
            }
        }
    }

    public void onRefreshLogFile(final String logFile) {
        circuit.dispatch(new ReadLogFilesForRefresh(logFile));
    }

    private void refreshLogFile(String name) {
        ModelNode fileNode = null;
        for (ModelNode node: logStore.getLogFiles()) {
            if (node.get(FILE_NAME).asString().equals(name)) {
                fileNode = node;
                break;
            }
        }
        if (fileNode != null) {
            int fileSize = fileNode.get(FILE_SIZE).asInt();
            if (fileSize > LOG_FILE_SIZE_THRESHOLD) {
                Feedback.confirm(
                        Console.CONSTANTS.downloadLogFile(),
                        Console.CONSTANTS.downloadingLogFileConfirmation(),
                        isConfirmed -> {
                            if (isConfirmed) {
                                this.circuit.dispatch(new RefreshLogFile(name));
                                streamingProgress.monitor(name);
                            }
                        });
            } else {
                this.circuit.dispatch(new RefreshLogFile(name));
                streamingProgress.monitor(name);
            }
        }
    }
}
