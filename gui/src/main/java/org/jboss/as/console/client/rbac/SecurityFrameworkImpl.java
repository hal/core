package org.jboss.as.console.client.rbac;

import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.proxy.Place;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityContextAware;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;
import org.jboss.ballroom.client.rbac.SecurityContextChangedHandler;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;

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
    protected Map<String, SecurityContext> subContextMapping = new HashMap<String, SecurityContext>();

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

        SecurityContext securityContext = null;

        if(subContextMapping.containsKey(id)) // child context enabled?
            securityContext = subContextMapping.get(id);
        else
            securityContext = contextMapping.get(id);

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
        final String addressTemplate = event.getResourceAddress();

        ModelNode addressNode = AddressMapping.fromString(addressTemplate)
                .asResource(statementContext, event.getWildcards()
                );

        String resourceAddress = normalize(addressNode.get(ADDRESS));

        SecurityContext context = contextMapping.get(token);// important: getSecurityContext() is not side effect free

        // look for child context
        if (context.hasChildContext(resourceAddress)) {
            SecurityContext childContext = context.getChildContext(resourceAddress);
            subContextMapping.put(token, childContext);
        }
        else {
            subContextMapping.remove(token);
        }

        forceUpdate(token);

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
