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

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.CloseLogFile;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.PauseFollowLogFile;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.SelectLogFile;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Harald Pehl
 */
public class LogFileTabs extends Composite {

    private final Dispatcher circuit;
    private final DefaultTabLayoutPanel tabLayout;

    public LogFileTabs(final Dispatcher circuit) {
        this.circuit = circuit;
        this.tabLayout = new DefaultTabLayoutPanel(40, Style.Unit.PX, true, true);
        this.tabLayout.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                LogFilePanel logFilePanel = selectedLogFilePanel();
                if (logFilePanel != null) {
                    circuit.dispatch(new SelectLogFile(logFilePanel.getName()));
                    logFilePanel.onResize();
                } else {
                    circuit.dispatch(new PauseFollowLogFile());
                }
            }
        });
        this.tabLayout.addCloseHandler(new CloseHandler<Widget>() {
            @Override
            public void onClose(CloseEvent<Widget> event) {
                if (event.getTarget() instanceof LogFilePanel) {
                    LogFilePanel logFilePanel = (LogFilePanel) event.getTarget();
                    circuit.dispatch(new CloseLogFile(logFilePanel.getName()));
                }
            }
        });

        initWidget(tabLayout);
        addStyleName("default-tabpanel");
    }

    public void open(LogFile logFile) {
        if (!tabLayout.contains(logFile.getName())) {
            tabLayout.add(new LogFilePanel(circuit, logFile), logFile.getName());
        }
        tabLayout.selectTab(logFile.getName());
    }

    public void refresh(LogFile logFile, Action action) {
        LogFilePanel logFilePanel = selectedLogFilePanel();
        if (logFilePanel != null) {
            logFilePanel.refresh(logFile, action);
        }
    }

    public void add(Widget child, String text) {
        tabLayout.add(child, text);
    }

    LogFilePanel selectedLogFilePanel() {
        Widget widget = tabLayout.getWidget(tabLayout.getSelectedIndex());
        if (widget instanceof LogFilePanel) {
            return (LogFilePanel) widget;
        }
        return null;
    }
}
