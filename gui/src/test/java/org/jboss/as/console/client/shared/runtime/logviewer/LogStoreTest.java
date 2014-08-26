package org.jboss.as.console.client.shared.runtime.logviewer;

import com.google.gwt.core.client.Scheduler;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.StaticDispatcher;
import org.jboss.dmr.client.StaticDmrResponse;
import org.jboss.gwt.circuit.util.NoopChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogStoreTest {

    private StaticDispatcher dispatcher;
    private LogStore logStore;

    @Before
    public void setUp() {
        BootstrapContext bootstrap = mock(BootstrapContext.class);
        Scheduler scheduler = mock(Scheduler.class);
        when(bootstrap.isStandalone()).thenReturn(true);

        dispatcher = new StaticDispatcher();
        logStore = new LogStore(null, dispatcher, scheduler, bootstrap);
    }

    @Test
    public void readLogFiles() {
        dispatcher.push(StaticDmrResponse.ok(logFileNodes("server.log", "server.log.2014.-08-01", "server.log.2014.-08-02")));
        logStore.readLogFiles(NoopChannel.INSTANCE);

        assertNull(logStore.getActiveLogFile());
        assertEquals(3, logStore.getLogFiles().size());
    }

    @Test
    public void readLogFilesAndVerifyStale() {
        LogFile stale = new LogFile("stale.log", Collections.<String>emptyList());
        logStore.states.put(stale.getName(), stale);

        dispatcher.push(StaticDmrResponse.ok(logFileNodes("server.log")));
        logStore.readLogFiles(NoopChannel.INSTANCE);

        // "stale.log" is no longer in the list of log files and must be stale
        assertTrue(logStore.states.get(stale.getName()).isStale());
    }

    @Test
    public void openLogFile() {
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.openLogFile("server.log", NoopChannel.INSTANCE);

        assertFalse(logStore.pauseFollow);
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertTrue(activeLogFile.isTail());
        assertEquals(Position.TAIL, activeLogFile.getReadFrom());
        assertEquals(0, activeLogFile.getSkipped());
        assertTrue(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());
    }

    @Test
    public void reopenLogFile() {
        LogFile logFile = new LogFile("server.log", lines(0));
        logStore.states.put(logFile.getName(), logFile);

        assertFalse(logStore.pauseFollow);
        // Must not dispatch a DMR operation
        logStore.openLogFile("server.log", NoopChannel.INSTANCE);
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertSame(logFile, activeLogFile);
    }

    @Test
    public void selectLogFile() {
        LogFile logFile = new LogFile("server.log", lines(0));
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        assertFalse(logStore.pauseFollow);
        // Must not dispatch a DMR operation
        logStore.selectLogFile("server.log", NoopChannel.INSTANCE);
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertSame(logFile, activeLogFile);
    }

    @Test
    public void closeLogFile() {
        LogFile foo = new LogFile("foo.log", Collections.<String>emptyList());
        LogFile bar = new LogFile("bar.log", Collections.<String>emptyList());
        logStore.states.put(foo.getName(), foo);
        logStore.states.put(bar.getName(), bar);
        logStore.activate(foo);

        logStore.closeLogFile("bar.log", NoopChannel.INSTANCE);

        assertFalse(logStore.pauseFollow);
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertSame(foo, activeLogFile);
        assertEquals(1, logStore.states.size());
        assertSame(foo, logStore.states.values().iterator().next());
    }

    @Test
    public void closeActiveLogFile() {
        LogFile logFile = new LogFile("server.log", Collections.<String>emptyList());
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        logStore.closeLogFile("server.log", NoopChannel.INSTANCE);

        assertTrue(logStore.pauseFollow);
        assertNull(logStore.getActiveLogFile());
        assertTrue(logStore.states.isEmpty());
    }

    @Test
    public void navigateHead() {
        LogFile logFile = new LogFile("server.log", lines(2));
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.HEAD, NoopChannel.INSTANCE);

        // 1. verify DMR operation
        ModelNode operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(0, operation.get("skip").asInt());

        // 2. verify log file state
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertTrue(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.HEAD, activeLogFile.getReadFrom());
        assertEquals(0, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());
    }

    @Test
    public void navigateTail() {
        LogFile logFile = new LogFile("server.log", lines(2));
        logFile.goTo(Position.HEAD);
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.TAIL, NoopChannel.INSTANCE);

        // 1. verify DMR operation
        ModelNode operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(0, operation.get("skip").asInt());

        // 2. verify log file state
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertTrue(activeLogFile.isTail());
        assertEquals(Position.TAIL, activeLogFile.getReadFrom());
        assertEquals(0, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());
    }

    @Test
    public void navigatePrev() {
        LogFile logFile = new LogFile("server.log", lines(2));
        logStore.pageSize = 2;
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.PREVIOUS, NoopChannel.INSTANCE);

        // 1. verify DMR operation
        ModelNode operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 2. verify log file state
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.TAIL, activeLogFile.getReadFrom());
        assertEquals(2, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());
    }

    @Test
    public void navigatePrevPrevNext() {
        LogFile logFile = new LogFile("server.log", lines(2));
        logStore.pageSize = 2;
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        // Prev (1)
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.PREVIOUS, NoopChannel.INSTANCE);

        // Prev (2)
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.PREVIOUS, NoopChannel.INSTANCE);

        // 1 verify DMR operation
        ModelNode operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(4, operation.get("skip").asInt());

        // 2 verify log file state
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.TAIL, activeLogFile.getReadFrom());
        assertEquals(4, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());

        // Next
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.NEXT, NoopChannel.INSTANCE);

        // 3.1 verify DMR operation
        operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 3.2 verify log file state
        activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.TAIL, activeLogFile.getReadFrom());
        assertEquals(2, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());
    }

    @Test
    public void navigatePrevHeadNext() {
        LogFile logFile = new LogFile("server.log", lines(2));
        logStore.pageSize = 2;
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        // Prev
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.PREVIOUS, NoopChannel.INSTANCE);

        // 1.1 verify DMR operation
        ModelNode operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 1.2 verify log file state
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.TAIL, activeLogFile.getReadFrom());
        assertEquals(2, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());

        // Head
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.HEAD, NoopChannel.INSTANCE);

        // 2.1 verify DMR operation
        operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(0, operation.get("skip").asInt());

        // 2.2 verify log file state
        activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertTrue(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.HEAD, activeLogFile.getReadFrom());
        assertEquals(0, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());

        // Next
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.NEXT, NoopChannel.INSTANCE);

        // 3.1 verify DMR operation
        operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 3.2 verify log file state
        activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.HEAD, activeLogFile.getReadFrom());
        assertEquals(2, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());
    }

    @Test
    public void navigateNext() {
        LogFile logFile = new LogFile("server.log", lines(2));
        logFile.goTo(Position.HEAD);
        logStore.pageSize = 2;
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.NEXT, NoopChannel.INSTANCE);

        // 1. verify DMR operation
        ModelNode operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 2. verify log file state
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.HEAD, activeLogFile.getReadFrom());
        assertEquals(2, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());
    }

    @Test
    public void navigateNextNextPrev() {
        LogFile logFile = new LogFile("server.log", lines(2));
        logFile.goTo(Position.HEAD);
        logStore.pageSize = 2;
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        // Next (1)
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.NEXT, NoopChannel.INSTANCE);

        // 1.1 verify DMR operation
        ModelNode operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 1.2 verify log file state
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.HEAD, activeLogFile.getReadFrom());
        assertEquals(2, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());

        // Next (2)
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.NEXT, NoopChannel.INSTANCE);

        // 2.1 verify DMR operation
        operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(4, operation.get("skip").asInt());

        // 1.2 verify log file state
        activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.HEAD, activeLogFile.getReadFrom());
        assertEquals(4, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());

        // Previous
        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.PREVIOUS, NoopChannel.INSTANCE);

        // 3.1 verify DMR operation
        operation = dispatcher.getLastOperation();
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 3.2 verify log file state
        activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("server.log", activeLogFile.getName());
        assertLines(activeLogFile.getContent(), 0, 1);
        assertFalse(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertEquals(Position.HEAD, activeLogFile.getReadFrom());
        assertEquals(2, activeLogFile.getSkipped());
        assertFalse(activeLogFile.isFollow());
        assertFalse(activeLogFile.isStale());
    }

    @Test
    public void changePageSize() {
        logStore.changePageSize(42, NoopChannel.INSTANCE);
        assertEquals(42, logStore.pageSize);
    }

    @Test
    public void follow() {
        LogFile logFile = new LogFile("server.log", Collections.<String>emptyList());
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        logStore.follow(NoopChannel.INSTANCE);

        assertFalse(logStore.pauseFollow);
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertTrue(activeLogFile.isFollow());
    }

    @Test
    public void pauseFollow() {
        LogFile logFile = new LogFile("server.log", Collections.<String>emptyList());
        logFile.setFollow(true);
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        logStore.pauseFollow(NoopChannel.INSTANCE);

        assertTrue(logStore.pauseFollow);
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertTrue(activeLogFile.isFollow());
    }

    @Test
    public void unFollow() {
        LogFile logFile = new LogFile("server.log", Collections.<String>emptyList());
        logFile.setFollow(true);
        logStore.states.put(logFile.getName(), logFile);
        logStore.activate(logFile);

        logStore.unFollow(NoopChannel.INSTANCE);

        assertFalse(logStore.pauseFollow);
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertFalse(activeLogFile.isFollow());
    }

    private ModelNode logFileNodes(String... names) {
        ModelNode payload = new ModelNode();
        for (String name : names) {
            ModelNode logFile = new ModelNode();
            logFile.get("file-name").set(name);
            payload.add(logFile);
        }
        return payload;
    }

    private ModelNode logFileNode(int numberOfLines) {
        ModelNode payload = new ModelNode();
        for (String line : lines(numberOfLines)) {
            payload.add(line);
        }
        return payload;
    }

    private List<String> lines(int numberOfLines) {
        List<String> lines = new LinkedList<>();
        for (int i = 0; i < numberOfLines; i++) {
            lines.add("line " + i);
        }
        return lines;
    }

    private void assertLines(String content, int... lines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            int number = lines[i];
            builder.append("line ").append(number);
            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }
        assertEquals(builder.toString(), content);
    }
}