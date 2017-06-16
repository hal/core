/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.shared.subsys.elytron;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
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
import org.jboss.as.console.client.widgets.nav.v3.PreviewFactory;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ElytronFinderView extends SuspendableViewImpl implements ElytronFinder.MyView {

    public static final String FACTORY = "Factory / Transformer";
    public static final String MAPPER_DECODER = "Mapper / Decoder";
    public static final String SECURITY_REALM = "Security Realm / Authentication";
    public static final String OTHER = "Other";

    private ElytronFinder presenter;
    private LayoutPanel previewCanvas;
    private SplitLayoutPanel layout;
    private final PlaceManager placeManager;
    private final PreviewContentFactory previewContentFactory;
    private FinderColumn<FinderItem> links;

    private ColumnManager columnManager;
    private Widget linksCol;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final ElytronFinderView.Template TEMPLATE = GWT.create(ElytronFinderView.Template.class);

    @Inject
    public ElytronFinderView(PlaceManager placeManager, PreviewContentFactory previewContentFactory) {
        this.placeManager = placeManager;
        this.previewContentFactory = previewContentFactory;
    }

    @Override
    public void setPresenter(ElytronFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setPreview(SafeHtml html) {
        Scheduler.get().scheduleDeferred(() -> {
            previewCanvas.clear();
            previewCanvas.add(new ScrollPanel(new HTML(html)));
        });
    }

    @Override
    public Widget createWidget() {
        previewCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, FinderColumn.FinderId.CONFIGURATION);

        links = new FinderColumn<FinderItem>(
                FinderColumn.FinderId.CONFIGURATION,
                "Settings",
                new FinderColumn.Display<FinderItem>() {

                    @Override
                    public boolean isFolder(FinderItem data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(String baseCss, FinderItem data) {
                        return TEMPLATE.item(baseCss, data.getTitle());
                    }

                    @Override
                    public String rowCss(FinderItem data) {
                        return "";
                    }
                },
                new ProvidesKey<FinderItem>() {
                    @Override
                    public Object getKey(FinderItem item) {
                        return item.getTitle();
                    }
                }, presenter.getProxy().getNameToken())
        ;

        links.setPreviewFactory(new PreviewFactory<FinderItem>() {
            @Override
            public void createPreview(final FinderItem data, final AsyncCallback<SafeHtml> callback) {
                if (FACTORY.equals(data.getTitle())) {
                    previewContentFactory.createContent(PreviewContent.INSTANCE.elytron_factory(), callback);

                } else if (MAPPER_DECODER.equals(data.getTitle())) {
                    previewContentFactory.createContent(PreviewContent.INSTANCE.elytron_mapper(), callback);

                } else if (OTHER.equals(data.getTitle())) {
                    previewContentFactory.createContent(PreviewContent.INSTANCE.elytron_settings(), callback);

                } else if (SECURITY_REALM.equals(data.getTitle())) {
                    previewContentFactory.createContent(PreviewContent.INSTANCE.elytron_security_realm(), callback);
                }
            }
        });

        links.setMenuItems(new MenuDelegate<>(Console.CONSTANTS.common_label_view(), item -> item.getCmd().execute()));

        links.addSelectionChangeHandler(event -> {
            if(links.hasSelectedItem())
            {
                FinderItem item = links.getSelectedItem();
                columnManager.updateActiveSelection(linksCol);
            }
        });

        linksCol = links.asWidget();

        columnManager.addWest(linksCol);
        columnManager.add(previewCanvas);

        columnManager.setInitialVisible(1);


        List<FinderItem> settings = new ArrayList<>();
        settings.add(new FinderItem(FACTORY, ()
                -> placeManager.revealRelativePlace(new PlaceRequest(NameTokens.ElytronFactoryPresenter)), false));

        settings.add(new FinderItem(MAPPER_DECODER, ()
                -> placeManager.revealRelativePlace(new PlaceRequest(NameTokens.ElytronMapperPresenter)), false));

        settings.add(new FinderItem(OTHER, ()
                -> placeManager.revealRelativePlace(new PlaceRequest(NameTokens.ElytronPresenter)), false));

        settings.add(new FinderItem(SECURITY_REALM, ()
                -> placeManager.revealRelativePlace(new PlaceRequest(NameTokens.ElytronSecurityRealmPresenter)), false));

        links.updateFrom(settings);
        return layout;
    }
}
