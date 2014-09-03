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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditor;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.ChangePageSize;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.FollowLogFile;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.NavigateInLogFile;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.UnFollowLogFile;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.gwt.circuit.Dispatcher;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static org.jboss.as.console.client.shared.runtime.logviewer.Direction.*;

/**
 * Shows a log file in a read-only ACE editor.
 *
 * @author Harald Pehl
 */
public class LogFilePanel extends Composite {

    private final static int HEADER_HEIGHT = 48;
    private final static int TOOLS_HEIGHT = 32;
    private final static int MARGIN_BOTTOM = 50;

    private final Dispatcher circuit;
    private final String name;
    private final VerticalPanel panel;
    private final AceEditor editor;
    private final LogFileIndicator indicator;
    private final Label position;
    private final HandlerRegistration resizeHandler;
    private final CheckBox follow;
    private final ToolButton head;
    private final ToolButton prev;
    private final ToolButton next;
    private final ToolButton tail;
    private int visibleLines;

    public LogFilePanel(final Dispatcher circuit, final LogFile logFile) {
        this.circuit = circuit;
        this.name = logFile.getName();

        panel = new VerticalPanel();
        HTML header = new HTML("<h3>" + logFile.getName() + "</h3>");
        panel.add(header);

        editor = new AceEditor();
        editor.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (event.isAttached()) {
                    Scheduler.get().scheduleDeferred(
                            new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    editor.startEditor();
                                    editor.setReadOnly(true);
                                    editor.setShowGutter(false);
                                    editor.setShowPrintMargin(false);
                                    editor.setModeByName("logfile");
                                    editor.setThemeByName("logfile");
                                    editor.setText(logFile.getContent());
                                    editor.setSearchPlaceHolder("Find");
                                    editor.setVScrollBarVisible(false);
                                }
                            }
                    );
                }
            }
        });
        indicator = new LogFileIndicator();
        HorizontalPanel editorPanel = new HorizontalPanel();
        editorPanel.setStyleName("fill-layout-width");
        editorPanel.add(editor);
        editorPanel.add(indicator);
        indicator.getElement().getParentElement().getStyle().setWidth(4, PX);
        indicator.getElement().getParentElement().getStyle().setPaddingLeft(4, PX);
        panel.add(editorPanel);

        ToolStrip tools = new ToolStrip();
        follow = new CheckBox("Auto Refresh");
        follow.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (follow.getValue()) {
                    circuit.dispatch(new FollowLogFile());
                } else {
                    circuit.dispatch(new UnFollowLogFile());
                }
            }
        });
        tools.addToolWidget(follow);
        position = new Label();
        position.getElement().setAttribute("style", "padding-right:15px;padding-top:4px;");
        tools.addToolWidgetRight(position);
        head = new ToolButton("Head", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(HEAD);
            }
        });
        tools.addToolButtonRight(head);
        prev = new ToolButton("<i class=\"icon-angle-up\"></i>", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(PREVIOUS);
            }
        });
        tools.addToolButtonRight(prev);
        next = new ToolButton("<i class=\"icon-angle-down\"></i>", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(NEXT);
            }
        });
        tools.addToolButtonRight(next);
        tail = new ToolButton("Tail", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(TAIL);
            }
        });
        tools.addToolButtonRight(tail);
        panel.add(tools);

        resizeHandler = Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                LogFilePanel.this.onResize();
            }
        });

        initWidget(panel);
        setStyleName("rhs-content-panel");
    }

    public void refresh(LogFile logFile, Class<?> actionType) {
        editor.setText(logFile.getContent());
        indicator.refresh(logFile, actionType);
        position.setText("Pos. " + (int) Math.floor(indicator.getRatio()) + " %");
        follow.setValue(logFile.isFollow());
        if (logFile.getLines().size() < visibleLines) {
            head.setEnabled(false);
            prev.setEnabled(false);
            next.setEnabled(false);
            tail.setEnabled(false);
        } else {
            head.setEnabled(!logFile.isHead());
            prev.setEnabled(!logFile.isHead());
            next.setEnabled(!logFile.isTail());
            tail.setEnabled(!logFile.isTail());
        }
    }

    private void onNavigate(Direction direction) {
        if (direction == Direction.HEAD || direction == Direction.PREVIOUS) {
            circuit.dispatch(new UnFollowLogFile());
        }
        circuit.dispatch(new NavigateInLogFile(direction));
    }

    @Override
    protected void onUnload() {
        editor.destroy();
        resizeHandler.removeHandler();
    }

    public void onResize() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                int panelHeight = panel.getElement().getParentElement().getOffsetHeight();
                int editorHeight = panelHeight - HEADER_HEIGHT - TOOLS_HEIGHT - MARGIN_BOTTOM;

                if (panelHeight > 0) {
                    editor.setHeight(editorHeight + "px");
                    visibleLines = editorHeight / 16;
                    circuit.dispatch(new ChangePageSize(visibleLines));
                }
            }
        });
    }

    public String getName() {
        return name;
    }
}
