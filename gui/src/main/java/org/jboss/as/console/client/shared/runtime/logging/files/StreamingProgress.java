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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.shared.patching.ui.Pending;
import org.jboss.as.console.client.shared.runtime.logging.store.LogStore;
import org.jboss.as.console.client.shared.runtime.logging.store.LogStore.PendingStreamingRequest;
import org.jboss.as.console.client.shared.runtime.logging.store.StreamLogFile;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.as.console.client.shared.runtime.logging.files.LogFilesId.BASE_ID;
import static org.jboss.as.console.client.shared.util.IdHelper.WidgetType.BUTTON;
import static org.jboss.as.console.client.shared.util.IdHelper.WidgetType.WINDOW;
import static org.jboss.as.console.client.shared.util.IdHelper.setId;

/**
 * @author Harald Pehl
 */
public class StreamingProgress extends PopupPanel {

    private final static int WIDTH = 360;
    private final static int HEIGHT = 200;

    private final Dispatcher circuit;
    private final int timeout;
    private final Button cancel;

    private String logFile;
    private boolean pending;

    public StreamingProgress(final Dispatcher circuit, final LogStore logStore, int timeout) {
        super(false, true);
        this.circuit = circuit;
        this.timeout = timeout;

        setWidth(WIDTH + "px");
        setHeight(HEIGHT + "px");
        setGlassEnabled(true);
        setId(getElement(), WINDOW, BASE_ID, "stream_in_progress");
        setStyleName("default-window");

        FlowPanel content = new FlowPanel();
        content.addStyleName("stream-log-file-pending");
        content.add(new Pending(Console.CONSTANTS.downloadInProgress()));
        cancel = new Button(Console.CONSTANTS.common_label_cancel());
        setId(cancel.getElement(), BUTTON, BASE_ID, "cancel_stream");
        cancel.addStyleName("cancel");
        cancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                PendingStreamingRequest request = logStore.getPendingStreamingRequest();
                if (request != null) {
                    request.cancel();
                    done();
                }
            }
        });
        content.add(cancel);
        setWidget(content);
    }

    public void monitor(final String logFile) {
        this.logFile = logFile;
        this.circuit.dispatch(new StreamLogFile(logFile));

        // deferred show
        pending = true;
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                // still streaming?
                if (pending) {
                    setPopupPosition((Window.getClientWidth() / 2) - (WIDTH / 2),
                            (Window.getClientHeight() / 2) - (HEIGHT / 2) - 50);
                    StreamingProgress.this.show();
                    cancel.setFocus(true);
                }
                return false;
            }
        }, timeout);
    }

    public void done() {
        pending = false;
        super.hide();
    }
}
