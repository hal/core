package org.jboss.as.console.client.shared.runtime.logging.store;

import com.google.gwt.core.client.Scheduler;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.runtime.logging.viewer.Position;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.StaticDispatcher;
import org.jboss.dmr.client.StaticDmrResponse;
import org.jboss.gwt.circuit.NoopChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.jboss.as.console.client.shared.runtime.logging.viewer.Direction.*;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogStoreTest {

    private StaticDispatcher dispatcher;
    private LogStore store;

    @Before
    public void setUp() {
        BootstrapContext bootstrap = mock(BootstrapContext.class);
        Scheduler scheduler = mock(Scheduler.class);
        when(bootstrap.isStandalone()).thenReturn(true);

        dispatcher = new StaticDispatcher();
        store = new LogStore(null, dispatcher, scheduler, bootstrap);
    }


    // ------------------------------------------------------ test methods

    @Test
    public void readLogFiles() {
        dispatcher.push(StaticDmrResponse.ok(logFileNodes("server.log", "server.log.2014.-08-01", "server.log.2014.-08-02")));
        store.readLogFiles(NoopChannel.INSTANCE);

        assertNull(store.getActiveLogFile());
        assertEquals(3, store.getLogFiles().size());
    }

    @Test
    public void readLogFilesAndVerifyStale() {
        LogFile stale = new LogFile("stale.log", Collections.<String>emptyList(), 0);
        store.states.put(stale.getName(), stale);

        dispatcher.push(StaticDmrResponse.ok(logFileNodes("server.log")));
        store.readLogFiles(NoopChannel.INSTANCE);

        // "stale.log" is no longer in the list of log files and must be stale
        assertTrue(store.states.get(stale.getName()).isStale());
    }

    @Test
    public void openLogFile() {
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.logFiles.add(logFileNode("server.log"));
        store.openLogFile(new OpenLogFile("server.log"), NoopChannel.INSTANCE);

        assertFalse(store.pauseFollow);
        LogFile activeLogFile = store.getActiveLogFile();
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
        LogFile logFile = new LogFile("server.log", lines(0), 0);
        store.states.put(logFile.getName(), logFile);

        assertFalse(store.pauseFollow);
        // Must not dispatch a DMR operation
        store.openLogFile(new OpenLogFile("server.log"), NoopChannel.INSTANCE);
        LogFile activeLogFile = store.getActiveLogFile();
        assertSame(logFile, activeLogFile);
    }

    @Test
    public void selectLogFile() {
        LogFile logFile = new LogFile("server.log", lines(0), 0);
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        assertFalse(store.pauseFollow);
        // Must not dispatch a DMR operation
        store.selectLogFile(new SelectLogFile("server.log"), NoopChannel.INSTANCE);
        LogFile activeLogFile = store.getActiveLogFile();
        assertSame(logFile, activeLogFile);
    }

    @Test
    public void closeLogFile() {
        LogFile foo = new LogFile("foo.log", Collections.<String>emptyList(), 0);
        LogFile bar = new LogFile("bar.log", Collections.<String>emptyList(), 0);
        store.states.put(foo.getName(), foo);
        store.states.put(bar.getName(), bar);
        store.activate(foo);

        store.closeLogFile(new CloseLogFile("bar.log"), NoopChannel.INSTANCE);

        assertFalse(store.pauseFollow);
        LogFile activeLogFile = store.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertSame(foo, activeLogFile);
        assertEquals(1, store.states.size());
        assertSame(foo, store.states.values().iterator().next());
    }

    @Test
    public void closeActiveLogFile() {
        LogFile logFile = new LogFile("server.log", Collections.<String>emptyList(), 0);
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        store.closeLogFile(new CloseLogFile("server.log"), NoopChannel.INSTANCE);

        assertTrue(store.pauseFollow);
        assertNull(store.getActiveLogFile());
        assertTrue(store.states.isEmpty());
    }

    @Test
    public void navigateHead() {
        LogFile logFile = new LogFile("server.log", lines(2), 0);
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(HEAD), NoopChannel.INSTANCE);

        // 1. verify DMR operation
        ModelNode operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(0, operation.get("skip").asInt());

        // 2. verify log file state
        LogFile activeLogFile = store.getActiveLogFile();
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
        LogFile logFile = new LogFile("server.log", lines(2), 0);
        logFile.goTo(Position.HEAD);
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(TAIL), NoopChannel.INSTANCE);

        // 1. verify DMR operation
        ModelNode operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(0, operation.get("skip").asInt());

        // 2. verify log file state
        LogFile activeLogFile = store.getActiveLogFile();
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
        LogFile logFile = new LogFile("server.log", lines(2), 0);
        store.pageSize = 2;
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(PREVIOUS), NoopChannel.INSTANCE);

        // 1. verify DMR operation
        ModelNode operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 2. verify log file state
        LogFile activeLogFile = store.getActiveLogFile();
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
        LogFile logFile = new LogFile("server.log", lines(2), 0);
        store.pageSize = 2;
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        // Prev (1)
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(PREVIOUS), NoopChannel.INSTANCE);

        // Prev (2)
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(PREVIOUS), NoopChannel.INSTANCE);

        // 1 verify DMR operation
        ModelNode operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(4, operation.get("skip").asInt());

        // 2 verify log file state
        LogFile activeLogFile = store.getActiveLogFile();
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
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(NEXT), NoopChannel.INSTANCE);

        // 3.1 verify DMR operation
        operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 3.2 verify log file state
        activeLogFile = store.getActiveLogFile();
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
        LogFile logFile = new LogFile("server.log", lines(2), 0);
        store.pageSize = 2;
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        // Prev
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(PREVIOUS), NoopChannel.INSTANCE);

        // 1.1 verify DMR operation
        ModelNode operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertTrue(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 1.2 verify log file state
        LogFile activeLogFile = store.getActiveLogFile();
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
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(HEAD), NoopChannel.INSTANCE);

        // 2.1 verify DMR operation
        operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(0, operation.get("skip").asInt());

        // 2.2 verify log file state
        activeLogFile = store.getActiveLogFile();
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
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(NEXT), NoopChannel.INSTANCE);

        // 3.1 verify DMR operation
        operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 3.2 verify log file state
        activeLogFile = store.getActiveLogFile();
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
        LogFile logFile = new LogFile("server.log", lines(2), 0);
        logFile.goTo(Position.HEAD);
        store.pageSize = 2;
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(NEXT), NoopChannel.INSTANCE);

        // 1. verify DMR operation
        ModelNode operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 2. verify log file state
        LogFile activeLogFile = store.getActiveLogFile();
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
        LogFile logFile = new LogFile("server.log", lines(2), 0);
        logFile.goTo(Position.HEAD);
        store.pageSize = 2;
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        // Next (1)
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(NEXT), NoopChannel.INSTANCE);

        // 1.1 verify DMR operation
        ModelNode operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 1.2 verify log file state
        LogFile activeLogFile = store.getActiveLogFile();
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
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(NEXT), NoopChannel.INSTANCE);

        // 2.1 verify DMR operation
        operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(4, operation.get("skip").asInt());

        // 1.2 verify log file state
        activeLogFile = store.getActiveLogFile();
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
        dispatcher.push(StaticDmrResponse.ok(comp(logFileNodes("server.log"), linesNode(2))));
        store.navigate(new NavigateInLogFile(PREVIOUS), NoopChannel.INSTANCE);

        // 3.1 verify DMR operation
        operation = dispatcher.getLastOperation().get("steps").asList().get(1);
        assertNotNull(operation);
        assertFalse(operation.get("tail").asBoolean());
        assertEquals(2, operation.get("skip").asInt());

        // 3.2 verify log file state
        activeLogFile = store.getActiveLogFile();
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
        store.changePageSize(new ChangePageSize(42), NoopChannel.INSTANCE);
        assertEquals(42, store.pageSize);
    }

    @Test
    public void follow() {
        LogFile logFile = new LogFile("server.log", Collections.<String>emptyList(), 0);
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        store.follow(NoopChannel.INSTANCE);

        assertFalse(store.pauseFollow);
        LogFile activeLogFile = store.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertTrue(activeLogFile.isFollow());
    }

    @Test
    public void pauseFollow() {
        LogFile logFile = new LogFile("server.log", Collections.<String>emptyList(), 0);
        logFile.setFollow(true);
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        store.pauseFollow(NoopChannel.INSTANCE);

        assertTrue(store.pauseFollow);
        LogFile activeLogFile = store.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertTrue(activeLogFile.isFollow());
    }

    @Test
    public void unFollow() {
        LogFile logFile = new LogFile("server.log", Collections.<String>emptyList(), 0);
        logFile.setFollow(true);
        store.states.put(logFile.getName(), logFile);
        store.activate(logFile);

        store.unFollow(NoopChannel.INSTANCE);

        assertFalse(store.pauseFollow);
        LogFile activeLogFile = store.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertFalse(activeLogFile.isFollow());
    }


    // ------------------------------------------------------ node factory methods

    private ModelNode comp(ModelNode logFileNodes, ModelNode linesNode) {
        ModelNode step1 = new ModelNode();
        step1.get(RESULT).set(logFileNodes);
        ModelNode step2 = new ModelNode();
        step2.get(RESULT).set(linesNode);

        ModelNode comp = new ModelNode();
        comp.get("step-1").set(step1);
        comp.get("step-2").set(step2);
        return comp;
    }

    private ModelNode logFileNodes(String... names) {
        ModelNode node = new ModelNode();
        for (String name : names) {
            node.add(logFileNode(name));
        }
        return node;
    }

    private ModelNode logFileNode(String name) {
        ModelNode node = new ModelNode();
        node.get("file-name").set(name);
        node.get("file-size").set(42);
        return node;
    }

    private ModelNode linesNode(int numberOfLines) {
        ModelNode node = new ModelNode();
        for (String line : lines(numberOfLines)) {
            node.add(line);
        }
        return node;
    }

    private List<String> lines(int numberOfLines) {
        List<String> lines = new LinkedList<>();
        for (int i = 0; i < numberOfLines; i++) {
            lines.add("line " + i);
        }
        return lines;
    }


    // ------------------------------------------------------ helper methods

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