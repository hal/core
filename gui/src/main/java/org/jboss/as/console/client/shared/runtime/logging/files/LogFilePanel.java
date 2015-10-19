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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditor;
import org.jboss.as.console.client.shared.runtime.logging.store.LogFile;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

import static com.google.gwt.dom.client.Style.Unit.EM;
import static com.google.gwt.dom.client.Style.Unit.PX;
import static com.google.gwt.dom.client.Style.VerticalAlign.MIDDLE;
import static org.jboss.as.console.client.shared.util.IdHelper.setId;

/**
 * Shows a log file in a read-only ACE editor.
 *
 * @author Harald Pehl
 */
public class LogFilePanel extends Composite implements LogFilesId {

    private final static int HEADER_HEIGHT = 48 + 35+20;
    private final static int TOOLS_HEIGHT = 32;
    private final static int MARGIN_BOTTOM = 20;

    private final String name;
    private final VerticalPanel panel;
    private final AceEditor editor;
    private final HandlerRegistration resizeHandler;

    public LogFilePanel(final LogFile logFile) {
        this.name = logFile.getName();

        panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");
        panel.getElement().getStyle().setPadding(30, Style.Unit.PX);

        editor = new AceEditor();
        editor.addStyleName("hal-LogViewer");
        editor.addAttachHandler(event -> {
            if (event.isAttached()) {
                Scheduler.get().scheduleDeferred(
                        new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {
                                editor.startEditor();
                                editor.setReadOnly(true);
                                editor.setShowGutter(true);
                                editor.setShowPrintMargin(false);
                                editor.setModeByName("logfile");
                                editor.setThemeByName("logfile");
                                editor.setText(logFile.getContent());
                                editor.setFontSize("11px");
                            }
                        }
                );
            }
        });
        HorizontalPanel editorPanel = new HorizontalPanel();
        editorPanel.setStyleName("fill-layout-width");
        editorPanel.add(editor);

        SearchBox searchBox = new SearchBox(editor.getElement().getId());
        editor.setSearchBox(searchBox.getElement());

        panel.add(new HTML("<h3>" + logFile.getName() + "</h3>"));
        panel.add(searchBox);
        panel.add(editorPanel);

        resizeHandler = Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                LogFilePanel.this.onResize();
            }
        });
        initWidget(panel);
        //setStyleName("rhs-content-panel");
    }

    public void refresh(LogFile logFile) {
        editor.setText(logFile.getContent());
    }

    @Override
    protected void onUnload() {
        editor.destroy();
        resizeHandler.removeHandler();
    }

    public void onResize() {
        Scheduler.get().scheduleDeferred(() -> {
            int panelHeight = panel.getElement().getParentElement().getOffsetHeight();
            int editorHeight = panelHeight - HEADER_HEIGHT - TOOLS_HEIGHT - MARGIN_BOTTOM;

            if (panelHeight > 0) {
                editor.setHeight(editorHeight + "px");
            }
        });
    }

    public String getName() {
        return name;
    }


    /**
     * Simulates AceEditor's build in search box
     */
    @SuppressWarnings("UnusedDeclaration")
    private class SearchBox extends Composite {

        SearchBox(final String editorId) {

            // first part: setup the visible widgets
            final TextBox findTextBox = new TextBox();
            findTextBox.addStyleName("ace_search_field");
            findTextBox.getElement().setAttribute("placeholder", "Find");
            setId(findTextBox, BASE_ID + editorId, "find_input");

            ToolButton findButton = new ToolButton("Find", new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    editor.search(findTextBox.getValue());
                }
            });
            setId(findButton, BASE_ID + editorId, "find");

            Button findPrev = new Button(SafeHtmlUtils.fromSafeConstant("<i class=\"icon-angle-left\"></i>"));
            findPrev.addStyleName("toolstrip-button");
            findPrev.getElement().setAttribute("action", "findPrev"); // AceEditor action wiring
            setId(findPrev, BASE_ID + editorId, "prev_match");

            Button findNext = new Button(SafeHtmlUtils.fromSafeConstant("<i class=\"icon-angle-right\"></i>"));
            findNext.addStyleName("toolstrip-button");
            findNext.getElement().setAttribute("action", "findNext"); // AceEditor action wiring
            setId(findNext, BASE_ID + editorId, "next_match");

            ToolStrip searchTools = new ToolStrip();
            searchTools.addToolWidget(findTextBox);
            searchTools.addToolButton(findButton);
            searchTools.addToolWidget(findPrev);
            searchTools.addToolWidget(findNext);
            searchTools.getElement().getStyle().setPaddingLeft(0, PX);
            searchTools.getElement().getStyle().setMarginBottom(0.5, EM);
            findTextBox.getElement().getStyle().setWidth(30, EM);
            findTextBox.getElement().getStyle().setMarginRight(1, EM);
            findTextBox.getElement().getStyle().setMarginBottom(0, PX);
            findTextBox.getElement().getParentElement().getStyle().setVerticalAlign(MIDDLE);
            findButton.getElement().getStyle().setMarginLeft(1, EM);
            findButton.getElement().getStyle().setHeight(25, PX);
            findButton.getElement().getParentElement().getStyle().setVerticalAlign(MIDDLE);
            findPrev.getElement().getStyle().setHeight(25, PX);
            findPrev.getElement().getParentElement().getStyle().setVerticalAlign(MIDDLE);
            findNext.getElement().getStyle().setHeight(25, PX);
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
            searchBox.getElement().setId(BASE_ID + editorId + "_search_panel");
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
