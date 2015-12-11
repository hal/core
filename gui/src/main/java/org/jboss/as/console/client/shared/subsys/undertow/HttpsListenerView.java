package org.jboss.as.console.client.shared.subsys.undertow;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class HttpsListenerView extends AbstractListenerView {

    private static final AddressTemplate  BASE_ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=undertow/server={undertow.server}/https-listener=*");

    public HttpsListenerView(HttpPresenter presenter) {
        super(presenter, BASE_ADDRESS, "HTTPS Listener");
    }

}
