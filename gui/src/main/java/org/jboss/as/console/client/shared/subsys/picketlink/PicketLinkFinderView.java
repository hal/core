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
package org.jboss.as.console.client.shared.subsys.picketlink;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
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
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

import static org.jboss.as.console.client.shared.subsys.picketlink.PicketLinkDirectory.FEDERATION_REQUEST_PARAM;
import static org.jboss.as.console.client.shared.subsys.picketlink.PicketLinkDirectory.SERVICE_PROVIDER_REQUEST_PARAM;
import static org.jboss.as.console.client.widgets.nav.v3.FinderColumn.FinderId.CONFIGURATION;
import static org.jboss.as.console.client.widgets.nav.v3.MenuDelegate.Role.Navigation;
import static org.jboss.as.console.client.widgets.nav.v3.MenuDelegate.Role.Operation;

/**
 * @author Harald Pehl
 */
public class PicketLinkFinderView extends SuspendableViewImpl implements PicketLinkFinder.MyView {

    interface Template extends SafeHtmlTemplates {

        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);

        @Template("<div class='preview-content'><h1>{0}</h1><ul>" +
                "<li>Identity Provider: {1}</li>" +
                "<li>Security Domain: {2}</li>" +
                "<li>URL: {3}</li>" +
                "</ul></div>")
        SafeHtml federationPreview(String name, String identityProvider, String securityDomain, String url);

        @Template("<div class='preview-content'><h1>{0}</h1><ul>" +
                "<li>Security Domain: {1}</li>" +
                "<li>URL: {2}</li>" +
                "</ul></div>")
        SafeHtml serviceProviderPreview(String name, String securityDomain, String url);
    }


    private static final Template TEMPLATE = GWT.create(Template.class);

    private final PlaceManager placeManager;
    private final PreviewContentFactory contentFactory;
    private PicketLinkFinder presenter;
    private LayoutPanel previewCanvas;
    private ColumnManager columnManager;
    private FinderColumn<Property> federationsColumn;
    private Widget federationsColumnWidget;
    private FinderColumn<Property> serviceProviderColumn;
    private Widget serviceProviderColumnWidget;

    @Inject
    public PicketLinkFinderView(final PlaceManager placeManager, final PreviewContentFactory contentFactory) {
        this.placeManager = placeManager;
        this.contentFactory = contentFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget createWidget() {
        previewCanvas = new LayoutPanel();
        SplitLayoutPanel layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout, CONFIGURATION);


        // ------------------------------------------------------ service provider

        serviceProviderColumn = new FinderColumn<>(CONFIGURATION, "Service Provider",
                new FinderColumn.Display<Property>() {
                    @Override
                    public boolean isFolder(final Property data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Property data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(final Property data) {
                        return "";
                    }
                },
                new ProvidesKey<Property>() {
                    @Override
                    public Object getKey(final Property data) {
                        return data.getName();
                    }
                },
                presenter.getProxy().getNameToken());

        serviceProviderColumn.setTopMenuItems(
                new MenuDelegate<>(Console.CONSTANTS.common_label_add(),
                        (ContextualCommand<Property>) item ->
                                presenter.launchAddServiceProviderDialog(federationsColumn.getSelectedItem().getName()),
                        Operation));
        serviceProviderColumn.setMenuItems(
                new MenuDelegate<>(Console.CONSTANTS.common_label_view(),
                        (ContextualCommand<Property>) item -> {
                            PlaceRequest placeRequest = new PlaceRequest.Builder()
                                    .nameToken(NameTokens.PicketLinkServiceProvider)
                                    .with(FEDERATION_REQUEST_PARAM, federationsColumn.getSelectedItem().getName())
                                    .with(SERVICE_PROVIDER_REQUEST_PARAM, item.getName())
                                    .build();
                            placeManager.revealRelativePlace(placeRequest);
                        }, Navigation),
                new MenuDelegate<>(Console.CONSTANTS.common_label_delete(),
                        (ContextualCommand<Property>) item -> {
                            if (serviceProviderColumn.hasSelectedItem()) {
                                Property selectedItem = serviceProviderColumn.getSelectedItem();
                                Feedback.confirm(Console.MESSAGES.deleteTitle("Service Provider"),
                                        Console.MESSAGES.deleteConfirm(selectedItem.getName()),
                                        isConfirmed -> {
                                            if (isConfirmed) {
                                                presenter.removeServiceProvider(
                                                        federationsColumn.getSelectedItem().getName(),
                                                        selectedItem);
                                            }
                                        });
                            }
                        }, Operation));

        serviceProviderColumn.addSelectionChangeHandler(selectionChangeEvent -> {
            if (serviceProviderColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(serviceProviderColumnWidget);
            }
        });

        serviceProviderColumn.setPreviewFactory((data, callback) -> {
            String name = data.getName();
            ModelNode serviceProvider = data.getValue();
            String securityDomain = serviceProvider.get("security-domain").asString();
            String url = serviceProvider.get("url").asString();
            callback.onSuccess(TEMPLATE.serviceProviderPreview(name, securityDomain, url));
        });

        serviceProviderColumnWidget = serviceProviderColumn.asWidget();


        // ------------------------------------------------------ federation

        federationsColumn = new FinderColumn<>(CONFIGURATION, "Federation",
                new FinderColumn.Display<Property>() {
                    @Override
                    public boolean isFolder(final Property data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Property data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(final Property data) {
                        return "";
                    }
                },
                new ProvidesKey<Property>() {
                    @Override
                    public Object getKey(final Property data) {
                        return data.getName();
                    }
                },
                presenter.getProxy().getNameToken());

        federationsColumn.setTopMenuItems(
                new MenuDelegate<>(Console.CONSTANTS.common_label_add(), (ContextualCommand<Property>) item -> presenter.launchAddFederationDialog(),
                        Operation));
        federationsColumn.setMenuItems(
                new MenuDelegate<>(Console.CONSTANTS.common_label_view(),
                        (ContextualCommand<Property>) item -> {
                            PlaceRequest placeRequest = new PlaceRequest.Builder()
                                    .nameToken(NameTokens.PicketLinkFederation)
                                    .with(FEDERATION_REQUEST_PARAM, item.getName()).build();
                            placeManager.revealRelativePlace(placeRequest);
                        }, Navigation),
                new MenuDelegate<>(Console.CONSTANTS.common_label_delete(),
                        (ContextualCommand<Property>) item -> {
                            if (federationsColumn.hasSelectedItem()) {
                                Property selectedItem = federationsColumn.getSelectedItem();
                                Feedback.confirm(Console.MESSAGES.deleteTitle("Federation"),
                                        Console.MESSAGES.deleteConfirm(selectedItem.getName()),
                                        isConfirmed -> {
                                            if (isConfirmed) {
                                                presenter.removeFederation(selectedItem);
                                            }
                                        });
                            }
                        }, Operation));

        federationsColumn.addSelectionChangeHandler(selectionChangeEvent -> {
            columnManager.reduceColumnsTo(1);
            if (federationsColumn.hasSelectedItem()) {
                columnManager.updateActiveSelection(federationsColumnWidget);
                columnManager.appendColumn(serviceProviderColumnWidget);
                presenter.readServiceProvider(federationsColumn.getSelectedItem().getName());
            } else {
                startupContent(contentFactory);
            }
        });

        federationsColumn.setPreviewFactory((data, callback) -> {
            String name = data.getName();
            String identityProvider = "n/a";
            String securityDomain = "n/a";
            String url = "n/a";
            if (data.getValue().get("identity-provider").isDefined()) {
                Property property = data.getValue().get("identity-provider").asProperty();
                identityProvider = property.getName();
                securityDomain = property.getValue().get("security-domain").asString();
                url = property.getValue().get("url").asString();
            }
            callback.onSuccess(TEMPLATE.federationPreview(name, identityProvider, securityDomain, url));
        });

        federationsColumnWidget = federationsColumn.asWidget();
        columnManager.addWest(federationsColumnWidget);
        columnManager.addWest(serviceProviderColumnWidget);
        columnManager.add(previewCanvas);
        columnManager.setInitialVisible(1);
        return layout;
    }

    @Override
    public void setPresenter(final PicketLinkFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateFederations(final List<Property> federations) {
        federationsColumn.updateFrom(federations);
    }

    @Override
    public void updateServiceProvider(final List<Property> serviceProvider) {
        serviceProviderColumn.updateFrom(serviceProvider);
    }

    @Override
    public void setPreview(final SafeHtml html) {
        Scheduler.get().scheduleDeferred(() -> {
            previewCanvas.clear();
            previewCanvas.add(new ScrollPanel(new HTML(html)));
        });
    }

    @Override
    public void toggleScrolling(final boolean enforceScrolling, final int requiredWidth) {
        columnManager.toogleScrolling(enforceScrolling, requiredWidth);
    }

    public void clearActiveSelection(final ClearFinderSelectionEvent event) {
        federationsColumnWidget.getElement().removeClassName("active");
        serviceProviderColumnWidget.getElement().removeClassName("active");
    }

    private void startupContent(PreviewContentFactory contentFactory) {
        contentFactory.createContent(PreviewContent.INSTANCE.picketlink_federations(),
                new SimpleCallback<SafeHtml>() {
                    @Override
                    public void onSuccess(SafeHtml previewContent) {
                        setPreview(previewContent);
                    }
                }
        );
    }
}
