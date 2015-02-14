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

package org.jboss.as.console.client.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.bootstrap.GlobalShortcuts;
import org.jboss.as.console.client.v3.stores.DiagnosticsView;
import org.jboss.as.console.client.widgets.popups.DefaultPopup;
import org.jboss.as.console.client.widgets.progress.ProgressElement;
import org.jboss.ballroom.client.widgets.InlineLink;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.dispatch.Diagnostics;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * @author Heiko Braun
 */
public class Footer {

    public final static ProgressElement PROGRESS_ELEMENT = new ProgressElement();

    private final BootstrapContext context;
    private Widget diagWidget;
    private DiagnosticsView diag;
    private PlaceManager placeManager;
    private ProductConfig productConfig;
    private Diagnostics diagnostics = GWT.create(Diagnostics.class);

    @Inject
    public Footer(PlaceManager placeManager, ProductConfig prodConfig, BootstrapContext context, Dispatcher circuit) {
        this.placeManager = placeManager;
        this.productConfig = prodConfig;
        this.context = context;

        if(!GWT.isScript()) {
            diag = new DiagnosticsView();
            diagWidget = diag.asWidget();
            circuit.addDiagnostics(diag);
        }

    }

    public Widget asWidget() {
        final LayoutPanel layout = new LayoutPanel();
        final PopupPanel toolsPopup = new DefaultPopup(DefaultPopup.Arrow.BOTTOM);
        final List<String[]> toolReference = new ArrayList<String[]>();


        toolReference.add(new String[]{"Management Model", "browser"});
        GlobalShortcuts.bind("mod+1", new Command() {
            @Override
            public void execute() {
                placeManager.revealPlace(
                        new PlaceRequest.Builder().nameToken(NameTokens.ToolsPresenter).with("name", "browser")
                                .build(), false);
            }
        });

        toolReference.add(new String[]{"Expression Resolver", "expressions"});
        if (diagnostics.isEnabled()) {
            toolReference.add(new String[]{"Diagnostics", "debug-panel"});
        }

        // only enabled in dev mode
        if (!GWT.isScript()) {
            toolReference.add(new String[]{"Modelling", "mbui-workbench"});
            toolReference.add(new String[]{"Resource Access", "access-log"});
//            toolReference.add(new String[]{"Search Index", "indexing"});
        }

        final VerticalPanel toolsList = new VerticalPanel();
        toolsList.getElement().setAttribute("width", "160px");
        for(final String[] tool : toolReference)
        {
            InlineLink browser = new InlineLink(tool[0]);
            browser.getElement().setAttribute("style", "margin:4px");
            browser.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    toolsPopup.hide();
                    placeManager.revealPlace(
                            new PlaceRequest.Builder().nameToken(NameTokens.ToolsPresenter).with("name", tool[1])
                                    .build(),false);
                }
            });
            toolsList.add(browser);
        }
        toolsPopup.setWidget(toolsList);

        final HTML toolsLink = new HTML("<i class='icon-caret-up'></i>&nbsp;"+"Tools");
        toolsLink.addStyleName("footer-link");
        toolsLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                int listHeight = toolsList.getWidgetCount() * 25;
                toolsPopup.setPopupPosition(
                        toolsLink.getAbsoluteLeft()-45,
                        toolsLink.getAbsoluteTop()-(listHeight-25)-50
                );
                toolsPopup.setWidth("165");
                toolsPopup.setHeight(listHeight +"");
                toolsPopup.show();
            }
        });

        HTML settings = new HTML("<i class='icon-wrench'></i>&nbsp;"+Console.CONSTANTS.common_label_settings());
        settings.addStyleName("footer-link");
        settings.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                placeManager.revealPlace(
                        new PlaceRequest.Builder().nameToken(NameTokens.SettingsPresenter).build(), false
                );
            }
        });

        HorizontalPanel tools = new HorizontalPanel();
        tools.add(toolsLink);
        tools.add(settings);
        layout.add(tools);

        PROGRESS_ELEMENT.addStyleName("footer");
        layout.add(PROGRESS_ELEMENT);


        if(!GWT.isScript()) {
            layout.add(diagWidget);
        }

        String versionToShow = productConfig.getConsoleVersion();
        if (versionToShow == null) {
            // That's no HAL build - fall back core version
            versionToShow = productConfig.getCoreVersion();
        }
        HTML version = new HTML(versionToShow);
        version.setTitle("Version Information");
        version.addStyleName("footer-link");
        version.getElement().setAttribute("style", "font-size:10px; align:left");
        version.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                final DefaultWindow window = new DefaultWindow("Version Information");
                DialogueOptions options = new DialogueOptions(Console.CONSTANTS.common_label_done(), new ClickHandler() {
                    @Override
                    public void onClick(final ClickEvent event) {
                        window.hide();
                    }
                }, "", new ClickHandler() {
                    @Override
                    public void onClick(final ClickEvent event) {
                        // noop
                    }
                }).showCancel(false);
                window.setWidth(480);
                window.setHeight(360);
                window.trapWidget(new WindowContentBuilder(new ProductConfigPanel(context, productConfig).asWidget(), options).build());
                window.setGlassEnabled(true);
                window.center();
            }
        });

        layout.add(version);
        layout.setWidgetLeftWidth(version, 20, PX, 200, PX);
        layout.setWidgetTopHeight(version, 10, PX, 32, PX);

        layout.setWidgetRightWidth(PROGRESS_ELEMENT, 200, PX, 150, PX);
        layout.setWidgetTopHeight(PROGRESS_ELEMENT, 12, PX, 32, PX);

        if(!GWT.isScript())
        {
            layout.setWidgetRightWidth(diagWidget, 400, PX, 300, PX);
            layout.setWidgetTopHeight(diagWidget, 12, PX, 32, PX);

        }

        layout.setWidgetRightWidth(tools, 5, PX, 200, PX);
        layout.setWidgetTopHeight(tools, 10, PX, 32, PX);

        layout.setWidgetHorizontalPosition(tools, Layout.Alignment.END);
        layout.getElement().setAttribute("role", "complementary");

        return layout;
    }
}
