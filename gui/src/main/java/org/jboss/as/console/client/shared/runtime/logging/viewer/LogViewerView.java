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
package org.jboss.as.console.client.shared.runtime.logging.viewer;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.runtime.logging.store.LogFile;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

public class LogViewerView extends SuspendableViewImpl implements LogViewerPresenter.MyView {

    private final Dispatcher circuit;
    private LogFileTable logFiles;
    private LogFileTabs logFileTabs;
    private LogViewerPresenter presenter;


    @Inject
    public LogViewerView(Dispatcher circuit) {
        this.circuit = circuit;
    }

    @Override
    public Widget createWidget() {
        logFileTabs = new LogFileTabs(circuit);
        logFiles = new LogFileTable(circuit);
        logFileTabs.add(logFiles.asWidget(), "Log Files");
        return logFileTabs;
    }

    @Override
    public void list(List<ModelNode> files) {
        logFiles.list(files);
    }

    @Override
    public void open(LogFile logFile) {
        logFileTabs.open(logFile);
    }

    @Override
    public void refresh(LogFile logFile, Action action) {
        logFileTabs.refresh(logFile, action);
    }

    @Override
    public boolean isLogFileSelected() {
        return logFileTabs.selectedLogFilePanel() != null;
    }
}
