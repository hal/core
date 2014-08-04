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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditor;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorTheme;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.ChangePageSize;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.NavigateInLogFile;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.gwt.circuit.Dispatcher;

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
    private final HTML header;
    private final AceEditor editor;
    private final ToolStrip tools;
    private final HandlerRegistration resizeHandler;

    public LogFilePanel(Dispatcher circuit, final LogState logState) {
        this.circuit = circuit;
        this.name = logState.getName();

        panel = new VerticalPanel();
        header = new HTML("<h3>" + logState.getName() + "</h3>");
        panel.add(header);

        editor = new AceEditor();
        panel.add(editor);
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
                                    editor.setMode(AceEditorMode.TEXT);
                                    editor.setTheme(AceEditorTheme.TERMINAL);
                                    editor.setText(logState.getContent());
                                }
                            }
                    );
                }
            }
        });
        resizeHandler = Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                resizeEditor();
            }
        });

        tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton("Head", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(HEAD);
            }
        }));
        tools.addToolButtonRight(new ToolButton("<i class=\"icon-angle-up\"></i>", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(PREVIOUS);
            }
        }));
        tools.addToolButtonRight(new ToolButton("<i class=\"icon-angle-down\"></i>", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(NEXT);
            }
        }));
        tools.addToolButtonRight(new ToolButton("Tail", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(TAIL);
            }
        }));
        panel.add(tools);

        initWidget(panel);
        setStyleName("rhs-content-panel");
    }

    public void refresh(LogState logState) {
        editor.setText(logState.getContent());
    }

    private void onNavigate(Direction direction) {
        circuit.dispatch(new NavigateInLogFile(direction));
    }

    @Override
    protected void onUnload() {
        editor.destroy();
        resizeHandler.removeHandler();
    }

    public void resizeEditor() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                int panelHeight = panel.getElement().getParentElement().getOffsetHeight();
                int editorHeight = panelHeight - HEADER_HEIGHT - TOOLS_HEIGHT - MARGIN_BOTTOM;

                if (panelHeight > 0) {
                    editor.setHeight(editorHeight + "px");
                    int lines = editorHeight / 16; // line-height
                    circuit.dispatch(new ChangePageSize(lines));
                }
            }
        });
    }

    public String getName() {
        return name;
    }
}
