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

import com.allen_sauer.gwt.log.client.Log;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.dmr.StaticResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class PicketLinkFinder extends Presenter<PicketLinkFinder.MyView, PicketLinkFinder.MyProxy>
        implements PicketLinkDirectory, PreviewEvent.Handler, FinderScrollEvent.Handler,
        ClearFinderSelectionEvent.Handler {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.PicketLinkFinder)
    @RequiredResources(resources = {FEDERATION_ADDRESS,
            IDENTITY_PROVIDER_ADDRESS,
            SERVICE_PROVIDER_ADDRESS
    }, recursive = false)
    public interface MyProxy extends Proxy<PicketLinkFinder>, Place {
    }

    public interface MyView extends View, HasPresenter<PicketLinkFinder> {
        void updateFederations(List<Property> list);
        void updateServiceProvider(List<Property> list);

        void setPreview(final SafeHtml html);
        void clearActiveSelection(ClearFinderSelectionEvent event);
        void toggleScrolling(boolean enforceScrolling, int requiredWidth);
    }
    // @formatter:on


    static final PicketLinkResources PICKET_LINK_RESOURCES = GWT.create(PicketLinkResources.class);

    private final DispatchAsync dispatcher;
    private final BootstrapContext bootstrapContext;
    private final SecurityContext securityContext;
    private final StatementContext statementContext;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final CrudOperationDelegate crud;
    private final List<String> securityDomains;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public PicketLinkFinder(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final DispatchAsync dispatcher, final BootstrapContext bootstrapContext,
            final SecurityFramework securityFramework, final StatementContext statementContext,
            final ResourceDescriptionRegistry descriptionRegistry) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.bootstrapContext = bootstrapContext;
        this.securityContext = securityFramework.getSecurityContext(NameTokens.PicketLinkFinder);
        this.statementContext = statementContext;
        this.descriptionRegistry = descriptionRegistry;
        this.crud = new CrudOperationDelegate(statementContext, dispatcher);
        this.securityDomains = new ArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(PreviewEvent.TYPE, this);
    }

    @Override
    protected void revealInParent() {
        if (bootstrapContext.isStandalone()) {
            RevealContentEvent.fire(this, ServerMgmtApplicationPresenter.TYPE_MainContent, this);
        } else {
            RevealContentEvent.fire(this, ProfileMgmtPresenter.TYPE_MainContent, this);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        readFederations();
    }


    // ------------------------------------------------------ finder related methods
    @Override
    public void onPreview(final PreviewEvent event) {
        if (isVisible()) {
            getView().setPreview(event.getHtml());
        }
    }

    @Override
    public void onToggleScrolling(final FinderScrollEvent event) {
        if (isVisible()) { getView().toggleScrolling(event.isEnforceScrolling(), event.getRequiredWidth()); }
    }

    @Override
    public void onClearActiveSelection(final ClearFinderSelectionEvent event) {
        if (isVisible()) { getView().clearActiveSelection(event); }
    }


    // ------------------------------------------------------ crud federation

    void launchAddFederationDialog() {
        ResourceDescription resourceDescription = StaticResourceDescription
                .from(PICKET_LINK_RESOURCES.newFederationDescription());
        ComboBoxItem securityDomains = new ComboBoxItem("security-domain", "Security Domain");
        securityDomains.setRequired(true);
        securityDomains.setValueMap(this.securityDomains);

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Federation"));
        ModelNodeFormBuilder.FormAssets assets = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .addFactory("security-domain", attributeDescription -> securityDomains)
                .setSecurityContext(securityContext)
                .unsorted()
                .build();
        assets.getForm().setEnabled(true);

        DialogueOptions options = new DialogueOptions(
                event -> {
                    FormValidation validation = assets.getForm().validate();
                    if (!validation.hasErrors()) {
                        dialog.hide();

                        ModelNode payload = assets.getForm().getUpdatedEntity();
                        String name = payload.get(NAME).asString();
                        String ip = payload.get("identity-provider").asString();

                        Operation addFederation = new Operation.Builder(ADD,
                                FEDERATION_TEMPLATE.resolve(statementContext, name)).build();
                        Operation addIdentityProvider = new Operation.Builder(ADD,
                                IDENTITY_PROVIDER_TEMPLATE.resolve(statementContext, name, ip))
                                .param("security-domain", payload.get("security-domain").asString())
                                .param("url", payload.get("url").asString())
                                .build();
                        dispatcher.execute(new DMRAction(new Composite(addFederation, addIdentityProvider)),
                                new SimpleCallback<DMRResponse>() {
                                    @Override
                                    public void onFailure(final Throwable caught) {
                                        super.onFailure(caught);
                                        readFederations();
                                    }

                                    @Override
                                    public void onSuccess(final DMRResponse dmrResponse) {
                                        Console.info(Console.MESSAGES.successfullyAdded((name)));
                                        readFederations();
                                    }
                                });
                    }
                },
                event -> dialog.hide()
        );

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width window-content");
        ContentDescription text = new ContentDescription(resourceDescription.get(DESCRIPTION).asString());
        layout.add(text);
        layout.add(assets.getHelp().asWidget());
        layout.add(assets.getForm().asWidget());

        ScrollPanel scroll = new ScrollPanel(layout);
        LayoutPanel content = new LayoutPanel();
        content.addStyleName("fill-layout");
        content.add(scroll);
        content.add(options);
        content.getElement().setAttribute("style", "margin-bottom:10px");
        content.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 92, Style.Unit.PCT);
        content.setWidgetBottomHeight(options, 0, Style.Unit.PX, 35, Style.Unit.PX);

        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(new TrappedFocusPanel(content));
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    void readFederations() {
        ResourceAddress address = ROOT_TEMPLATE.resolve(statementContext);
        Operation fedOp = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                .param(CHILD_TYPE, "federation")
                .param("recursive-depth", 1)
                .build();
        Operation sdOp = new Operation.Builder(READ_CHILDREN_NAMES_OPERATION,
                AddressTemplate.of("{selected.profile}/subsystem=security").resolve(statementContext))
                .param(CHILD_TYPE, "security-domain")
                .build();
        dispatcher.execute(new DMRAction(new Composite(fedOp, sdOp)), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(final DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if (response.isFailure()) {
                    Log.error("Failed to load federations", response.getFailureDescription());
                    getView().updateFederations(Collections.<Property>emptyList());
                } else {
                    List<Property> federations = response.get(RESULT).get("step-1").get(RESULT).asPropertyList();
                    List<ModelNode> sdNodes = response.get(RESULT).get("step-2").get(RESULT).asList();
                    securityDomains.clear();
                    securityDomains.addAll(Ordering.natural().immutableSortedCopy(
                            FluentIterable.from(sdNodes).transform(ModelNode::asString)));
                    getView().updateFederations(federations);
                }
            }
        });
    }

    void removeFederation(Property federation) {
        crud.onRemoveResource(FEDERATION_TEMPLATE, federation.getName(), new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                Console.info(Console.MESSAGES.successfullyRemoved((name)));
                readFederations();
            }

            @Override
            public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                Console.error(Console.MESSAGES.failedToRemoveResource(name), t.getMessage());
                readFederations();
            }
        });
    }


    // ------------------------------------------------------ crud service provider

    void launchAddServiceProviderDialog(final String federation) {
        ComboBoxItem securityDomains = new ComboBoxItem("security-domain", "Security Domain");
        securityDomains.setRequired(true);
        securityDomains.setValueMap(this.securityDomains);

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Service Provider"));
        ResourceDescription resourceDescription = descriptionRegistry.lookup(SERVICE_PROVIDER_TEMPLATE);
        //noinspection Duplicates
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        dialog.hide();

                        String name = payload.get("name").asString();
                        ResourceAddress address = SERVICE_PROVIDER_TEMPLATE.resolve(statementContext, federation, name);
                        payload.get(OP).set(ADD);
                        payload.get(ADDRESS).set(address);
                        dispatcher.execute(new DMRAction(payload), new SimpleCallback<DMRResponse>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                super.onFailure(caught);
                                readServiceProvider(federation);
                            }

                            @Override
                            public void onSuccess(DMRResponse dmrResponse) {
                                Console.info(Console.MESSAGES.successfullyAdded((name)));
                                readServiceProvider(federation);
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        dialog.hide();
                    }
                })
                .addFactory("security-domain", attributeDescription -> securityDomains)
                .include();

        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    void readServiceProvider(final String federation) {
        ResourceAddress address = FEDERATION_TEMPLATE.resolve(statementContext, federation);
        Operation operation = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                .param(CHILD_TYPE, "service-provider")
                .build();
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(final DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if (response.isFailure()) {
                    Log.error("Failed to load service provider", response.getFailureDescription());
                    getView().updateServiceProvider(Collections.<Property>emptyList());
                } else {
                    List<Property> serviceProvider = response.get(RESULT).asPropertyList();
                    getView().updateServiceProvider(serviceProvider);
                }
            }
        });
    }

    void removeServiceProvider(final String federation, final Property serviceProvider) {
        crud.onRemoveResource(SERVICE_PROVIDER_TEMPLATE.replaceWildcards(federation), serviceProvider.getName(),
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                        Console.info(Console.MESSAGES.successfullyRemoved((name)));
                        readFederations();
                    }

                    @Override
                    public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                        Console.error(Console.MESSAGES.failedToRemoveResource(name), t.getMessage());
                        readFederations();
                    }
                });
    }
}
