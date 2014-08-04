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
package org.jboss.as.console.client.widgets.tabs;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.widgets.lists.DefaultCellList;
import org.jboss.as.console.client.widgets.popups.DefaultPopup;

import java.util.List;

class OffPageSelector extends Composite implements ClickHandler {

    private static final String STYLE_NAME = "hal-TabLayout-offPageMenu";
    private final DefaultTabLayoutPanel tabLayout;
    private final OffPagePopup popup;

    OffPageSelector(final DefaultTabLayoutPanel tabLayout) {
        this.tabLayout = tabLayout;
        this.popup = new OffPagePopup();

        InlineHTML selector = new InlineHTML(Templates.TEMPLATES.selectorIcon());
        selector.addClickHandler(this);

        initWidget(selector);
        setStyleName(STYLE_NAME);
    }

    @Override
    public void onClick(ClickEvent event) {
        if (!popup.isShowing()) {
            popup.setValues(tabLayout.getOffPageContainer().getTexts());

            int width = 200;
            int height = tabLayout.getOffPageContainer().getTexts().size() * 35;

            popup.setPopupPosition(getAbsoluteLeft() - (width - getOffsetWidth() - 35),
                    getAbsoluteTop() + 42);
            popup.show();
            popup.setWidth(width + "px");
            popup.setHeight(height + "px");

            event.stopPropagation();
        }
    }


    class OffPagePopup extends DefaultPopup {
        private CellList<OffPageText> texts;
        private ListDataProvider<OffPageText> dataProvider;
        private final SingleSelectionModel<OffPageText> selectionModel;

        OffPagePopup() {
            super(Arrow.TOP);
            this.dataProvider = new ListDataProvider<>();
            this.sinkEvents(Event.MOUSEEVENTS);

            Cell<OffPageText> textCell = new AbstractCell<OffPageText>() {
                @Override
                public void render(Context context, OffPageText value, SafeHtmlBuilder sb) {
                    sb.appendEscaped(value.getText());
                }
            };
            texts = new DefaultCellList<OffPageText>(textCell);
            texts.setTabIndex(-1);
            texts.addStyleName("hal-TabLayout-offPageList");
            texts.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.ENABLED);
            dataProvider.addDataDisplay(texts);

            selectionModel = new SingleSelectionModel<>();
            texts.setSelectionModel(selectionModel);
            selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
                public void onSelectionChange(SelectionChangeEvent event) {
                    OffPageText selected = selectionModel.getSelectedObject();
                    if (selected != null) {
                        tabLayout.selectTab(selected.getText());
                    }
                }
            });

            VerticalPanel panel = new VerticalPanel();
            panel.setStyleName("fill-layout-width");
            panel.add(texts);
            setWidget(panel);

            addCloseHandler(new CloseHandler<PopupPanel>() {
                @Override
                public void onClose(CloseEvent<PopupPanel> event) {
                    // If the last selection does not match visible tab users get confused
                    selectionModel.clear();
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

        void setValues(List<OffPageText> values) {
            dataProvider.setList(values);
        }
    }
}
