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
package org.jboss.as.console.client.shared.runtime.logging.store;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.runtime.logging.viewer.Direction;
import org.jboss.as.console.client.shared.runtime.logging.viewer.Position;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import java.util.*;

import static com.google.gwt.http.client.URL.encode;
import static java.lang.Math.max;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * A store which holds a list of log files and manages the state of the active {@link LogFile}.
 *
 * @author Harald Pehl
 */
@Store
public class LogStore extends ChangeSupport {

    public final static String FILE_NAME = "file-name";
    public final static String FILE_SIZE = "file-size";
    public final static String LAST_MODIFIED_TIME = "last-modified-time";
    public final static String LAST_MODIFIED_TIMESTAMP = "last-modified-timestamp";

    private final static int PAGE_SIZE = 25;
    private final static int FOLLOW_INTERVAL = 1200; // ms

    private final HostStore hostStore;
    private final DispatchAsync dispatcher;
    private final Scheduler scheduler;
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
     * The selected log file
     */
    protected LogFile activeLogFile;

    /**
     * Pending streaming request.
     */
    protected PendingStreamingRequest pendingStreamingRequest;

    /**
     * The number of lines which is displayed in the log view. Changing this value influences the parameters
     * for the {@code read-log-file} operations
     */
    protected int pageSize;

    /**
     * Flag to pause the {@link LogStore.RefreshLogFile} command
     * when the related log view is no longer visible.
     */
    protected boolean pauseFollow;

    @Inject
    public LogStore(HostStore hostStore, DispatchAsync dispatcher, Scheduler scheduler, BootstrapContext bootstrap) {
        this.hostStore = hostStore;
        this.dispatcher = dispatcher;
        this.scheduler = scheduler;
        this.bootstrap = bootstrap;

        this.logFiles = new ArrayList<>();
        this.states = new LinkedHashMap<>();
        this.activeLogFile = null;
        this.pendingStreamingRequest = null;
        this.pageSize = PAGE_SIZE;
    }


    // ------------------------------------------------------ process methods

    @Process(actionType = ReadLogFiles.class)
    public void readLogFiles(final Dispatcher.Channel channel) {
        final ModelNode op = listLogFilesOp();

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
                    List<Property> properties = response.get(RESULT).asPropertyList();
                    for (Property property : properties) {
                        ModelNode node = property.getValue();
                        node.get(FILE_NAME).set(property.getName());
                        logFiles.add(node);
                    }

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

    @Process(actionType = OpenLogFile.class)
    public void openLogFile(final String name, final Dispatcher.Channel channel) {
        final LogFile logFile = states.get(name);

        if (logFile == null) {
            final ModelNode op = readLogFileOp(name);
            op.get("tail").set(true);
            dispatcher.execute(new DMRAction(wrapInComposite(op)), new AsyncCallback<DMRResponse>() {
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
                        ModelNode compResult = response.get(RESULT);
                        LogFile newLogFile = new LogFile(name, readLines(compResult), readFileSize(name, compResult));
                        newLogFile.setFollow(true);
                        states.put(name, newLogFile);
                        activate(newLogFile);
                        channel.ack();
                    }
                }
            });

        } else {
            // already open, just activate
            activate(logFile);
            channel.ack();
        }
    }

    @Process(actionType = StreamLogFile.class)
    public void streamLogFile(final String name, final Dispatcher.Channel channel) {
        final LogFile logFile = states.get(name);

        if (logFile == null) {
            RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, encode(streamUrl(name)));
            requestBuilder.setHeader("Accept", "text/plain");
            requestBuilder.setHeader("Content-Type", "text/plain");
            try {
                // store the request in order to cancel it later
                pendingStreamingRequest = new PendingStreamingRequest(name, requestBuilder.sendRequest(null, new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        if (response.getStatusCode() >= 400) {
                            channel.nack(new IllegalStateException("Failed to stream log file " + name + ": " +
                                    response.getStatusCode() + " - " + response.getStatusText()));
                        } else {
                            LogFile newLogFile = new LogFile(name, response.getText());
                            newLogFile.setFollow(false);
                            states.put(name, newLogFile);
                            activate(newLogFile);
                            channel.ack();
                        }
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        channel.nack(exception);
                    }
                }), channel);
            } catch (RequestException e) {
                channel.nack(e);
            }

        } else {
            // already streamed, just activate
            activate(logFile);
            channel.ack();
        }
    }

    @Process(actionType = DownloadLogFile.class)
    public void downloadLogFile(final String name, final Dispatcher.Channel channel) {
        Window.open(streamUrl(name), "", "");
        channel.ack();
    }

    @Process(actionType = CloseLogFile.class)
    public void closeLogFile(final String name, final Dispatcher.Channel channel) {
        LogFile removed = states.remove(name);
        if (removed == activeLogFile) {
            activeLogFile = null;
            pauseFollow = true;
        }
        channel.ack();
    }

    @Process(actionType = SelectLogFile.class)
    public void selectLogFile(final String name, final Dispatcher.Channel channel) {
        final LogFile logFile = states.get(name);
        if (logFile == null) {
            channel.nack(new IllegalStateException("Cannot select unknown log file " + name + ". " +
                    "Please open the log file first!"));
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

        final int skipped;
        final ModelNode op = readLogFileOp(activeLogFile.getName());
        if (prepareNavigation(activeLogFile, direction, op)) {
            skipped = op.get("skip").asInt();
            dispatcher.execute(new DMRAction(wrapInComposite(op)), new AsyncCallback<DMRResponse>() {
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
                        ModelNode compResult = response.get(RESULT);
                        int fileSize = readFileSize(activeLogFile.getName(), compResult);
                        List<String> lines = readLines(compResult);
                        finishNavigation(activeLogFile, direction, skipped, lines, fileSize);
                        channel.ack();
                    }
                }
            });
        } else {
            // TODO replace with channel.nack(String) after update to latest circuit
            channel.ack();
        }
    }

    private boolean prepareNavigation(final LogFile logFile, final Direction direction, final ModelNode op) {
        int skip = 0;
        boolean validOp = true;

        switch (direction) {
            case HEAD:
                if (logFile.isHead()) {
                    validOp = false;
                } else {
                    op.get("tail").set(false);
                }
                break;
            case PREVIOUS:
                if (logFile.getReadFrom() == Position.HEAD) {
                    op.get("tail").set(false);
                    skip = logFile.getSkipped() - pageSize;
                } else if (logFile.getReadFrom() == Position.TAIL) {
                    op.get("tail").set(true);
                    skip = logFile.getSkipped() + pageSize;
                } else {
                    validOp = false;
                }
                break;
            case NEXT:
                if (logFile.getReadFrom() == Position.HEAD) {
                    op.get("tail").set(false);
                    skip = logFile.getSkipped() + pageSize;
                } else if (logFile.getReadFrom() == Position.TAIL) {
                    op.get("tail").set(true);
                    skip = logFile.getSkipped() - pageSize;
                } else {
                    validOp = false;
                }
                break;
            case TAIL:
                if (logFile.isTail() && !logFile.isFollow()) {
                    validOp = false;
                } else {
                    op.get("tail").set(true);
                }
                break;
            default:
                break;
        }
        op.get("skip").set(skip);
        return validOp;
    }

    private void finishNavigation(LogFile logFile, Direction direction, int skipped, List<String> lines, int fileSize) {
        int diff = pageSize - lines.size();
        switch (direction) {
            case HEAD:
                logFile.goTo(Position.HEAD);
                break;
            case PREVIOUS:
                if (diff > 0) {
                    List<String> buffer = logFile.getLines(0, diff);
                    lines.addAll(buffer);
                    logFile.goTo(Position.HEAD);
                } else {
                    logFile.goTo(skipped);
                }
                break;
            case NEXT:
                if (diff > 0) {
                    List<String> buffer = logFile.getLines(pageSize - diff, pageSize);
                    lines.addAll(0, buffer);
                    logFile.goTo(Position.TAIL);
                } else {
                    logFile.goTo(skipped);
                }
                break;
            case TAIL:
                logFile.goTo(Position.TAIL);
                break;
        }
        logFile.setLines(lines);
        logFile.setFileSize(fileSize);
    }

    @Process(actionType = ChangePageSize.class)
    public void changePageSize(final int pageSize, final Dispatcher.Channel channel) {
        if (pageSize == this.pageSize) {
            // noop
            channel.ack();

        } else {
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

                dispatcher.execute(new DMRAction(wrapInComposite(op)), new AsyncCallback<DMRResponse>() {
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
                            ModelNode compResult = response.get(RESULT);
                            int fileSize = readFileSize(activeLogFile.getName(), compResult);
                            List<String> lines = readLines(compResult);
                            activeLogFile.setFileSize(fileSize);
                            activeLogFile.setLines(lines);
                            channel.ack();
                        }
                    }
                });
            }
        }
    }

    @Process(actionType = FollowLogFile.class)
    public void follow(final Dispatcher.Channel channel) {
        if (activeLogFile == null) {
            channel.nack(new IllegalStateException("Unable to follow: No active log file!"));
            return;
        }

        navigate(Direction.TAIL, channel);
        activeLogFile.setFollow(true);
        startFollowing(activeLogFile);
    }

    @Process(actionType = PauseFollowLogFile.class)
    public void pauseFollow(final Dispatcher.Channel channel) {
        if (activeLogFile == null) {
            channel.nack(new IllegalStateException("Unable to pause follow: No active log file!"));
            return;
        }

        pauseFollow = true;
        channel.ack();
    }

    @Process(actionType = UnFollowLogFile.class)
    public void unFollow(final Dispatcher.Channel channel) {
        if (activeLogFile == null) {
            channel.nack(new IllegalStateException("Unable to unfollow: No active log file!"));
            return;
        }

        activeLogFile.setFollow(false);
        channel.ack();
    }


    // ------------------------------------------------------ helper methods

    protected void activate(LogFile logFile) {
        activeLogFile = logFile;
        pauseFollow = false;
        if (logFile.isFollow()) {
            startFollowing(activeLogFile);
        }
    }

    private void startFollowing(LogFile logFile) {
        scheduler.scheduleFixedDelay(new RefreshLogFile(logFile.getName()), FOLLOW_INTERVAL);
    }

    private String streamUrl(final String name) {
        StringBuilder url = new StringBuilder();
        url.append(bootstrap.getProperty(ApplicationProperties.DOMAIN_API)).append("/");
        for (Property segment : baseAddress().asPropertyList()) {
            url.append(segment.getName()).append("/").append(segment.getValue().asString()).append("/");
        }
        url.append("log-file/").append(name).append("?operation=attribute&name=stream&useStreamAsResponse");
        return url.toString();
    }


    // ------------------------------------------------------ model node methods

    private ModelNode baseAddress() {
        ModelNode address = new ModelNode();
        if (!bootstrap.isStandalone()) {
            address.add("host", hostStore.getSelectedHost());
            address.add("server", hostStore.getSelectedServer());
        }
        address.add("subsystem", "logging");
        return address;
    }

    private ModelNode listLogFilesOp() {
        final ModelNode op = new ModelNode();
        op.get(ADDRESS).set(baseAddress());
        op.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        op.get(CHILD_TYPE).set("log-file");
        op.get(INCLUDE_RUNTIME).set(true);
        return op;
    }

    private ModelNode readLogFileOp(String logFile) {
        final ModelNode op = new ModelNode();
        op.get(ADDRESS).set(baseAddress());
        op.get(ADDRESS).add("log-file", logFile);
        op.get(OP).set("read-log-file");
        op.get("lines").set(pageSize);
        return op;
    }

    private ModelNode wrapInComposite(ModelNode readLogFileOp) {
        final ModelNode comp = new ModelNode();
        comp.get(ADDRESS).setEmptyList();
        comp.get(OP).set(COMPOSITE);

        List<ModelNode> steps = new LinkedList<>();
        steps.add(listLogFilesOp());
        steps.add(readLogFileOp);
        comp.get(STEPS).set(steps);

        return comp;
    }

    private int readFileSize(String name, ModelNode compResult) {
        int size = -1;
        ModelNode stepResult = compResult.get("step-1");
        if (stepResult.get(RESULT).isDefined()) {
            for (Property property : stepResult.get(RESULT).asPropertyList()) {
                if (name.equals(property.getName())) {
                    size = property.getValue().get(FILE_SIZE).asInt();
                    break;
                }
            }
        }
        if (size == -1) {
            // fall back to previously read nodes
            for (ModelNode node : logFiles) {
                if (name.equals(node.get(FILE_NAME).asString())) {
                    size = node.get(FILE_SIZE).asInt();
                }
            }

        }
        // to prevent follow up ArithmeticExceptions like x / size
        return max(1, size);
    }

    private List<String> readLines(ModelNode compResult) {
        List<String> extractedLines = new ArrayList<>();
        ModelNode stepResult = compResult.get("step-2");
        if (stepResult.get(RESULT).isDefined()) {
            for (ModelNode node : stepResult.get(RESULT).asList()) {
                extractedLines.add(node.asString());
            }
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

    public PendingStreamingRequest getPendingStreamingRequest() {
        return pendingStreamingRequest;
    }

    public static final class PendingStreamingRequest {
        private final String logFile;
        private final Request request;
        private final Dispatcher.Channel channel;

        private PendingStreamingRequest(final String logFile, final Request request, final Dispatcher.Channel channel) {
            this.logFile = logFile;
            this.request = request;
            this.channel = channel;
        }

        public void cancel() {
            if (request != null && request.isPending() && channel != null) {
                request.cancel();
                channel.nack("Download of " + logFile + " canceled");
            }
        }
    }


    // ------------------------------------------------------ polling

    private class RefreshLogFile implements Scheduler.RepeatingCommand {

        private final String name;

        private RefreshLogFile(String name) {
            this.name = name;
        }

        @Override
        public boolean execute() {
            if (isValid()) {
                final ModelNode op = readLogFileOp(name);
                op.get("tail").set(true);
                dispatcher.execute(new DMRAction(wrapInComposite(op)), new AsyncCallback<DMRResponse>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        // noop
                    }

                    @Override
                    public void onSuccess(DMRResponse result) {
                        ModelNode response = result.get();
                        if (!response.isFailure()) {
                            if (isValid()) {
                                ModelNode compResult = response.get(RESULT);
                                int fileSize = readFileSize(name, compResult);
                                List<String> lines = readLines(compResult);
                                LogFile logFile = states.get(name);
                                logFile.setFileSize(fileSize);
                                logFile.setLines(lines);
                                logFile.goTo(Position.TAIL);
                                fireChange(new FollowLogFile());
                            }
                        }
                    }
                });
            }
            return isValid();
        }

        private boolean isValid() {
            LogFile logFile = states.get(name);
            return logFile != null && logFile == activeLogFile && logFile.isFollow() && !pauseFollow;
        }
    }
}
