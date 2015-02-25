package org.jboss.as.console.client.core.bootstrap.server;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import static org.jboss.as.console.client.core.ApplicationProperties.*;

/**
 * Class which connects to a running JBoss management interface or triggers the selection of an arbitrary management
 * interface. By default this class first tries to connect to the management interface this console was loaded from.
 * If no server was found or if running dev mode, the selection is triggered by {@link BootstrapServerDialog}.
 * <p/>
 * <p>Please note: This class must run <em>before</em> any other bootstrap steps!</p>
 *
 * @author Harald Pehl
 */
public class BootstrapServerSetup implements Function<BootstrapContext> {

    public final static String CONNECT_PARAMETER = "connect";

    private Control<BootstrapContext> control;
    private BootstrapContext context;
    private BootstrapServerDialog dialog;

    @Override
    public void execute(final Control<BootstrapContext> control) {
        this.control = control;
        this.context = control.getContext();

        String connect = Window.Location.getParameter(CONNECT_PARAMETER);
        if (connect != null) {
            // Connect to a server given as a request parameter
            final BootstrapServer server = new BootstrapServerStore().get(connect);
            if (server != null) {
                pingServer(server, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        openDialog();
                    }

                    @Override
                    public void onSuccess(Void whatever) {
                        onConnect(server);
                    }
                });
            } else {
                openDialog();
            }

        } else {
            final String baseUrl = getBaseUrl();
            RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, baseUrl + "/management");
            requestBuilder.setCallback(new RequestCallback() {
                @Override
                public void onResponseReceived(final Request request, final Response response) {
                    int statusCode = response.getStatusCode();
                    if (statusCode == 0 || statusCode == 200 || statusCode == 401) {
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
        }
    }

    private void openDialog() {
        Console.hideLoadingPanel();
        dialog = new BootstrapServerDialog(this);
        dialog.open();
    }

    void pingServer(final BootstrapServer server, final AsyncCallback<Void> callback) {
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, getServerUrl(server));
        requestBuilder.setTimeoutMillis(2000);
        requestBuilder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(final Request request, final Response response) {
                int statusCode = response.getStatusCode();
                if (statusCode == 200) {
                    callback.onSuccess(null);
                } else {
                    Log.error("Wrong status " + statusCode + " when pinging '" + getServerUrl(server) + "'");
                    callback.onFailure(new IllegalStateException());
                }
            }

            @Override
            public void onError(final Request request, final Throwable exception) {
                Log.error("Ping.onError(): '" + getServerUrl(server) + "': " + exception.getMessage());
                callback.onFailure(new IllegalStateException());
            }
        });
        try {
            requestBuilder.send();
        } catch (RequestException e) {
            Log.error("Failed to ping '" + getServerUrl(server) + "': " + e.getMessage());
            callback.onFailure(new IllegalStateException());
        }
    }

    void onConnect(BootstrapServer server) {
        // store selected server
        new BootstrapServerStore().storeSelection(server);

        if (dialog != null) {
            dialog.hide();
            Console.showLoadingPanel();
        }
        String serverUrl = getServerUrl(server);
        context.setSameOrigin(serverUrl.equals(getBaseUrl()));
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }

        // Trigger authentication using a hidden iframe. This way also Safari will show the login dialog
        setUrls(serverUrl);
        control.proceed();
    }

    private void setUrls(String baseUrl) {
        String localBaseUrl = baseUrl;
        if (!localBaseUrl.endsWith("/")) {
            localBaseUrl += "/";
        }
        String domainApi = localBaseUrl + "management";
        String deploymentApi = localBaseUrl + "management/add-content";
        String logoutApi = localBaseUrl + "logout";
        String patchApi = localBaseUrl + "management-upload";
        String cspApi = localBaseUrl + "console/csp";

        System.out.println("Domain API Endpoint: " + domainApi);

        context.setProperty(DOMAIN_API, domainApi);
        context.setProperty(DEPLOYMENT_API, deploymentApi);
        context.setProperty(LOGOUT_API, logoutApi);
        context.setProperty(PATCH_API, patchApi);
        context.setProperty(CSP_API, cspApi);
    }

    String getBaseUrl() {
        String hostUrl = GWT.getHostPageBaseURL();
        int schemeIndex = hostUrl.indexOf("://");
        int slash = hostUrl.indexOf('/', schemeIndex + 3);
        if (slash != -1) {
            return hostUrl.substring(0, slash);
        }
        return hostUrl;
    }

    static String getServerUrl(BootstrapServer server) {
        StringBuilder builder = new StringBuilder();
        builder.append(server.getScheme()).append("://").append(server.getHostname());
        if (server.getPort() != 0) {
            builder.append(":").append(server.getPort());
        }
        return builder.toString();
    }
}
