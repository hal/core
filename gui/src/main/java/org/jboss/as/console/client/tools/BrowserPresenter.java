package org.jboss.as.console.client.tools;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PopupView;
import com.gwtplatform.mvp.client.PresenterWidget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.util.LRUCache;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.mbui.widgets.AddressUtils;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 6/15/12
 */
public class BrowserPresenter extends PresenterWidget<BrowserPresenter.MyView>{

    private static final ModelNode ROOT = new ModelNode().setEmptyList();
    private DispatchAsync dispatcher;
    private boolean hasBeenRevealed;
    private ModelNode pinToAddress = null;

    private LRUCache<String, SecurityContext> contextCache = new LRUCache<String, SecurityContext>(25);

    public interface MyView extends PopupView {
        void setPresenter(BrowserPresenter presenter);
        void updateRootTypes(ModelNode address, List<ModelNode> modelNodes);
        void updateChildrenTypes(ModelNode address, List<ModelNode> modelNodes);
        void updateChildrenNames(ModelNode address, List<ModelNode> modelNodes, boolean flagSquatting);
        void updateResource(ModelNode address, SecurityContext securityContext, ModelNode description, ModelNode resource);

        void showAddDialog(ModelNode address, SecurityContext securityContext, ModelNode desc);
    }

    @Inject
    public BrowserPresenter(
            EventBus eventBus, MyView view,
            DispatchAsync dispatcher) {
        super(eventBus, view);

        this.dispatcher = dispatcher;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReveal() {
        if(!hasBeenRevealed)
        {
            hasBeenRevealed = true;
            onRefresh();
        }
    }

    public void onRefresh() {

        ModelNode target = pinToAddress != null ? pinToAddress : ROOT;

        readChildrenTypes(target, true);
        readResource(target);
    }

    class DMRContext {
        ModelNode response;
        boolean flagSquatting;
    }

    public void readChildrenTypes(final ModelNode address, final boolean resetRoot) {

        Function<DMRContext> fn = new Function<DMRContext>() {
            @Override
            public void execute(final Control<DMRContext> control) {

                // read children types
                ModelNode childTypeOp  = new ModelNode();
                childTypeOp.get(ADDRESS).set(address);
                childTypeOp.get(OP).set(READ_CHILDREN_TYPES_OPERATION);

                dispatcher.execute(new DMRAction(childTypeOp), new SimpleCallback<DMRResponse>() {
                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {
                        ModelNode dmrRsp = dmrResponse.get();
                        control.getContext().response = dmrRsp;

                        if(dmrRsp.isFailure())
                            control.abort();
                        else
                            control.proceed();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Console.error("Failed to load children types: "+ caught.getMessage());
                        control.abort();
                    }
                });
            }
        };

        new Async(Footer.PROGRESS_ELEMENT).waterfall(new DMRContext(), new Outcome<DMRContext>() {
            @Override
            public void onFailure(DMRContext context) {
                Console.error("Failed ot load children types: "+context.response.getFailureDescription());
            }

            @Override
            public void onSuccess(DMRContext context) {
                ModelNode response = context.response;
                if(resetRoot)
                    getView().updateRootTypes(address, response.get(RESULT).asList());
                else
                    getView().updateChildrenTypes(address, response.get(RESULT).asList());
            }
        }, fn);
    }

    public void readChildrenNames(final ModelNode address) {

        final List<ModelNode> addressList = address.asList();
        final List<ModelNode> actualAddress = new ArrayList<ModelNode>();

        ModelNode typeDenominator = null;
        int i=0;
        for(ModelNode path : addressList)
        {
            if(i<addressList.size()-1)
                actualAddress.add(path);
            else
                typeDenominator = path;

            i++;
        }

        final String typeName = typeDenominator.asProperty().getName();

        Function<DMRContext> squatterFn = new Function<DMRContext>() {
            @Override
            public void execute(final Control<DMRContext> control) {

                ModelNode operation  = new ModelNode();
                operation.get(ADDRESS).set(address);
                operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);

                dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        Log.error("Failed to load child names: " + caught.getMessage());

                        control.getContext().flagSquatting = true;
                        control.proceed();
                    }

                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {
                        ModelNode dmrRsp = dmrResponse.get();

                        // TODO (hbraun): workaround for https://issues.jboss.org/browse/WFLY-3706
                        if(dmrRsp.isFailure() || dmrRsp.get(RESULT).isFailure())
                        {
                            control.getContext().flagSquatting = true;
                            //System.out.println("squatting: "+ address);
                            control.proceed();
                        }
                        else
                        {
                            control.proceed();
                        }
                    }
                });

            }
        };

        Function<DMRContext> childNameFn = new Function<DMRContext>() {
            @Override
            public void execute(final Control<DMRContext> control) {

                ModelNode operation  = new ModelNode();
                operation.get(ADDRESS).set(actualAddress);
                operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
                operation.get(CHILD_TYPE).set(typeName);

                dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        Console.error("Failed to load child names: "+caught.getMessage());
                        control.abort();
                    }

                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {
                        ModelNode dmrRsp = dmrResponse.get();
                        control.getContext().response = dmrRsp;

                        if(dmrRsp.isFailure())
                        {
                            control.abort();
                        }
                        else
                        {
                            control.proceed();
                        }
                    }
                });

            }
        };

        new Async(Footer.PROGRESS_ELEMENT).waterfall(new DMRContext(), new Outcome<DMRContext>() {
            @Override
            public void onFailure(DMRContext context) {
                Console.error("Failed to load children names: "+ context.response.getFailureDescription());
            }

            @Override
            public void onSuccess(DMRContext context) {
                ModelNode response = context.response;
                getView().updateChildrenNames(address, response.get(RESULT).asList(), context.flagSquatting);
            }
        }, squatterFn, childNameFn);

    }

    class ResourceData {
        SecurityContext securityContext;
        ModelNode description;
        ModelNode data;
    }

    public void readResource(final ModelNode address) {


        Function<ResourceData> secFn = new Function<ResourceData>() {
            @Override
            public void execute(final Control<ResourceData> control) {

                SecurityFramework securityFramework = Console.MODULES.getSecurityFramework();

                final String addressString = AddressUtils.toString(address, false); // TODO: what about squatting resources?

                final Set<String> resources = new HashSet<String>();
                resources.add(addressString);

                if(contextCache.containsKey(addressString))
                {
                    control.getContext().securityContext = contextCache.get(addressString);
                    control.proceed();
                }
                else {

                    securityFramework.createSecurityContext(addressString, resources, false,
                            new AsyncCallback<SecurityContext>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    Console.error("Failed to create security context for "+addressString, caught.getMessage());
                                    control.abort();
                                }

                                @Override
                                public void onSuccess(SecurityContext result) {
                                    final String cacheKey = AddressUtils.asKey(address, false);
                                    contextCache.put(cacheKey, result);
                                    control.getContext().securityContext = result;
                                    control.proceed();
                                }
                            }
                    );
                }

            }
        };

        Function<ResourceData> descFn = new Function<ResourceData>() {
            @Override
            public void execute(final Control<ResourceData> control) {
                _readDescription(address, new SimpleCallback<ModelNode>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        Console.error("Failed to read resource description: " + caught.getMessage());
                        control.abort();
                    }

                    @Override
                    public void onSuccess(ModelNode result) {
                        control.getContext().description = result;
                        control.proceed();
                    }
                });
            }
        };

        Function<ResourceData> dataFn = new Function<ResourceData>() {
            @Override
            public void execute(final Control<ResourceData> control) {
                _readResouce(address, new SimpleCallback<ModelNode>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        Console.error("Failed to read resource: "+caught.getMessage());
                        control.abort();
                    }

                    @Override
                    public void onSuccess(ModelNode result) {
                        control.getContext().data = result;
                        control.proceed();
                    }
                });
            }
        };


        new Async(Footer.PROGRESS_ELEMENT).waterfall(new ResourceData(), new Outcome<ResourceData>() {
            @Override
            public void onFailure(ResourceData context) {

            }

            @Override
            public void onSuccess(ResourceData context) {
                getView().updateResource(address, context.securityContext, context.description, context.data);
            }
        }, secFn, descFn, dataFn);


    }

    private void _readDescription(ModelNode address, final AsyncCallback<ModelNode> callback) {

        final ModelNode descriptionOp  = new ModelNode();
        descriptionOp.get(ADDRESS).set(address);
        descriptionOp.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        descriptionOp.get(OPERATIONS).set(true);

        dispatcher.execute(new DMRAction(descriptionOp), new SimpleCallback<DMRResponse>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        callback.onFailure(caught);
                    }

                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {

                        final ModelNode response = dmrResponse.get();

                        if(response.isFailure())
                        {
                            callback.onFailure(new RuntimeException("Failed to load resource description: "+response.getFailureDescription()));
                        }
                        else {


                            ModelNode resourceDescription = null;

                            if (ModelType.LIST.equals(response.get(RESULT).getType()))
                                resourceDescription = response.get(RESULT).asList().get(0).get(RESULT).asObject();
                            else {
                                // workaround ...
                                if (!response.hasDefined(RESULT)) {
                                    Console.warning("Failed to read description" + descriptionOp.get(ADDRESS));
                                    resourceDescription = new ModelNode();
                                } else {
                                    resourceDescription = response.get(RESULT).asObject();
                                }
                            }

                            callback.onSuccess(resourceDescription);
                        }
                    }
                }
        );
    }


    private void _readResouce(ModelNode address, final AsyncCallback<ModelNode> callback) {

        // the actual values
        final ModelNode resourceOp  = new ModelNode();
        resourceOp.get(ADDRESS).set(address);
        resourceOp.get(OP).set(READ_RESOURCE_OPERATION);
        resourceOp.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(resourceOp), new SimpleCallback<DMRResponse>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        callback.onFailure(caught);
                    }

                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {

                        final ModelNode response = dmrResponse.get();

                        if(response.isFailure())
                        {
                            callback.onFailure(new RuntimeException("Failed to load resource: "+response.getFailureDescription()));
                        }
                        else
                        {

                            ModelNode resourceData = response.get(RESULT).asObject();
                            callback.onSuccess(resourceData);
                        }
                    }
                }
        );
    }

    public void onSaveResource(final ModelNode address, Map<String, Object> changeset) {

        final ModelNodeAdapter adapter = new ModelNodeAdapter();

        ModelNode operation = adapter.fromChangeset(changeset, address);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if (response.isFailure()) {
                    Console.error("Failed to save resource " + address.asString(), response.getFailureDescription());
                }
                else {
                    Console.info("Successfully saved resource " + address.asString());
                    readResource(address); // refresh
                }

            }
        });


    }

    /**
     * Creates a permanent selection for a subtree
     * @param address
     */
    public void onPinTreeSelection(ModelNode address) {
        this.pinToAddress = address;
        onRefresh();
    }

    /**
     * Remove a child resource
     * @param address
     * @param selection
     */
    public void onRemoveChildResource(final ModelNode address, final ModelNode selection) {

        final ModelNode fqAddress = AddressUtils.toFqAddress(address, selection.asString());

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(REMOVE);
        operation.get(ADDRESS).set(fqAddress);

        Feedback.confirm(
                "Remove Resource",
                "Do you really want to remove resource "+fqAddress.toString(),
                new Feedback.ConfirmationHandler() {
                    @Override
                    public void onConfirmation(boolean isConfirmed) {

                        if(isConfirmed) {

                            dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                                @Override
                                public void onSuccess(DMRResponse dmrResponse) {
                                    ModelNode response = dmrResponse.get();

                                    if (response.isFailure()) {
                                        Console.error("Failed to remove resource " + fqAddress.asString(), response.getFailureDescription());
                                    } else {
                                        Console.info("Successfully removed resource " + fqAddress.asString());
                                        readChildrenNames(address);
                                    }

                                }
                            });
                        }
                    }
                }
        );
    }

    /**
     * Add a child resource
     * @param address
     * @param isSquatting
     */
    public void onPrepareAddChildResource(final ModelNode address, final boolean isSquatting) {


        Function<ResourceData> secFn = new Function<ResourceData>() {
            @Override
            public void execute(final Control<ResourceData> control) {

                SecurityFramework securityFramework = Console.MODULES.getSecurityFramework();

                final String addressString = AddressUtils.toString(address, isSquatting);

                final Set<String> resources = new HashSet<String>();
                resources.add(addressString);

                if(contextCache.containsKey(addressString))
                {
                    control.getContext().securityContext = contextCache.get(addressString);
                    control.proceed();
                }
                else {

                    securityFramework.createSecurityContext(addressString, resources, false,
                            new AsyncCallback<SecurityContext>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    Console.error("Failed to create security context for "+addressString, caught.getMessage());
                                    control.abort();
                                }

                                @Override
                                public void onSuccess(SecurityContext result) {
                                    final String cacheKey = AddressUtils.asKey(address, false);
                                    contextCache.put(cacheKey, result);
                                    control.getContext().securityContext = result;
                                    control.proceed();
                                }
                            }
                    );
                }

            }
        };

        Function<ResourceData> descFn = new Function<ResourceData>() {
            @Override
            public void execute(final Control<ResourceData> control) {

                final ModelNode operation = new ModelNode();
                operation.get(ADDRESS).set(address);
                operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
                operation.get(OPERATIONS).set(true);

                dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {
                        ModelNode response = dmrResponse.get();

                        if (response.isFailure()) {
                            Console.error("Failed to read resource description" + address.asString(),
                                    response.getFailureDescription());
                        } else {

                            ModelNode desc = null;
                            ModelNode result = response.get(RESULT);
                            if (ModelType.LIST.equals(result.getType()))
                                desc = result.asList().get(0).get(RESULT).asObject();
                            else {
                                // workaround ...
                                if (!result.hasDefined(RESULT)) {
                                    Console.warning("Failed to read resource description" + address);
                                } else {
                                    desc = result.asObject();
                                }
                            }

                            if (desc != null) {

                                control.getContext().description = desc;
                                control.proceed();
                            }

                        }

                    }
                });
            }
        };

        new Async(Footer.PROGRESS_ELEMENT).waterfall(new ResourceData(), new Outcome<ResourceData>() {
            @Override
            public void onFailure(ResourceData context) {

            }

            @Override
            public void onSuccess(ResourceData context) {
                getView().showAddDialog(address, context.securityContext, context.description);
            }
        }, secFn, descFn);

    }

    public void onAddChildResource(final ModelNode address, ModelNode resource) {

        final ModelNode fqAddress = AddressUtils.toFqAddress(address, resource.get("name").asString());

        resource.get(OP).set(ADD);
        resource.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(resource), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if (response.isFailure()) {
                    Console.error("Failed to add resource " + fqAddress.asString(),
                            response.getFailureDescription());
                } else {
                    readChildrenNames(address);
                }

            }
        });
    }

}
