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

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * A store which holds a list of log files for the selected server and manages a list
 * of {@link LogFile}s.
 * <p/>
 * TODO Implement 'tail -f' using the GWT Scheduler.
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
    private final static int FOLLOW_INTERVAL = 2000; // ms

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
    protected final Map<String, LogFile> states;

    /**
     * The selected log state
     */
    protected LogFile activeLogFile;

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
        this.activeLogFile = null;
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
                    for (Map.Entry<String, LogFile> entry : states.entrySet()) {
                        if (!names.contains(entry.getKey())) {
                            entry.getValue().setStale(true);
                        }
                    }
                    channel.ack();
                }
            }
        });
    }

    // TODO Follow by default
    @Process(actionType = OpenLogFile.class)
    public void openLogFile(final String name, final Dispatcher.Channel channel) {
        final LogFile logFile = states.get(name);

        if (logFile == null) {
            final ModelNode op = readLogFileOp(name);
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
                        channel.nack(new RuntimeException("Failed to open " + name + " using " + op + ": " +
                                response.getFailureDescription()));
                    } else {
                        activate(new LogFile(name, readLines(response.get(RESULT).asList())));
                        channel.ack();
                    }
                }
            });

        } else {
            // already open, just select
            selectLogFile(name, channel);
        }
    }

    @Process(actionType = CloseLogFile.class)
    public void closeLogFile(final String name, final Dispatcher.Channel channel) {
        LogFile removed = states.remove(name);
        if (removed == activeLogFile) {
            activeLogFile = null;
        }
        channel.ack();
    }

    @Process(actionType = SelectLogFile.class)
    public void selectLogFile(final String name, final Dispatcher.Channel channel) {
        final LogFile logFile = states.get(name);
        if (logFile == null) {
            channel.nack(new IllegalStateException("Cannot select unknown log file " + name + ". Please open the log file first!"));
            return;
        }
        activate(logFile);
        channel.ack();
    }

    @Process(actionType = NavigateInLogFile.class)
    public void navigate(final Direction direction, final Dispatcher.Channel channel) {
        if (activeLogFile == null) {
            channel.nack(new IllegalStateException("Unable to navigate: No active log file!"));
            return;
        }

        final ModelNode op;
        final int skipped;
        try {
            op = prepareNavigation(direction);
            skipped = op.get("skip").asInt();
        } catch (IllegalStateException e) {
            channel.nack(e);
            return;
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
                    channel.nack(new RuntimeException("Failed to navigate in " + activeLogFile + " using " + op + ": " +
                            response.getFailureDescription()));
                } else {
                    List<String> lines = readLines(response.get(RESULT).asList());
                    finishNavigation(direction, skipped, lines);
                    channel.ack();
                }
            }
        });
    }

    private ModelNode prepareNavigation(Direction direction) {
        int skip = 0;
        ModelNode op = readLogFileOp(activeLogFile.getName());
        switch (direction) {
            case HEAD:
                if (activeLogFile.isHead()) {
                    throw new IllegalStateException(
                            "Invalid direction " + Direction.HEAD + ": " + activeLogFile + " already at " + Position.HEAD);
                } else {
                    op.get("tail").set(false);
                }
                break;
            case PREVIOUS:
                if (activeLogFile.isHead()) {
                    throw new IllegalStateException(
                            "Invalid direction " + Direction.PREVIOUS + ": " + activeLogFile + " already at " + Position.HEAD);
                } else if (activeLogFile.getLastDirection() == null ||
                        activeLogFile.getLastDirection() == Direction.PREVIOUS ||
                        activeLogFile.getLastDirection() == Direction.TAIL) {
                    skip = activeLogFile.getSkipped() + pageSize;
                    op.get("tail").set(true);
                } else if (activeLogFile.getLastDirection() == Direction.NEXT) {
                    // TODO
                } else {
                    throw new IllegalStateException("Invalid combination of last direction " +
                            activeLogFile.getLastDirection() + " and current direction " + direction +
                            " for " + activeLogFile);
                }
                break;
            case NEXT:
                if (activeLogFile.isTail()) {
                    throw new IllegalStateException(
                            "Invalid direction " + Direction.TAIL + ": " + activeLogFile + " already at " + Position.TAIL);
                } else if (activeLogFile.getLastDirection() == null ||
                        activeLogFile.getLastDirection() == Direction.NEXT ||
                        activeLogFile.getLastDirection() == Direction.HEAD) {
                    skip = activeLogFile.getSkipped() + pageSize;
                    op.get("tail").set(false);
                } else if (activeLogFile.getLastDirection() == Direction.PREVIOUS) {
                    // TODO
                } else {
                    throw new IllegalStateException("Invalid combination of last direction " +
                            activeLogFile.getLastDirection() + " and current direction " + direction +
                            " for " + activeLogFile);
                }
                break;
            case TAIL:
                if (activeLogFile.isTail()) {
                    throw new IllegalStateException(
                            "Invalid direction " + Direction.TAIL + ": " + activeLogFile + " already at " + Position.TAIL);
                } else {
                    op.get("tail").set(true);
                }
                break;
            default:
                break;
        }
        op.get("skip").set(skip);
        return op;
    }

    private void finishNavigation(Direction direction, int skipped, List<String> lines) {
        int diff = pageSize - lines.size();
        switch (direction) {
            case HEAD:
                activeLogFile.goTo(Position.HEAD);
                activeLogFile.setLines(lines);
                break;
            case PREVIOUS:
                if (diff > 0) {
                    List<String> buffer = activeLogFile.getLines(0, diff);
                    lines.addAll(buffer);
                    activeLogFile.goTo(Position.HEAD);
                } else {
                    activeLogFile.goTo(skipped);
                }
                break;
            case NEXT:
                if (diff > 0) {
                    List<String> buffer = activeLogFile.getLines(pageSize - diff, pageSize);
                    lines.addAll(0, buffer);
                    activeLogFile.goTo(Position.TAIL);
                } else {
                    activeLogFile.goTo(skipped);
                }
                break;
            case TAIL:
                activeLogFile.goTo(Position.TAIL);
                break;
        }
        activeLogFile.setLines(lines);
        activeLogFile.setLastDirection(direction);
    }

    @Process(actionType = ChangePageSize.class)
    public void changePageSize(final Integer pageSize, final Dispatcher.Channel channel) {
        this.pageSize = pageSize;
        if (activeLogFile != null) {
            final ModelNode op = readLogFileOp(activeLogFile.getName());
            switch (activeLogFile.getPosition()) {
                case HEAD:
                    op.get("tail").set(false);
                    break;
                case LINE_NUMBER:
                    op.get("skip").set(activeLogFile.getSkipped());
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
                                " for " + activeLogFile + " using " + op + ": " + response.getFailureDescription()));
                    } else {
                        activeLogFile.setLines(readLines(response.get(RESULT).asList()));
                        channel.ack();
                    }
                }
            });
        }
    }


    // ------------------------------------------------------ helper methods

    protected void activate(LogFile logFile) {
        states.put(logFile.getName(), logFile);
        activeLogFile = logFile;
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

    public LogFile getActiveLogFile() {
        return activeLogFile;
    }
}
