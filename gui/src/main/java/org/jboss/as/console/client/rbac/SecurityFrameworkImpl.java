package org.jboss.as.console.client.rbac;

import com.google.gwt.core.client.Scheduler;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.proxy.Place;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.ReadRequiredResources;
import org.jboss.as.console.client.core.RequiredResourcesContext;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityContextAware;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;
import org.jboss.ballroom.client.rbac.SecurityContextChangedHandler;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Control;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The security framework maintains a {@link SecurityContext} per place
 *
 * @see com.gwtplatform.mvp.client.proxy.PlaceManager
 *
 * @author Heiko Braun
 */
public class SecurityFrameworkImpl implements SecurityFramework, SecurityContextChangedHandler {

    private static final String ADDRESS = "address";

    protected final RequiredResourcesRegistry requiredResourcesRegistry;
    protected final DispatchAsync dispatcher;
    protected final CoreGUIContext statementContext;
    protected final ContextKeyResolver keyResolver;

    private final Map<String, SecurityContextAware> contextAwareWidgets;

    protected Map<String, SecurityContext> contextMapping = new HashMap<String, SecurityContext>();

    @Inject
    public SecurityFrameworkImpl(RequiredResourcesRegistry requiredResourcesRegistry, DispatchAsync dispatcher,
                                 CoreGUIContext statementContext, EventBus eventBus) {

        this.requiredResourcesRegistry = requiredResourcesRegistry;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.keyResolver = new PlaceSecurityResolver();
        this.contextAwareWidgets = new HashMap<>();

        SecurityContextChangedEvent.register(eventBus, this);
    }

    @Override
    public String resolveToken() {
        return keyResolver.resolveKey();
    }

    @Override
    public boolean hasContext(String id) {
        return contextMapping.containsKey(id);
    }

    @Override
    public SecurityContext getSecurityContext(String id) {

        if(null==id) return new NoGatekeeperContext(); // used with connect pages

        SecurityContext securityContext = contextMapping.get(id);

        if(null==securityContext) {
            // if this happens the order of presenter initialisation is probably wrong
            String msg = "Failed to resolve security context for #" + id+" (Fallback to read only context)";
            new RuntimeException(msg).printStackTrace();
            securityContext = new ReadOnlyContext();

        }

        return securityContext;
    }

    @Override
    public void registerWidget(final String id, final SecurityContextAware widget) {
        contextAwareWidgets.put(id, widget);
        widget.onSecurityContextChanged(); // apply security context when being loaded

    }

    @Override
    public void unregisterWidget(final String id) {
        contextAwareWidgets.remove(id);
    }

    @Override
    public void onSecurityContextChanged(final SecurityContextChangedEvent event) {

        Presenter presenter = (Presenter) event.getSource(); // mandatory, see SecurityContextChangedEvent#fire()
        if(!(presenter.getProxy() instanceof Place))
            throw new IllegalArgumentException("Source needs to be presenter place");

        final String token = ((Place)presenter.getProxy()).getNameToken();

        /*AddressTemplate addressTemplate = AddressTemplate.of(event.getResourceAddress());
        String resolvedKey = addressTemplate.resolveAsKey(statementContext, event.getWildcards());*/

        // instead of using a specific address, we can rely on one of the address associated with the current context
        // assumption: if that single address exists, all the others should also
        SecurityContext securityContext = getSecurityContext(token);
        if(!(securityContext instanceof SecurityContextImpl))
            throw new IllegalStateException("Cannot process context change on security context of type "+securityContext.getClass());


        // reset clear the previous child context selection
        if(event.isReset()) {

            System.out.println("Context reset at #" + token + ": " + token);

            deactivateChildContexts((SecurityContextImpl) securityContext, event);
        }
        else
        {

            AddressTemplate probeTemplate =  ((SecurityContextImpl)securityContext).getResourceAddresses().iterator().next();
            String probeKey = event.getResolver().resolve(probeTemplate);

            System.out.println("Context changed: " + probeKey + " at #" + token + ": " + token);

            SecurityContext context = contextMapping.get(token);

            // look for child context
            if (context.hasChildContext(probeTemplate, probeKey)) { // the probe is one of many, just used for testing the condition

                activetChildContexts((SecurityContextImpl)securityContext, event);

            } else {

                // not found, create and activate it
                lazyLoadChildcontext(
                        context, token,
                        event
                );

            }

        }

    }

    private void deactivateChildContexts(SecurityContextImpl securityContext, SecurityContextChangedEvent event) {
        for (AddressTemplate requiredResource : securityContext.requiredResources) {
            securityContext.activateChildContext(requiredResource, null);

        }

        notifyWidgets(event, securityContext.nameToken);

    }

    private void activetChildContexts(SecurityContextImpl securityContext, SecurityContextChangedEvent event) {
        for (AddressTemplate requiredResource : securityContext.requiredResources) {
            String resolvedKey = event.getResolver().resolve(requiredResource);
            securityContext.activateChildContext(requiredResource, resolvedKey);
        }

        notifyWidgets(event, securityContext.nameToken);
    }

    private void notifyWidgets(SecurityContextChangedEvent event, final String token) {
        if(event.getPostContruct()!=null)
            event.getPostContruct().execute();

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                forceUpdate(token);
            }
        });
    }

    private void lazyLoadChildcontext(final SecurityContext parentContext, String token,
            SecurityContextChangedEvent event) {


        final Set<AddressTemplate> resources = new HashSet<AddressTemplate>();
        final Map<AddressTemplate, String> resolvedKeys = new HashMap<>();
        final Map<AddressTemplate, AddressTemplate> childToParent = new HashMap<>();

        // Merge parent context addresses to retain full scope,
        // but adopt to child specifics
        for (AddressTemplate requiredResource : ((SecurityContextImpl) parentContext).requiredResources) {

            String resolvedKey = event.getResolver().resolve(requiredResource);
            resolvedKeys.put(requiredResource, resolvedKey);
            AddressTemplate child = AddressTemplate.of(resolvedKey);
            resources.add(child);

            childToParent.put(child, requiredResource);
        }

        final RequiredResourcesContext ctx = new RequiredResourcesContext(token);
        final ReadRequiredResources operation = new ReadRequiredResources(dispatcher, statementContext);

        // fetch each resolved resource
        for (AddressTemplate resource : resources) {
            operation.add(resource, false);
        }

        operation.execute(
                new Control<RequiredResourcesContext>() {
                    @Override
                    public void proceed() {

                        // although it has been resolved as parent context, we register it as child
                        // the same twist applies to the addressing above
                        for (AddressTemplate address : resources) {
                            AddressTemplate parentAddress = childToParent.get(address);
                            Constraints constraints = ctx.getParentConstraints().get(address);

                            String resolvedKey = resolvedKeys.get(parentAddress);
                            ((SecurityContextImpl)parentContext).addChildContext(parentAddress, resolvedKey, constraints);

                            // activate the new context right away
                            parentContext.activateChildContext(parentAddress, resolvedKey);
                        }

                        notifyWidgets(event, token);
                    }

                    @Override
                    public void abort() {

                        if (event.getPostContruct() != null)
                            event.getPostContruct().execute();

                        Console.error("Failed to create security (child) context for #" + token, ctx.getError().getMessage());
                    }

                    @Override
                    public RequiredResourcesContext getContext() {
                        return ctx;
                    }
                }
        );
    }


    @Override
    public void assignContext(String id, SecurityContext context) {
        contextMapping.put(id, context);
    }

    private static String normalize(final ModelNode address) {
        if (address.isDefined()) {
            StringBuilder normalized = new StringBuilder();
            List<Property> properties = address.asPropertyList();
            for (Property property : properties) {
                normalized.append("/").append(property.getName()).append("=").append(property.getValue().asString());
            }
            return normalized.toString();
        }
        return null;
    }


    @Override
    public Set<String> getReadOnlyJavaNames(Class<?> type, SecurityContext securityContext) {

        // Fallback
        if(type == Object.class || type == null)
            return Collections.emptySet();

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getReadOnlyJavaNames(type, securityContext);
    }

    @Override
    public Set<String> getReadOnlyJavaNames(Class<?> type, String resourceAddress, SecurityContext securityContext) {

        // Fallback
        if(type == Object.class || type == null)
            return Collections.emptySet();

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getReadOnlyJavaNames(type, resourceAddress, securityContext);
    }

    @Override
    public Set<String> getFilteredJavaNames(Class<?> type, String resourceAddress, SecurityContext securityContext) {
        // Fallback
        if(type == Object.class || type == null)
            return Collections.emptySet();

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getFilteredJavaNames(type, resourceAddress, securityContext);
    }

    @Override
    public Set<String> getFilteredJavaNames(Class<?> type, SecurityContext securityContext) {
        // Fallback
        if(type == Object.class || type == null)
            return Collections.emptySet();

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getFilteredJavaNames(type,  securityContext);
    }

    @Override
    public Set<String> getReadOnlyDMRNames(String resourceAddress, List<String> formItemNames, SecurityContext securityContext) {

        // TODO: at some point this should refer to the actual resource address
        Set<String> readOnly = new HashSet<String>();
        for(String item : formItemNames)
        {
            if(!securityContext.getAttributeWritePriviledge(item).isGranted())
                readOnly.add(item);
        }
        return readOnly;
    }

    @Override
    public Set<String> getFilteredDMRNames(String resourceAddress, List<String> formItemNames, SecurityContext securityContext) {
        // TODO: at some point this should refer to the actual resource address
        Set<String> readOnly = new HashSet<String>();
        for(String item : formItemNames)
        {
            boolean writepriv = securityContext.getAttributeWritePriviledge(item).isGranted();
            boolean readpriv = securityContext.getAttributeReadPriviledge(item).isGranted();

            if(!writepriv && !readpriv)
                readOnly.add(item);
        }
        return readOnly;
    }

    @Override
    public void forceUpdate(String id) {
        // update widgets (if attached and filter applies)
        for (Map.Entry<String, SecurityContextAware> entry : contextAwareWidgets.entrySet()) {

            SecurityContextAware widget = entry.getValue();
            if(widget.getToken().equals(id)) {   // only touch the ones that matter

                /*
                TODO: is the filter still needed?

                boolean update = true;

                if (widget.getFilter() != null) {
                    update = widget.getFilter().equals(addressTemplate);
                }
                if (update && widget.isAttached()) {
                    widget.onSecurityContextChanged();
                }*/

                if (widget.isAttached()) {
                    widget.onSecurityContextChanged();
                }
            }
        }
    }
}
