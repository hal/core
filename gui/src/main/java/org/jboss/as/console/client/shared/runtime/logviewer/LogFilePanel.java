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
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditor;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.ChangePageSize;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.FollowLogFile;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.NavigateInLogFile;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.UnFollowLogFile;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import static com.google.gwt.dom.client.Style.Unit.EM;
import static com.google.gwt.dom.client.Style.Unit.PX;
import static com.google.gwt.dom.client.Style.VerticalAlign.MIDDLE;
import static org.jboss.as.console.client.shared.runtime.logviewer.Direction.*;
import static org.jboss.as.console.client.shared.util.IdHelper.setId;

/**
 * Shows a log file in a read-only ACE editor.
 *
 * @author Harald Pehl
 */
public class LogFilePanel extends Composite implements LogViewerId {

    private final static int HEADER_HEIGHT = 48 + 35;
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
        panel.setStyleName("rhs-content-panel");
        panel.add(new HTML("<h3>" + logFile.getName() + "</h3>"));
        panel.add(new SearchBox());

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
        setId(follow, BASE_ID, "auto_refresh");

        position = new Label();
        position.getElement().setAttribute("style", "padding-right:15px;padding-top:4px;");
        setId(position, BASE_ID, "position");

        head = new ToolButton("Head", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(HEAD);
            }
        });
        setId(head, BASE_ID, "head");

        prev = new ToolButton("<i class=\"icon-angle-up\"></i>", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(PREVIOUS);
            }
        });
        setId(prev, BASE_ID, "prev");

        next = new ToolButton("<i class=\"icon-angle-down\"></i>", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(NEXT);
            }
        });
        setId(next, BASE_ID, "next");

        tail = new ToolButton("Tail", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNavigate(TAIL);
            }
        });
        setId(tail, BASE_ID, "tail");

        ToolStrip navigationTools = new ToolStrip();
        navigationTools.addToolWidget(follow);
        navigationTools.addToolWidgetRight(position);
        navigationTools.addToolButtonRight(head);
        navigationTools.addToolButtonRight(prev);
        navigationTools.addToolButtonRight(next);
        navigationTools.addToolButtonRight(tail);
        panel.add(navigationTools);

        resizeHandler = Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                LogFilePanel.this.onResize();
            }
        });
        initWidget(panel);
        setStyleName("rhs-content-panel");
    }

    public void refresh(LogFile logFile, Action action) {
        editor.setText(logFile.getContent());
        indicator.refresh(logFile, action);
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


    /**
     * Simulates AceEditor's build in search box
     */
    private class SearchBox extends Composite {

        public SearchBox() {

            // first part: setup the visible widgets
            final TextBox findTextBox = new TextBox();
            findTextBox.addStyleName("ace_search_field");
            findTextBox.getElement().setAttribute("placeholder", "Find");
            setId(findTextBox, BASE_ID, "find_input");

            ToolButton findButton = new ToolButton("Find", new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    editor.search(findTextBox.getValue());
                }
            });
            setId(findButton, BASE_ID, "find");

            Button findPrev = new Button(SafeHtmlUtils.fromSafeConstant("<i class=\"icon-angle-left\"></i>"));
            findPrev.addStyleName("toolstrip-button");
            findPrev.getElement().setAttribute("action", "findPrev"); // AceEditor action wiring
            setId(findPrev, BASE_ID, "prev_match");

            Button findNext = new Button(SafeHtmlUtils.fromSafeConstant("<i class=\"icon-angle-right\"></i>"));
            findNext.addStyleName("toolstrip-button");
            findNext.getElement().setAttribute("action", "findNext"); // AceEditor action wiring
            setId(findNext, BASE_ID, "next_match");

            ToolStrip searchTools = new ToolStrip();
            searchTools.addToolWidget(findTextBox);
            searchTools.addToolButton(findButton);
            searchTools.addToolWidget(findPrev);
            searchTools.addToolWidget(findNext);
            findTextBox.getElement().getStyle().setWidth(20, EM);
            findTextBox.getElement().getStyle().setMarginBottom(0, PX);
            findTextBox.getElement().getParentElement().getStyle().setVerticalAlign(MIDDLE);
            findButton.getElement().getParentElement().getStyle().setVerticalAlign(MIDDLE);
            findPrev.getElement().getParentElement().getStyle().setVerticalAlign(MIDDLE);
            findNext.getElement().getParentElement().getStyle().setVerticalAlign(MIDDLE);

            // next part: rebuild the original search box
            FlowPanel searchForm = div("ace_search_form", false);
            searchForm.add(searchTools);

            FlowPanel replaceForm = div("ace_replace_form", true);
            replaceForm.add(hiddenTextBox("ace_search_field"));
            replaceForm.add(hiddenButton("replaceAndFindNext", "ace_replacebtn"));
            replaceForm.add(hiddenButton("replaceAll", "ace_replacebtn"));

            FlowPanel searchOptions = div("ace_search_options", true);
            searchOptions.add(hiddenButton("toggleRegexpMode", "ace_button"));
            searchOptions.add(hiddenButton("toggleCaseSensitive", "ace_button"));
            searchOptions.add(hiddenButton("toggleWholeWords", "ace_button"));

            FlowPanel searchBox = div("ace_search_log_viewer", false);
            searchBox.add(hiddenButton("close", "ace_searchbtn_close"));
            searchBox.add(searchForm);
            searchBox.add(replaceForm);
            searchBox.add(searchOptions);

            initWidget(searchBox);
        }

        private FlowPanel div(String styleName, boolean hidden) {
            FlowPanel div = new FlowPanel();
            div.setStyleName(styleName);
            if (hidden) {
                div.setVisible(false);
            }
            return div;
        }

        private Button hiddenButton(String action, String styleName) {
            Button button = new Button();
            button.setStyleName(styleName);
            button.getElement().setAttribute("action", action);
            button.setVisible(false);
            return button;
        }

        private TextBox hiddenTextBox(String styleName) {
            TextBox textBox = new TextBox();
            textBox.setStyleName(styleName);
            textBox.setVisible(false);
            return textBox;
        }
    }
}
