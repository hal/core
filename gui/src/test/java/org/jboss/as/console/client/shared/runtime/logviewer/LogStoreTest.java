package org.jboss.as.console.client.shared.runtime.logviewer;

import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.StaticDispatcher;
import org.jboss.dmr.client.StaticDmrResponse;
import org.jboss.gwt.circuit.util.NoopChannel;
import org.junit.Before;
import org.junit.Test;

import static org.jboss.as.console.client.shared.runtime.logviewer.Position.HEAD;
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
        dispatcher.push(StaticDmrResponse.ok(logFiles("server.log", "server.log.2014.-08-01", "server.log.2014.-08-02")));
        logStore.readLogFiles(NoopChannel.INSTANCE);

        assertNull(logStore.getActiveState());
        assertEquals(3, logStore.getLogFiles().size());
    }

    @Test
    public void readAndStaleLogFiles() {
        LogState stale = new LogState("stale.log", "");
        logStore.states.put(stale.getName(), stale);

        dispatcher.push(StaticDmrResponse.ok(logFiles("server.log")));
        logStore.readLogFiles(NoopChannel.INSTANCE);

        // "stale.log" is no longer in the list of log files and must be stale
        assertTrue(logStore.states.get(stale.getName()).isStale());
    }

    @Test
    public void openLogFile() {
        dispatcher.push(StaticDmrResponse.ok(lines("line 1", "line 2")));
        logStore.selectLogFile("server.log", NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertNotNull(activeView);
        assertEquals("line 1\nline 2", activeView.getContent());
        assertTrue(activeView.isAutoRefresh());
        assertFalse(activeView.isHead());
        assertTrue(activeView.isTail());
        assertFalse(activeView.isStale());
        assertEquals(0, activeView.getLineNumber());
    }

    @Test
    public void reopenLogFile() {
        LogState view = new LogState("server.log", "line 1");
        view.setAutoRefresh(true);
        logStore.states.put(view.getName(), view);

        dispatcher.push(StaticDmrResponse.ok(lines("line 2", "line 3")));
        logStore.selectLogFile("server.log", NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertNotNull(activeView);
        assertEquals("line 2\nline 3", activeView.getContent());
        assertTrue(activeView.isAutoRefresh());
        assertFalse(activeView.isHead());
        assertTrue(activeView.isTail());
        assertFalse(activeView.isStale());
        assertEquals(0, activeView.getLineNumber());
    }

    @Test
    public void openNonAutoRefreshLogFile() {
        LogState view = new LogState("server.log", "");
        view.setAutoRefresh(false);
        logStore.states.put(view.getName(), view);

        // Must not dispatch a DMR operation
        logStore.selectLogFile("server.log", NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertSame(activeView, view);
    }

    @Test
    public void closeLogFile() {
        LogState foo = new LogState("foo.log", "");
        logStore.states.put(foo.getName(), foo);
        logStore.activate(foo);

        LogState bar = new LogState("bar.log", "");
        logStore.states.put(bar.getName(), bar);

        logStore.closeLogFile("bar.log", NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertNotNull(activeView);
        assertSame(foo, activeView);
        assertEquals(1, logStore.states.size());
        assertSame(foo, logStore.states.values().iterator().next());
    }

    @Test
    public void closeActiveLogFile() {
        LogState foo = new LogState("foo.log", "");
        logStore.activate(foo);

        logStore.closeLogFile("foo.log", NoopChannel.INSTANCE);

        assertNull(logStore.getActiveState());
        assertTrue(logStore.states.isEmpty());
    }

    @Test
    public void refresh() {
        LogState view = new LogState("server.log", "line 1");
        view.goTo(HEAD);
        logStore.activate(view);

        dispatcher.push(StaticDmrResponse.ok(lines("line 2", "line 3")));
        logStore.refresh(NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertNotNull(activeView);
        assertEquals("line 2\nline 3", activeView.getContent());
        assertTrue(activeView.isAutoRefresh());
        assertFalse(activeView.isHead());
        assertTrue(activeView.isTail());
        assertFalse(activeView.isStale());
        assertEquals(0, activeView.getLineNumber());
    }

    @Test
    public void navigateHead() {
        LogState view = new LogState("server.log", "");
        logStore.activate(view);

        dispatcher.push(StaticDmrResponse.ok(lines("content", "is", "irrelevant")));
        logStore.navigate(Direction.HEAD, NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertNotNull(activeView);
        assertFalse(activeView.isAutoRefresh());
        assertTrue(activeView.isHead());
        assertFalse(activeView.isTail());
        assertFalse(activeView.isStale());
        assertEquals(0, activeView.getLineNumber());
    }

    @Test
    public void navigatePrevious() {
        LogState view = new LogState("server.log", "");
        view.goTo(2);
        logStore.pageSize = 1;
        logStore.activate(view);

        dispatcher.push(StaticDmrResponse.ok(lines("content", "is", "irrelevant")));
        logStore.navigate(Direction.PREVIOUS, NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertNotNull(activeView);
        assertFalse(activeView.isAutoRefresh());
        assertFalse(activeView.isHead());
        assertFalse(activeView.isTail());
        assertFalse(activeView.isStale());
        assertEquals(3, activeView.getLineNumber());
    }

    @Test
    public void navigateNext() {
        LogState view = new LogState("server.log", "");
        view.goTo(2);
        logStore.pageSize = 1;
        logStore.activate(view);

        dispatcher.push(StaticDmrResponse.ok(lines("content", "is", "irrelevant")));
        logStore.navigate(Direction.NEXT, NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertNotNull(activeView);
        assertFalse(activeView.isAutoRefresh());
        assertFalse(activeView.isHead());
        assertFalse(activeView.isTail());
        assertFalse(activeView.isStale());
        assertEquals(3, activeView.getLineNumber());
    }

    @Test
    public void navigateTail() {
        LogState view = new LogState("server.log", "");
        logStore.activate(view);

        dispatcher.push(StaticDmrResponse.ok(lines("content", "is", "irrelevant")));
        logStore.navigate(Direction.TAIL, NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertNotNull(activeView);
        assertTrue(activeView.isAutoRefresh());
        assertFalse(activeView.isHead());
        assertTrue(activeView.isTail());
        assertFalse(activeView.isStale());
        assertEquals(0, activeView.getLineNumber());
    }

    @Test
    public void unfollow() {
        LogState view = new LogState("server.log", "");
        logStore.activate(view);

        logStore.unfollow(NoopChannel.INSTANCE);

        LogState activeView = logStore.getActiveState();
        assertNotNull(activeView);
        assertFalse(activeView.isAutoRefresh());
    }

    @Test
    public void changePageSize() {
        logStore.changePageSize(42, NoopChannel.INSTANCE);
        assertEquals(42, logStore.pageSize);
    }

    private ModelNode logFiles(String... names) {
        ModelNode payload = new ModelNode();
        for (String name : names) {
            ModelNode logFile = new ModelNode();
            logFile.get("file-name").set(name);
            payload.add(logFile);
        }
        return payload;
    }

    private ModelNode lines(String... lines) {
        ModelNode payload = new ModelNode();
        for (String line : lines) {
            payload.add(line);
        }
        return payload;
    }
}