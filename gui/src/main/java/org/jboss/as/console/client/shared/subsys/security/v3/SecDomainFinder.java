package org.jboss.as.console.client.shared.subsys.security.v3;

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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;


/**
 * The Presenter for Cache Containers
 *
 * @author Heiko Braun
 */
public class SecDomainFinder extends Presenter<SecDomainFinder.MyView, SecDomainFinder.MyProxy> implements PreviewEvent.Handler {

    private RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;
    private final PlaceManager placeManager;

    public static AddressTemplate SECURITY_DOMAIN = AddressTemplate.of("{selected.profile}/subsystem=security/security-domain=*");
    private DefaultWindow domainDialog;
    private DomainPropertiesView domainView;
    private String selectedDomain;

    @ProxyCodeSplit
    @NameToken(NameTokens.SecDomains)
    @RequiredResources(resources = {
            "{selected.profile}/subsystem=security/",
            "{selected.profile}/subsystem=security/security-domain=*",
    }, recursive = false)
    @SearchIndex(
            keywords = {
                    "security-domain", "authentication", "security", "vault", "authorisation", "jaas", "login-module", "vault"}
    )
    public interface MyProxy extends Proxy<SecDomainFinder>, Place {
    }

    public interface MyView extends View {
        void setPresenter(SecDomainFinder presenter);
        public void updateFrom(List<Property> list);
        public void setPreview(final SafeHtml html);
    }

    @Inject
    public SecDomainFinder(
            EventBus eventBus, MyView view, MyProxy proxy,
            RevealStrategy revealStrategy, DispatchAsync dispatcher,
            ResourceDescriptionRegistry descriptionRegistry, SecurityFramework securityFramework,
            CoreGUIContext delegate, PlaceManager placeManager) {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;

        this.dispatcher = dispatcher;
        this.descriptionRegistry = descriptionRegistry;
        this.securityFramework = securityFramework;
        this.statementContext = delegate;
        this.placeManager = placeManager;
    }

    @Override
    public void onPreview(PreviewEvent event) {
        if(isVisible())
            getView().setPreview(event.getHtml());
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(PreviewEvent.TYPE,this);
    }

    @Override
    protected void onReset() {
        super.onReset();

        if(placeManager.getCurrentPlaceRequest().matchesNameToken(getProxy().getNameToken()))
            loadDomains(null);

    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    private void loadDomains() {
        loadDomains(null);
    }

    private void loadDomains(String pref) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "security");
        operation.get(CHILD_TYPE).set("security-domain");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to load security domains", response.getFailureDescription());
                    getView().updateFrom(Collections.EMPTY_LIST);
                } else {
                    List<Property> domains = response.get(RESULT).asPropertyList();
                    getView().updateFrom(domains);

                    if(domainDialog!=null && domainDialog.isVisible())
                    {
                        for (Property domain : domains) {
                            if(domain.getName().equals(selectedDomain))
                            {
                                domainView.updateFrom(domain.getValue());
                                break;
                            }
                        }
                    }

                }

            }
        });
    }

    @Override
    protected void revealInParent() {
        if(Console.getBootstrapContext().isStandalone())
            RevealContentEvent.fire(this, ServerMgmtApplicationPresenter.TYPE_MainContent, this);
        else
            RevealContentEvent.fire(this, ProfileMgmtPresenter.TYPE_MainContent, this);
    }

    public void onRemove(Property cacheContainer) {
        ResourceAddress fqAddress = SECURITY_DOMAIN.resolve(statementContext, cacheContainer.getName());

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                loadDomains();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {

                ModelNode response = dmrResponse.get();
                if(response.isFailure())
                {
                    Console.error("Failed to remove resource "+fqAddress, response.getFailureDescription());
                }
                else
                {
                    Console.info("Successfully removed " + fqAddress);
                }

                loadDomains();
            }
        });

    }

    public void onSaveDomain(AddressTemplate address, String name, Map<String, Object> changeset) {
        ResourceAddress fqAddress = address.resolve(statementContext, name);

        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode operation = adapter.fromChangeset(changeset, fqAddress);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to modify resource " + fqAddress, caught.getMessage());
                loadDomains();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error("Failed to modify resource " + fqAddress, response.getFailureDescription());
                } else {
                    Console.info("Successfully modified " + fqAddress);
                }
                loadDomains();
            }
        });
    }


    public void onLauchAddDomain() {
        final SecurityContext securityContext =
                securityFramework.getSecurityContext(getProxy().getNameToken());

        final ResourceDescription resourceDescription = descriptionRegistry.lookup(SECURITY_DOMAIN);

        final DefaultWindow dialog = new DefaultWindow("New Security Domain");
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        dialog.hide();

                        final ResourceAddress fqAddress =
                                SECURITY_DOMAIN.resolve(statementContext, payload.get("name").asString());

                        payload.get(OP).set(ADD);
                        payload.get(ADDRESS).set(fqAddress);

                        dispatcher.execute(new DMRAction(payload), new SimpleCallback<DMRResponse>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                super.onFailure(caught);
                                loadDomains();
                            }

                            @Override
                            public void onSuccess(DMRResponse dmrResponse) {
                                Console.info("Successfully added "+fqAddress);
                                loadDomains();
                            }
                        });


                    }

                    @Override
                    public void onCancel() {
                        dialog.hide();
                    }
                });

        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    public void onLaunchDomainSettings(Property domain) {

        this.selectedDomain= domain.getName();

        domainDialog = new DefaultWindow("Security Domain Settings");

        domainView = new DomainPropertiesView(this, domain);

        domainDialog.setWidth(640);
        domainDialog.setHeight(480);
        domainDialog.trapWidget(domainView.asWidget());
        domainDialog.setGlassEnabled(true);
        domainDialog.center();

        domainView.updateFrom(domain.getValue());
    }

}

