package org.jboss.as.console.client.core.bootstrap.server;

import static com.google.gwt.user.client.Event.ONLOAD;
import static org.jboss.as.console.client.core.ApplicationProperties.*;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * Class which connects to a running JBoss management interface or triggers the selection of an arbitrary management
 * interface. By default this class first tries to connect to the management interface this console was loaded from.
 * If no server was found or if running dev mode, the selection is triggered by {@link BootstrapServerDialog}.
 * <p/>
 * <p>Please note: This class must run <em>before</em> any bootstrap steps!</p>
 *
 * @author Harald Pehl
 * @date 02/27/2013
 */
public class BootstrapServerSetup implements Function<BootstrapContext> {

    private final static String IFRAME_ID = "__console_corsAuthentication";
    private Control<BootstrapContext> control;
    private BootstrapContext context;
    private DefaultWindow window;
    private BootstrapServerDialog dialog;


    @Override
    public void execute(final Control<BootstrapContext> control) {
        this.control = control;
        this.context = control.getContext();

        if (GWT.isScript()) {
            // Check if the console is part of a running JBoss AS instance
            final String baseUrl = getBaseUrl();
            // TODO Think of a better way to check the URL. If the standalone console was served from
            // http://www.acme.org/console this code would assume that it is part of a running server instance!
            RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, baseUrl + "console");
            requestBuilder.setCallback(new RequestCallback() {
                @Override
                public void onResponseReceived(final Request request, final Response response) {
                    // Use this JBoss instance for building the URLs
                    int statusCode = response.getStatusCode();
                    if (statusCode == 0 || statusCode == 200 || statusCode == 401) // firefox returns 0
                    {
                        setUrls(baseUrl);
                        control.proceed();
                    } else {
                        openDialog();
                    }
                }

                @Override
                public void onError(final Request request, final Throwable exception) {
                    // This is a 'standalone' console. Show selection dialog
                    openDialog();
                }
            });
            try {
                requestBuilder.send();
            } catch (RequestException e) {
                openDialog();
            }
        } else {
            openDialog();
        }
    }

    private void openDialog() {
        dialog = new BootstrapServerDialog(this);
        window = new DefaultWindow("Connect to Server");
        window.setWidth(600);
        window.setHeight(400);
        window.trapWidget(dialog.getConnectPage().asWidget());
        window.setGlassEnabled(true);
        window.center();
        dialog.getConnectPage().reset();
    }

    void pingServer(final BootstrapServer server, final AsyncCallback<Void> callback) {
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, server.getUrl());
        requestBuilder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(final Request request, final Response response) {
                if (response.getStatusCode() == 200 || response.getStatusCode() == 0) // firefox returns 0
                {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(new IllegalStateException(
                            "Server " + server.getName() + " at " + server.getUrl() + " not running!"));
                }
            }

            @Override
            public void onError(final Request request, final Throwable exception) {
                callback.onFailure(new IllegalStateException(
                        "Server " + server.getName() + " at " + server.getUrl() + " not running!"));
            }
        });
        try {
            requestBuilder.send();
        } catch (RequestException e) {
            callback.onFailure(new IllegalStateException(
                    "Server " + server.getName() + " at " + server.getUrl() + " not running!"));
        }
    }

    void onConfigure() {
        // Changing the title is not implemented in DefaultWindow window.setTitle("Configure Server");
        window.trapWidget(dialog.getConfigurePage().asWidget());
        dialog.getConfigurePage().reset();
    }

    void onConfigureOk() {
        // Changing the title is not implemented in DefaultWindow window.setTitle("Connect to Server");
        window.trapWidget(dialog.getConnectPage().asWidget());
        dialog.getConnectPage().reset();
    }

    void onConfigureCancel() {
        // Changing the title is not implemented in DefaultWindow window.setTitle("Connect to Server");
        window.trapWidget(dialog.getConnectPage().asWidget());
    }

    void onConnect(BootstrapServer server) {
        window.hide();
        setUrls(server.getUrl());

        // Trigger authentication using a hidden iframe. This way also Safari will show the login dialog
        Element iframe = Document.get().getElementById(IFRAME_ID).cast();
        DOM.sinkEvents(iframe, ONLOAD);
        DOM.setEventListener(iframe, new EventListener() {
            @Override
            public void onBrowserEvent(final Event event) {
                if (DOM.eventGetType(event) == ONLOAD) {
                    control.proceed();
                }
            }
        });
        iframe.setAttribute("src", context.getProperty(BootstrapContext.DOMAIN_API));

    }

    private void setUrls(String baseUrl) {
        String localBaseUrl = baseUrl;
        if (!localBaseUrl.endsWith("/")) {
            localBaseUrl += "/";
        }
        String domainApi = localBaseUrl + "management";
        String deploymentApi = localBaseUrl + "management/add-content";
        String logoutApi = localBaseUrl + "logout";
        System.out.println("Domain API Endpoint: " + domainApi);

        context.setProperty(DOMAIN_API, domainApi);
        context.setProperty(DEPLOYMENT_API, deploymentApi);
        context.setProperty(LOGOUT_API, logoutApi);
    }

    private String getBaseUrl() {
        String base = GWT.getHostPageBaseURL();

        String host;
        String port;
        String protocol = base.substring(0, base.indexOf("//") + 2);
        String remainder = base.substring(base.indexOf(protocol) + protocol.length(), base.length());

        int portDelim = remainder.indexOf(":");
        if (portDelim != -1) {
            host = remainder.substring(0, portDelim);
            String portRemainder = remainder.substring(portDelim + 1, remainder.length());
            if (portRemainder.contains("/")) {
                port = portRemainder.substring(0, portRemainder.indexOf("/"));
            } else {
                port = portRemainder;
            }
        } else {
            host = remainder.substring(0, remainder.indexOf("/"));
            if ("https://".equalsIgnoreCase(protocol)) {
                port = "443";
            } else {
                port = "80";
            }
        }

        // default url
        return protocol + host + ":" + port + "/";
    }
}
