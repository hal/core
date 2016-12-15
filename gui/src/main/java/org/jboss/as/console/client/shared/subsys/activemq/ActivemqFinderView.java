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
package org.jboss.as.console.client.shared.subsys.activemq;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.widgets.nav.v3.MenuDelegate.Role.Operation;

public class ActivemqFinderView extends SuspendableViewImpl implements ActivemqFinder.MyView {

    static final String MESSAGING_PROVIDER = "Messaging Provider";
    static final String JMS_BRIDGE = "JMS Bridge";

    private ActivemqFinder presenter;
    private LayoutPanel previewCanvas;
    private final PlaceManager placeManager;
    private final PreviewContentFactory previewContentFactory;
    private FinderColumn<FinderItem> settingsColumn;

    private Widget messagingProviderColumnWidget;
    private FinderColumn<Property> messagingProviderColumn;

    private ColumnManager columnManager;
    private Widget linksCol;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final ActivemqFinderView.Template TEMPLATE = GWT.create(ActivemqFinderView.Template.class);

    @Inject
    public ActivemqFinderView(PlaceManager placeManager, PreviewContentFactory previewContentFactory) {
        this.placeManager = placeManager;
        this.previewContentFactory = previewContentFactory;
    }

    @Override
    public void setPresenter(ActivemqFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setPreview(SafeHtml html) {
        Scheduler.get().scheduleDeferred(() -> {
            previewCanvas.clear();
            previewCanvas.add(new HTML(html));
        });
    }

    @Override
    public void updateFrom(List<Property> list) {
        messagingProviderColumn.updateFrom(list);
    }

    @Override
    public Widget createWidget() {
        previewCanvas = new LayoutPanel();

        SplitLayoutPanel layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.CONFIGURATION);

        //  the settings column, it contains the "messaging provider" and "jms-bridge" link
        settingsColumn = new FinderColumn<>(
                FinderColumn.FinderId.CONFIGURATION,
                "Settings",
                new FinderColumn.Display<FinderItem>() {

                    @Override
                    public boolean isFolder(FinderItem data) {
                        return data.isFolder();
                    }

                    @Override
                    public SafeHtml render(String baseCss, FinderItem data) {
                        return TEMPLATE.item(baseCss, data.getTitle());
                    }

                    @Override
                    public String rowCss(FinderItem data) {
                        // if folder, do not show the "view" link button
                        return data.isFolder() ? "no-menu" : "";
                    }
                },
                new ProvidesKey<FinderItem>() {
                    @Override
                    public Object getKey(FinderItem item) {
                        return item.getTitle();
                    }
                }, presenter.getProxy().getNameToken())
        ;

        settingsColumn.setPreviewFactory((data, callback) -> {
            if (MESSAGING_PROVIDER.equals(data.getTitle())) {
                previewContentFactory.createContent(PreviewContent.INSTANCE.messaging_provider(), callback);
            } else if (JMS_BRIDGE.equals(data.getTitle())) {
                previewContentFactory.createContent(PreviewContent.INSTANCE.jms_bridge(), callback);
            }
        });

        settingsColumn.setMenuItems(new MenuDelegate<>(Console.CONSTANTS.common_label_view(), item -> item.getCmd().execute()));

        List<FinderItem> settings = new ArrayList<>();
        FinderItem messagingPoviderFinderItem = new FinderItem(MESSAGING_PROVIDER, () -> {
            columnManager.reduceColumnsTo(1);
            columnManager.appendColumn(messagingProviderColumnWidget);
            presenter.loadProvider();
        }, true);
        settings.add(messagingPoviderFinderItem);
        settings.add(new FinderItem(JMS_BRIDGE, () -> placeManager.revealRelativePlace(new PlaceRequest(NameTokens.JMSBridge)), false));

        settingsColumn.addSelectionChangeHandler(event -> {
            if (settingsColumn.hasSelectedItem()) {
                columnManager.reduceColumnsTo(1);
                if (settingsColumn.hasSelectedItem() && settingsColumn.getSelectedItem() == messagingPoviderFinderItem) {
                    // load and displays the provider list.
                    FinderItem item = settingsColumn.getSelectedItem();
                    columnManager.updateActiveSelection(linksCol);
                    item.getCmd().execute();
                }
            }
        });

        // the messaging-provider column, it display the list of messaging providers.
        messagingProviderColumn = new FinderColumn<>(
                FinderColumn.FinderId.CONFIGURATION,
                Console.MESSAGES.messagingProvider(),
                new FinderColumn.Display<Property>() {

                    @Override
                    public boolean isFolder(Property data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(String baseCss, Property data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(Property data) {
                        return "";
                    }
                },
                new ProvidesKey<Property>() {
                    @Override
                    public Object getKey(Property item) {
                        return item.getName();
                    }
                },
                presenter.getProxy().getNameToken());
        messagingProviderColumnWidget = messagingProviderColumn.asWidget();

        messagingProviderColumn.setTopMenuItems(
                new MenuDelegate<>(Console.CONSTANTS.common_label_add(),
                        mailSession -> presenter.launchNewProviderWizard(), Operation));

        messagingProviderColumn.setMenuItems(
                new MenuDelegate<>("Queues/Topics", provider ->
                        placeManager.revealRelativePlace(
                                new PlaceRequest.Builder().nameToken(NameTokens.ActivemqMessagingPresenter)
                                        .with("name", provider.getName()).build())),

                new MenuDelegate<>("Connections", provider ->
                        placeManager.revealRelativePlace(
                                new PlaceRequest.Builder().nameToken(NameTokens.ActivemqMsgConnectionsPresenter)
                                        .with("name", provider.getName()).build())),

                new MenuDelegate<>("Clustering", provider ->
                        placeManager.revealRelativePlace(
                                new PlaceRequest.Builder().nameToken(NameTokens.ActivemqMsgClusteringPresenter)
                                        .with("name", provider.getName()).build())),

                new MenuDelegate<>(Console.MESSAGES.providerSettings(), presenter::onLaunchProviderSettings),

                new MenuDelegate<>(Console.CONSTANTS.common_label_delete(), provider ->
                        Feedback.confirm(Console.MESSAGES.deleteTitle("Messaging Provider"),
                                Console.MESSAGES.deleteConfirm("provider " + provider.getName()),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        presenter.onDeleteProvider(provider);
                                    }
                                }), Operation)
        );

        messagingProviderColumn.setPreviewFactory(
                (data, callback) -> previewContentFactory
                        .createContent(PreviewContent.INSTANCE.messaging_provider(), callback));

        messagingProviderColumn.addSelectionChangeHandler(event -> {
            if (messagingProviderColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(messagingProviderColumnWidget);
            }
        });

        linksCol = settingsColumn.asWidget();
        columnManager.addWest(linksCol);
        columnManager.addWest(messagingProviderColumnWidget);
        columnManager.add(previewCanvas);
        columnManager.setInitialVisible(1);

        settingsColumn.updateFrom(settings);
        return layout;
    }
}
