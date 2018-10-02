/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.core.message;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.shared.state.ReloadEvent;
import org.jboss.as.console.client.widgets.lists.DefaultCellList;
import org.jboss.as.console.client.widgets.popups.DefaultPopup;
import org.jboss.ballroom.client.widgets.InlineLink;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;

/**
 * @author Greg Hinkle
 * @author Heiko Braun
 */
public class MessageCenterView implements MessageListener, ReloadEvent.ReloadListener {

    private static final String MESSAGE_LABEL = Console.CONSTANTS.common_label_messages();

    private MessageCenter messageCenter;
    private HorizontalPanel messageDisplay;
    final MessageListPopup messagePopup = new MessageListPopup();
    private HTML messageButton;

    @Inject
    public MessageCenterView(MessageCenter messageCenter) {
        this.messageCenter = messageCenter;
    }

    private class MessageListPopup extends DefaultPopup
    {
        private CellList<Message> messageList;

        public MessageListPopup()
        {
            super(Arrow.NONE);

            this.sinkEvents(Event.MOUSEEVENTS);
            setAutoHideEnabled(true);

            SafeHtmlBuilder emptyMessage = new SafeHtmlBuilder();
            emptyMessage.appendHtmlConstant("<div style='padding:10px'>");
            emptyMessage.appendHtmlConstant(Console.CONSTANTS.common_label_noRecentMessages());
            emptyMessage.appendHtmlConstant("</div>");

            MessageCell messageCell = new MessageCell();
            messageList = new DefaultCellList<Message>(messageCell);
            messageList.setTabIndex(-1);
            messageList.addStyleName("message-list");
            messageList.setEmptyListWidget(new HTML(emptyMessage.toSafeHtml()));
            messageList.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.ENABLED);

            final SingleSelectionModel<Message> selectionModel = new SingleSelectionModel<Message>();
            messageList.setSelectionModel(selectionModel);
            selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
                public void onSelectionChange(SelectionChangeEvent event) {
                    Message selected = selectionModel.getSelectedObject();
                    if (selected != null) {
                        if(selected.isSticky())
                        {
                            clearSticky();
                        }

                        showDetail(selected);
                    }
                }
            });

            VerticalPanel panel = new VerticalPanel();
            panel.setStyleName("fill-layout-width");

            panel.add(messageList);
            InlineLink clearBtn = new InlineLink(Console.CONSTANTS.common_label_clear());
            clearBtn.getElement().setAttribute("style", "float:right;padding-right:5px;font-size:10px;");

            clearBtn.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    messageCenter.getMessages().clear();
                    reflectMessageCount();
                    messagePopup.hide();
                }
            });
            panel.add(clearBtn);

            setWidget(panel);

            addCloseHandler(new CloseHandler<PopupPanel>() {
                @Override
                public void onClose(CloseEvent<PopupPanel> event) {
                    reflectMessageCount();
                }
            });

        }

        @Override
        protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
            if (Event.ONKEYDOWN == event.getTypeInt()) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                    // Dismiss when escape is pressed
                    hide();
                }
            }
        }

        public CellList<Message> getMessageList() {
            return messageList;
        }

        public void focusOnFirstMessage() {
            messageList.setFocus(true);
        }
    }

    private void clearSticky() {
        messageDisplay.clear();
    }


    private void showDetail(final Message msg) {

        msg.setNew(false);

        final DefaultWindow window = new DefaultWindow(Console.CONSTANTS.common_label_messageDetailTitle());

        window.setWidth(480);
        window.setHeight(360);
        window.setGlassEnabled(true);


        //ImageResource icon = MessageCenterView.getSeverityIcon(msg.getSeverity());
        //AbstractImagePrototype prototype = AbstractImagePrototype.create(icon);

        SafeHtmlBuilder html = new SafeHtmlBuilder();

        String style = "list-"+msg.getSeverity().getStyle();

        // TODO: XSS prevention?
        html.appendHtmlConstant(msg.getSeverity().getTag());
        html.appendHtmlConstant("&nbsp;");
        html.appendHtmlConstant(msg.getFired().toString());
        html.appendHtmlConstant("<h3 id='consise-message' class='"+style+"' style='padding:10px;box-shadow:none!important;border-width:5px'>");
        html.appendEscaped(msg.getConciseMessage());
        html.appendHtmlConstant("</h3>");
        html.appendHtmlConstant("<p/>");

        String detail = msg.getDetailedMessage() != null ? msg.getDetailedMessage() : "";

        html.appendHtmlConstant("<pre style='font-family:tahoma, verdana, sans-serif;' id='detail-message'>");
        html.appendEscaped(detail);
        html.appendHtmlConstant("</pre>");

        final HTML widget = new HTML(html.toSafeHtml());
        widget.getElement().setAttribute("style", "margin:5px");


        DialogueOptions options = new DialogueOptions(
                Console.CONSTANTS.dismiss(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        window.hide();
                    }
                },
                Console.CONSTANTS.common_label_cancel(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        window.hide();
                    }
                }
        ).showCancel(false);

        options.getSubmit().setAttribute("aria-describedby", "consise-message detail-message");

        Widget windowContent = new WindowContentBuilder(widget, options).build();

        TrappedFocusPanel trap = new TrappedFocusPanel(windowContent)
        {
            @Override
            protected void onAttach() {
                super.onAttach();

                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        getFocus().onFirstButton();
                    }
                });
            }
        };

        window.setWidget(trap);

        window.addCloseHandler(new CloseHandler<PopupPanel>() {

            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                messagePopup.getMessageList().getSelectionModel().setSelected(msg, false);
                messagePopup.hide();
            }
        });

        messagePopup.hide();
        window.center();
    }

    public Widget asWidget()
    {

        HorizontalPanel layout = new HorizontalPanel();
        layout.getElement().setAttribute("title", "Notification Center");
        layout.setStyleName("notification-center");

        messageButton = new HTML(MESSAGE_LABEL+": "+messageCenter.getNewMessageCount());
        messageButton.addStyleName("notification-button");

        ClickHandler clickHandler = new ClickHandler() {
            public void onClick(ClickEvent event) {

                int numMessages = fetchMessages(messagePopup);
                if(numMessages==0)numMessages=1;

                int width = 250;
                int height = numMessages*35;

                int btnRight = messageButton.getAbsoluteLeft()+messageButton.getOffsetWidth();

                messagePopup.setPopupPosition(
                        btnRight-width,// - (width+10- messageButton.getOffsetWidth()) ,
                        messageButton.getAbsoluteTop() + 25
                );

                messagePopup.show();
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        messagePopup.focusOnFirstMessage();
                    }
                });


                messagePopup.setWidth(width+"px");
                messagePopup.setHeight(height+"px");
            }
        };

        messageButton.addClickHandler(clickHandler);

        messageDisplay = new HorizontalPanel();
        messageDisplay.getElement().setAttribute("role", "log");
        messageDisplay.getElement().setAttribute("aria-live", "polite");
        messageDisplay.getElement().setAttribute("aria-atomic", "true");

        layout.add(messageDisplay);
        layout.add(messageButton);

        messageDisplay.getElement().getParentElement().setAttribute("style", "width:100%;padding-right:5px");
        messageDisplay.getElement().getParentElement().setAttribute("align", "right");

        messageButton.getElement().getParentElement().setAttribute("style", "width:60px");
        messageButton.getElement().getParentElement().setAttribute("align", "right");

        // register listener
        messageCenter.addMessageListener(this);
        Console.getEventBus().addHandler(ReloadEvent.TYPE, this);

        return layout;
    }

    private int fetchMessages(MessageListPopup popup) {
        List<Message> messages = messageCenter.getMessages();
        popup.getMessageList().setRowCount(messages.size(), true);
        popup.getMessageList().setRowData(0, messages);
        return messages.size();
    }

    public void onMessage(final Message message) {
        if (!message.isTransient()) {

            // update the visible message count
            reflectMessageCount();

            final PopupPanel display = createDisplay(message);
            displayNotification(display, message);

            if (!message.isSticky()) {

                Timer hideTimer = new Timer() {
                    @Override
                    public void run() {
                        // hide message
                        messageDisplay.clear();
                        display.hide();
                    }
                };

                hideTimer.schedule(4000);
            }

        }
    }

    private void reflectMessageCount() {
        int numMessages = messageCenter.getNewMessageCount();
        messageButton.setHTML(MESSAGE_LABEL+": "+ numMessages);
    }

    private PopupPanel createDisplay(Message message) {
        PopupPanel displayPopup = new PopupPanel() {
            {
                this.sinkEvents(Event.ONKEYDOWN);

                getElement().setAttribute("role", "alert");
                getElement().setAttribute("aria-live", "assertive");

                if(!message.isSticky()) {
                    setAutoHideEnabled(true);
                    setAutoHideOnHistoryEventsEnabled(true);
                }
            }

            @Override
            protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                if (Event.ONKEYDOWN == event.getTypeInt()) {
                    if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                        // Dismiss when escape is pressed
                        hide();
                    }
                }
            }
        };

        displayPopup.addStyleName("back");
        return displayPopup;
    }

    private void displayNotification(final PopupPanel display, final Message message) {


        final int MAX = 65;
        String actualMessage = message.getConciseMessage().length()> MAX ?
                message.getConciseMessage().substring(0, MAX)+" ..." :
                message.getConciseMessage();

        if(message.isSticky())
        {
            display.addStyleName("front");
        }
        else
        {
            display.removeStyleName("front");
        }

        // display structure

        LayoutPanel container = new LayoutPanel();
        container.addStyleName("notification-display");
        container.addStyleName(message.severity.getStyle());
        container.addStyleName("fill-layout");

        final HTML msg = new HTML(message.getSeverity().getTag()+"&nbsp;"+ SafeHtmlUtils.fromString(actualMessage).asString());

        ClickHandler msgClickHandler = new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                messageDisplay.clear();
                display.hide();
                showDetail(message);
            }
        };
        msg.addClickHandler(msgClickHandler);


        container.add(msg);

        HTML closeIcon = new HTML("<i class='icon-remove'></i>");
        closeIcon.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                display.hide();
            }
        });

        closeIcon.getElement().getStyle().setCursor(Style.Cursor.POINTER);

        container.add(closeIcon);

        container.setWidgetLeftWidth(msg, 5, Style.Unit.PX, 95, Style.Unit.PCT);
        container.setWidgetTopHeight(msg, 15, Style.Unit.PX, 100, Style.Unit.PCT);
        container.setWidgetRightWidth(closeIcon, 10, Style.Unit.PX, 15, Style.Unit.PX);
        container.setWidgetTopHeight(closeIcon, 15, Style.Unit.PX, 100, Style.Unit.PCT);
        display.setWidget(container);

        int width=500;
        int height=25;

        display.setPopupPosition(
                (Window.getClientWidth()-width)/2,
                50
        );

        display.show();

        display.setWidth(width + "px");
        display.setHeight(height + "px");

    }

    @Override
    public void onReload() {
        clearSticky();
    }
}
