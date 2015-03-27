package org.jboss.as.console.client.preview;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundleWithLookup;
import com.google.gwt.resources.client.ExternalTextResource;

/**
 * @author Heiko Braun
 * @since 27/03/15
 */
public interface PreviewContent extends ClientBundleWithLookup {
    PreviewContent INSTANCE = GWT.create(PreviewContent.class);

    @Source("profiles_empty.html")
    ExternalTextResource profiles_empty();

    @Source("profiles_profile.html")
    ExternalTextResource profiles_profile();

    @Source("interfaces.html")
    ExternalTextResource interfaces();

    @Source("sockets.html")
    ExternalTextResource sockets();

    @Source("paths.html")
    ExternalTextResource paths();

    @Source("properties.html")
    ExternalTextResource properties();


    // subsystems below (referenced by name token)

    @Source("subsystems/jca.html")
    ExternalTextResource jca();

    @Source("subsystems/datasources.html")
    ExternalTextResource datasources();

}
