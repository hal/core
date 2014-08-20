package org.jboss.as.console.client.shared.runtime.logviewer;

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
        when(bootstrap.isStandalone()).thenReturn(true);

        dispatcher = new StaticDispatcher();
        logStore = new LogStore(null, dispatcher, bootstrap);
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

        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertEquals("line 0\nline 1", activeLogFile.getContent());
        assertTrue(activeLogFile.isFollow());
        assertFalse(activeLogFile.isHead());
        assertTrue(activeLogFile.isTail());
        assertFalse(activeLogFile.isStale());
        assertEquals(0, activeLogFile.getSkipped());
    }

    @Test
    public void reopenLogFile() {
        LogFile logFile = new LogFile("server.log", lines(0));
        logStore.states.put(logFile.getName(), logFile);

        // Must not dispatch a DMR operation
        logStore.openLogFile("server.log", NoopChannel.INSTANCE);
        LogFile activeLogFile = logStore.getActiveLogFile();
        assertSame(logFile, activeLogFile);
    }

    @Test
    public void selectLogFile() {
        LogFile logFile = new LogFile("server.log", lines(0));
        logStore.activate(logFile);

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

        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertSame(foo, activeLogFile);
        assertEquals(1, logStore.states.size());
        assertSame(foo, logStore.states.values().iterator().next());
    }

    @Test
    public void closeActiveLogFile() {
        LogFile foo = new LogFile("foo.log", Collections.<String>emptyList());
        logStore.activate(foo);

        logStore.closeLogFile("foo.log", NoopChannel.INSTANCE);

        assertNull(logStore.getActiveLogFile());
        assertTrue(logStore.states.isEmpty());
    }

    @Test
    public void navigateHead() {
        LogFile view = new LogFile("server.log", Collections.<String>emptyList());
        logStore.activate(view);

        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.HEAD, NoopChannel.INSTANCE);

        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertFalse(activeLogFile.isFollow());
        assertTrue(activeLogFile.isHead());
        assertFalse(activeLogFile.isTail());
        assertFalse(activeLogFile.isStale());
        assertEquals(0, activeLogFile.getSkipped());
    }

    @Test
    public void navigateTail() {
        LogFile view = new LogFile("server.log", Collections.<String>emptyList());
        logStore.activate(view);

        dispatcher.push(StaticDmrResponse.ok(logFileNode(2)));
        logStore.navigate(Direction.TAIL, NoopChannel.INSTANCE);

        LogFile activeLogFile = logStore.getActiveLogFile();
        assertNotNull(activeLogFile);
        assertTrue(activeLogFile.isFollow());
        assertFalse(activeLogFile.isHead());
        assertTrue(activeLogFile.isTail());
        assertFalse(activeLogFile.isStale());
        assertEquals(0, activeLogFile.getSkipped());
    }

    @Test
    public void changePageSize() {
        logStore.changePageSize(42, NoopChannel.INSTANCE);
        assertEquals(42, logStore.pageSize);
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
}