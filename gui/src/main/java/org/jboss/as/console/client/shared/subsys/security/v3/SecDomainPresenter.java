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
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.SuggestBoxItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * The Presenter for Caches
 *
 * @author Heiko Braun
 */
public class SecDomainPresenter extends Presenter<SecDomainPresenter.MyView, SecDomainPresenter.MyProxy> {

    private RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;

    public enum SubResource {
        AUTHENTICATION("authentication", "login-module"),
        AUTHORIZATION("authorization", "policy-module"),
        AUDIT("audit", "provider-module"),
        MAPPING("mapping", "mapping-module"),
        ACL("acl", "acl-module"),
        TRUST("identity-trust", "trust-module");

        private String type;
        private String moduleDef;

        SubResource(String type, String moduleDef) {
            this.type = type;
            this.moduleDef = moduleDef;
        }

        public String getType() {
            return type;
        }

        public String getModuleDef() {
            return moduleDef;
        }
    }

    private Set<SubResource> missingContainer = new HashSet<>();


    final static AddressTemplate SEC_DOMAIN = AddressTemplate.of("{selected.profile}/subsystem=security/security-domain=*");

    @ProxyCodeSplit
    @NameToken(NameTokens.SecDomain)
    @RequiredResources(resources = {
            "{selected.profile}/subsystem=security/",
            "{selected.profile}/subsystem=security/security-domain=*",

            "{selected.profile}/subsystem=security/security-domain=*/authentication=classic",
            "{selected.profile}/subsystem=security/security-domain=*/authentication=classic/login-module=*",


            "{selected.profile}/subsystem=security/security-domain=*/authorization=classic",
            "{selected.profile}/subsystem=security/security-domain=*/authorization=classic/policy-module=*",

            "{selected.profile}/subsystem=security/security-domain=*/audit=classic",
            "{selected.profile}/subsystem=security/security-domain=*/audit=classic/provider-module=*",

            "{selected.profile}/subsystem=security/security-domain=*/mapping=classic",
            "{selected.profile}/subsystem=security/security-domain=*/mapping=classic/mapping-module=*",

            "{selected.profile}/subsystem=security/security-domain=*/acl=classic",
            "{selected.profile}/subsystem=security/security-domain=*/acl=classic/acl-module=*",

            "{selected.profile}/subsystem=security/security-domain=*/identity-trust=classic",
            "{selected.profile}/subsystem=security/security-domain=*/identity-trust=classic/trust-module=*",

            //"{selected.profile}/subsystem=security/vault=classic",
    }, recursive = false)
    @SearchIndex(
            keywords = {
                    "security-domain", "authentication", "security", "vault", "authorisation", "jaas", "login-module", "vault"}
    )
    public interface MyProxy extends Proxy<SecDomainPresenter>, Place {
    }

    private String domain;

    public interface MyView extends View {
        void setPresenter(SecDomainPresenter presenter);

        void setPreview(final SafeHtml html);

        void updateSubResource(SubResource resource, List<Property> modules);

        void reset();

    }

    @Inject
    public SecDomainPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            RevealStrategy revealStrategy, DispatchAsync dispatcher,
            ResourceDescriptionRegistry descriptionRegistry, SecurityFramework securityFramework, CoreGUIContext delegate) {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;

        this.dispatcher = dispatcher;
        this.descriptionRegistry = descriptionRegistry;
        this.securityFramework = securityFramework;
        this.statementContext = delegate;

    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        super.prepareFromRequest(request);
        domain = request.getParameter("domain", null);
    }

    @Override
    protected void onReset() {
        super.onReset();

        loadDomain();

    }

    private void loadDomain() {

        if (null == domain)
            throw new RuntimeException("No security domain specified");

        missingContainer.clear();
        getView().reset();

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "security");
        operation.get(ADDRESS).add("security-domain", domain);
        operation.get(RECURSIVE).set(true);
        operation.get("recursive-depth").set(2);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to load security domain" + domain, response.getFailureDescription());
                } else {

                    ModelNode payload = response.get(RESULT);

                    for (SubResource subResource : SubResource.values()) {
                        String type = subResource.getType();
                        if (payload.hasDefined(type)) {
                            List<Property> modules = payload.get(type).get("classic").get(subResource.getModuleDef()).asPropertyList();
                            getView().updateSubResource(subResource, modules);
                        }
                        else
                        {
                            missingContainer.add(subResource);
                        }
                    }

                }

            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }


    public void onCreate(AddressTemplate address, String name, ModelNode entity) {

        ResourceAddress fqAddress = address.resolve(statementContext, domain, name);

        entity.get(OP).set(ADD);
        entity.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(entity), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failedToCreateResource(fqAddress.toString()), response.getFailureDescription());
                } else {

                    Console.info(Console.MESSAGES.successfullyAdded(fqAddress.toString()));
                }

                loadDomain();
            }
        });
    }

    public void onSave(AddressTemplate address, String name, Map<String, Object> changeset) {
        ResourceAddress fqAddress = address.resolve(statementContext, domain, name);

        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode operation = adapter.fromChangeset(changeset, fqAddress);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.MESSAGES.failedToModifyResource(fqAddress.toString()), caught.getMessage());
                loadDomain();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failedToModifyResource(fqAddress.toString()), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.successfullyModifiedResource(fqAddress.toString()));
                }
                loadDomain();
            }
        });

    }

    public void onLaunchAddWizard(final AddressTemplate type, final List<String> codes) {

        final SecurityContext securityContext =
                getSecurityFramework().getSecurityContext(getProxy().getNameToken());

        final ResourceDescription resourceDescription = getDescriptionRegistry().lookup(type);

        final DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle(type.getResourceType()));

        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        dialog.hide();


                        ModelNode compositeOp = new ModelNode();
                        compositeOp.get(ADDRESS).setEmptyList();
                        compositeOp.get(OP).set(COMPOSITE);

                        List<ModelNode> steps = new LinkedList<>();

                        String resourceType = type.getResourceType();
                        for (SubResource subResource : SubResource.values()) {
                            if (subResource.getModuleDef().equals(resourceType)) {
                                if (missingContainer.contains(subResource)) {
                                    // create parent resource if necessary
                                    ModelNode addOp = new ModelNode();
                                    ResourceAddress address = SEC_DOMAIN.append(subResource.getType() + "=classic").resolve(
                                            statementContext, domain, payload.get("name").asString()
                                    );

                                    addOp.get(ADDRESS).set(address);
                                    addOp.get(OP).set(ADD);
                                    steps.add(addOp);
                                }
                                break;
                            }
                        }

                        final ResourceAddress fqAddress =
                                type.resolve(statementContext, domain, payload.get("name").asString());

                        payload.get(OP).set(ADD);
                        payload.get(ADDRESS).set(fqAddress);

                        steps.add(payload);

                        compositeOp.get(STEPS).set(steps);

                        dispatcher.execute(new DMRAction(compositeOp), new SimpleCallback<DMRResponse>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                super.onFailure(caught);
                                loadDomain();
                            }

                            @Override
                            public void onSuccess(DMRResponse dmrResponse) {
                                Console.info(Console.MESSAGES.successfullyAdded(fqAddress.toString()));
                                loadDomain();
                            }
                        });


                    }

                    @Override
                    public void onCancel() {
                        dialog.hide();
                    }
                }

        )
                // custom field for code attribute
                .addFactory("code", attributeDescription -> {
                    SuggestBoxItem item = new SuggestBoxItem("code", "Code", true);
                    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
                    oracle.setDefaultSuggestionsFromText(codes);
                    item.setOracle(oracle);
                    return item;
                });

        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    public void onRemove(AddressTemplate cacheType, String name) {

        ResourceAddress fqAddress = cacheType.resolve(statementContext, domain, name);

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                loadDomain();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {

                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failedToRemoveResource(fqAddress.toString()), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.successfullyRemoved(fqAddress.toString()));
                }

                loadDomain();
            }
        });


    }



}