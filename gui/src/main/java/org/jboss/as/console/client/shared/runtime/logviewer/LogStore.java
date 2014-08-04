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
package org.jboss.as.console.client.shared.runtime.logviewer;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.*;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import javax.inject.Inject;
import java.util.*;

import static org.jboss.as.console.client.shared.runtime.logviewer.Direction.HEAD;
import static org.jboss.as.console.client.shared.runtime.logviewer.Direction.TAIL;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * A store which holds a list of log files for the selected server and manages a list
 * of {@link LogState}s.
 *
 * @author Harald Pehl
 */
@Store
@SuppressWarnings("UnusedDeclaration")
public class LogStore extends ChangeSupport {

    public final static String FILE_NAME = "file-name";
    public final static String FILE_SIZE = "file-size";
    public final static String LAST_MODIFIED_DATE = "last-modified-date";
    private final static int PAGE_SIZE = 25;

    private final HostStore hostStore;
    private final DispatchAsync dispatcher;
    private final BootstrapContext bootstrap;

    // ------------------------------------------------------ state

    /**
     * Log files of the selected server. Each element in the list contains the following attributes:
     * <ul>
     * <li>{@code file-name}</li>
     * <li>{@code file-size}</li>
     * <li>{@code last-modified-date}</li>
     * </ul>
     */
    protected final List<ModelNode> logFiles;

    /**
     * Open logs with the name of the log file as key and the related state as value.
     */
    protected final Map<String, LogState> states;

    /**
     * The selected log state
     */
    protected LogState activeState;

    /**
     * The number of lines which is displayed in the log view. Changing this value influences the parameters
     * for the {@code read-log-file} operations
     */
    protected int pageSize;

    @Inject
    public LogStore(HostStore hostStore, DispatchAsync dispatcher, BootstrapContext bootstrap) {
        this.hostStore = hostStore;
        this.dispatcher = dispatcher;
        this.bootstrap = bootstrap;

        this.logFiles = new ArrayList<>();
        this.states = new LinkedHashMap<>();
        this.activeState = null;
        this.pageSize = PAGE_SIZE;
    }


    // ------------------------------------------------------ action handlers

    @Process(actionType = ReadLogFiles.class)
    public void readLogFiles(final Dispatcher.Channel channel) {
        final ModelNode op = new ModelNode();
        op.get(ADDRESS).set(baseAddress());
        op.get(OP).set("list-log-files");

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    channel.nack(new RuntimeException("Failed to read list of log files using " + op +
                            ": " + response.getFailureDescription()));
                } else {
                    logFiles.clear();
                    logFiles.addAll(response.get(RESULT).asList());

                    // mark outdated log views as stale
                    Set<String> names = new HashSet<String>();
                    for (ModelNode logFile : logFiles) {
                        names.add(logFile.get(FILE_NAME).asString());
                    }
                    for (Map.Entry<String, LogState> entry : states.entrySet()) {
                        if (!names.contains(entry.getKey())) {
                            entry.getValue().setStale(true);
                        }
                    }
                    channel.ack();
                }
            }
        });
    }

    @Process(actionType = SelectLogFile.class)
    public void selectLogFile(final String logFile, final Dispatcher.Channel channel) {
        final LogState logState = states.get(logFile);
        boolean dmrOp = logState == null || (!logState.isStale() && logState.isAutoRefresh());
        if (dmrOp) {

            final ModelNode op = readLogFileOp(logFile);
            op.get("tail").set(true);
            dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(Throwable caught) {
                    channel.nack(caught);
                }

                @Override
                public void onSuccess(DMRResponse result) {
                    ModelNode response = result.get();
                    if (response.isFailure()) {
                        channel.nack(new RuntimeException("Failed to select " + logState + " using "  + op + ": " +
                                response.getFailureDescription()));
                    } else {
                        activate(new LogState(logFile, readLines(response.get(RESULT).asList())));
                        channel.ack();
                    }
                }
            });

        } else {
            activate(logState);
            channel.ack();
        }
    }

    @Process(actionType = ActivateLogFile.class)
    public void activateLogFile(final String logFile, final Dispatcher.Channel channel) {
        LogState logState = states.get(logFile);
        if (logState == null) {
            channel.nack(new IllegalStateException("No log file found for " + logFile));
        } else {
            activate(logState);
            channel.ack();
        }
    }

    @Process(actionType = CloseLogFile.class)
    public void closeLogFile(final String logFile, final Dispatcher.Channel channel) {
        states.remove(logFile);
        if (activeState != null && activeState.getName().endsWith(logFile)) {
            activeState = null;
        }
        channel.ack();
    }

    @Process(actionType = RefreshLogFile.class)
    public void refresh(final Dispatcher.Channel channel) {
        if (activeState == null) {
            channel.nack(new IllegalStateException("No active log file"));
            return;
        }

        final ModelNode op = readLogFileOp(activeState.getName());
        op.get("tail").set(true);
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    channel.nack(new RuntimeException("Failed to refresh " + activeState + " using " + op + ": " +
                            response.getFailureDescription()));
                } else {
                    activeState.setLines(readLines(response.get(RESULT).asList()));
                    activeState.goTo(Position.TAIL);
                    channel.ack();
                }
            }
        });
    }

    @Process(actionType = NavigateInLogFile.class)
    public void navigate(final Direction direction, final Dispatcher.Channel channel) {
        if (activeState == null) {
            channel.nack(new IllegalStateException("No active log file"));
            return;
        }

        int lineNumber = 0;
        final ModelNode op = readLogFileOp(activeState.getName());
        switch (direction) {
            case HEAD:
                op.get("tail").set(false);
                break;
            case PREVIOUS:
                if (activeState.getPosition() == Position.HEAD) {
                    navigate(HEAD, channel);
                } else {
                    lineNumber = activeState.getLineNumber() + pageSize;
                    op.get("tail").set(true);
                    op.get("skip").set(lineNumber);
                }
                break;
            case NEXT:
                if (activeState.getPosition() == Position.TAIL) {
                    navigate(TAIL, channel);
                } else {
                    lineNumber = activeState.getLineNumber() + pageSize;
                    op.get("tail").set(false);
                }
                break;
            case TAIL:
                op.get("tail").set(true);
                break;
            default:
                break;
        }
        op.get("skip").set(lineNumber);
        final int finalLineNumber = lineNumber;

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    channel.nack(new RuntimeException("Failed to navigate in " + activeState + " using " + op + ": " +
                            response.getFailureDescription()));
                } else {
                    List<String> lines = readLines(response.get(RESULT).asList());
                    if (lines.size() < pageSize) {
                        // TODO fill empty space with buffered lines
                    }
                    if (direction == HEAD) {
                        activeState.goTo(Position.HEAD);
                    } else if (direction == TAIL) {
                        activeState.goTo(Position.TAIL);
                    } else {
                        activeState.goTo(finalLineNumber);
                    }
                    activeState.setLines(lines);
                    channel.ack();
                }
            }
        });
    }

    @Process(actionType = UnfollowLogFile.class)
    public void unfollow(final Dispatcher.Channel channel) {
        if (activeState != null) {
            activeState.setAutoRefresh(false);
        }
        channel.ack();
    }

    @Process(actionType = ChangePageSize.class)
    public void changePageSize(final Integer pageSize, final Dispatcher.Channel channel) {
        this.pageSize = pageSize;
        if (activeState != null) {
            final ModelNode op = readLogFileOp(activeState.getName());
            switch (activeState.getPosition()) {
                case HEAD:
                    op.get("tail").set(false);
                    break;
                case LINE_NUMBER:
                    op.get("skip").set(activeState.getLineNumber());
                    break;
                case TAIL:
                    op.get("tail").set(true);
                    break;
            }

            dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(Throwable caught) {
                    channel.nack(caught);
                }

                @Override
                public void onSuccess(DMRResponse result) {
                    ModelNode response = result.get();
                    if (response.isFailure()) {
                        channel.nack(new RuntimeException("Failed to change page size to " + pageSize +
                                " for " + activeState + " using " + op + ": " + response.getFailureDescription()));
                    } else {
                        activeState.setLines(readLines(response.get(RESULT).asList()));
                        channel.ack();
                    }
                }
            });
        }
    }


    // ------------------------------------------------------ helper methods

    protected void activate(LogState logState) {
        states.put(logState.getName(), logState);
        activeState = logState;
    }

    private ModelNode baseAddress() {
        ModelNode address = new ModelNode();
        if (!bootstrap.isStandalone()) {
            address.add("host", hostStore.getSelectedHost());
            address.add("server", hostStore.getSelectedServer());
        }
        address.add("subsystem", "logging");
        return address;
    }

    private ModelNode readLogFileOp(String logFile) {
        final ModelNode op = new ModelNode();
        op.get(ADDRESS).set(baseAddress());
        op.get(OP).set("read-log-file");
        op.get(NAME).set(logFile);
        op.get("lines").set(pageSize);
        return op;
    }

    private List<String> readLines(List<ModelNode> lines) {
        List<String> extractedLines = new ArrayList<>();
        for (ModelNode line : lines) {
            extractedLines.add(line.asString());
        }
        return extractedLines;
    }


    // ------------------------------------------------------ state access

    public List<ModelNode> getLogFiles() {
        return logFiles;
    }

    public LogState getActiveState() {
        return activeState;
    }
}
