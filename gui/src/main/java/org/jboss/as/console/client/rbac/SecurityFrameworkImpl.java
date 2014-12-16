package org.jboss.as.console.client.rbac;

import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityContextAware;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;
import org.jboss.ballroom.client.rbac.SecurityContextChangedHandler;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import javax.inject.Inject;
import java.util.*;

/**
 * The security manager creates and provides a {@link SecurityContext} per place
 *
 * @see com.gwtplatform.mvp.client.proxy.PlaceManager
 *
 * @author Heiko Braun
 */
public class SecurityFrameworkImpl implements SecurityFramework, SecurityContextChangedHandler {

    private static final String ADDRESS = "address";

    private final CoreGUIContext coreGUIContext;
    private final ContextKeyResolver keyResolver;
    private Map<String, SecurityContext> contextMapping = new HashMap<String, SecurityContext>();
    private final Map<String, SecurityContextAware> contextAwareWidgets;

    @Inject
    public SecurityFrameworkImpl(CoreGUIContext coreGUIContext, EventBus eventBus) {
        this.coreGUIContext = coreGUIContext;
        this.keyResolver = new PlaceSecurityResolver();
        this.contextAwareWidgets = new HashMap<String, SecurityContextAware>();
        SecurityContextChangedEvent.register(eventBus, this);
    }

    @Override
    public void assignContext(String id, SecurityContext context) {
        contextMapping.put(id, context);
    }

    @Override
    public SecurityContext getSecurityContext() {
        return getSecurityContext(keyResolver.resolveKey());
    }

    @Override
    public boolean hasContext(String id) {
        return contextMapping.containsKey(id);
    }

    @Override
    public SecurityContext getSecurityContext(String id) {
        return contextMapping.get(id);
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
        SecurityContext context = event.getSecurityContext();
        String addressTemplate = event.getResourceAddress();
        // System.out.println("<SCC>");

        if (context == null) {
            // address resolution
            ModelNode addressNode = AddressMapping.fromString(addressTemplate).asResource(coreGUIContext,
                    event.getWildcards());
            String resourceAddress = normalize(addressNode.get(ADDRESS));
            // System.out.println(
            //         "\tReceiving security context change event for " + addressTemplate + " -> " + resourceAddress);

            // look for child context
            context = getSecurityContext();
            if (context.hasChildContext(resourceAddress)) {
                System.out.println("\tFound child context for " + resourceAddress);
                context = context.getChildContext(resourceAddress);
            }
        }/* else {
            System.out.println("\tReceiving security context change event for " + context);
        }*/

        // update widgets (if visible and filter applies)
        for (Map.Entry<String, SecurityContextAware> entry : contextAwareWidgets.entrySet()) {
            String id = entry.getKey();
            SecurityContextAware widget = entry.getValue();

            boolean update = true;
            if (widget.getFilter() != null) {
                update = widget.getFilter().equals(addressTemplate);
            }
            if (update && widget.isAttached()) {
                //System.out.println("\tUpdating widget " + id);
                widget.updateSecurityContext(context);
            }
        }
        //System.out.println("</SCC>\n");
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
    public void flushContext(String nameToken) {
        contextMapping.remove(nameToken);
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
}
