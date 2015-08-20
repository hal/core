package org.jboss.as.console.client.core.bootstrap.cors;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.LoadingPanel;

import static org.jboss.as.console.client.core.ApplicationProperties.*;

/**
 * Class which connects to a running management interface or triggers the selection of an arbitrary management
 * interface. By default this class first tries to connect to the management interface this console was loaded from.
 * If no server was found, the selection is triggered by {@link BootstrapServerDialog}.
 * <p>
 * Please note: This class must run <em>before</em> any other bootstrap steps!
 *
 * @author Harald Pehl
 */
public class BootstrapServerSetup {

    public final static String CONNECT_PARAMETER = "connect";

    private final BootstrapContext context;
    private BootstrapServerDialog dialog;
    private ScheduledCommand andThen;

    @Inject
    public BootstrapServerSetup(BootstrapContext context) {
        this.context = context;
    }

    public void select(ScheduledCommand andThen) {
        this.andThen = andThen;
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
            // Test whether this console is served from a WildFly / EAP instance
            RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, baseUrl + "/management");
            requestBuilder.setCallback(new RequestCallback() {
                @Override
                public void onResponseReceived(final Request request, final Response response) {
                    int statusCode = response.getStatusCode();
                    // anything but 404 is considered successful
                    if (statusCode == 0 || statusCode == 200 || statusCode == 401) {
                        setUrls(baseUrl);
                        andThen.execute();
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
        LoadingPanel.get().off();
        dialog = new BootstrapServerDialog(this);
        dialog.open();
    }

    void pingServer(final BootstrapServer server, final AsyncCallback<Void> callback) {
        final String managementEndpoint = getServerUrl(server) + "/management";
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, managementEndpoint);
        requestBuilder.setIncludeCredentials(true);
        requestBuilder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(final Request request, final Response response) {
                int statusCode = response.getStatusCode();
                if (statusCode == 200) {
                    callback.onSuccess(null);
                } else {
                    Log.error("Wrong status " + statusCode + " when pinging '" + managementEndpoint + "'");
                    callback.onFailure(new IllegalStateException());
                }
            }

            @Override
            public void onError(final Request request, final Throwable exception) {
                Log.error("Ping.onError(): '" + managementEndpoint + "': " + exception.getMessage());
                callback.onFailure(new IllegalStateException());
            }
        });
        try {
            requestBuilder.send();
        } catch (RequestException e) {
            Log.error("Failed to ping '" + managementEndpoint + "': " + e.getMessage());
            callback.onFailure(new IllegalStateException());
        }
    }

    void onConnect(BootstrapServer server) {
        // store selected server
        new BootstrapServerStore().storeSelection(server);

        if (dialog != null) {
            dialog.hide();
            LoadingPanel.get().on();
        }
        String serverUrl = getServerUrl(server);
        context.setSameOrigin(serverUrl.equals(getBaseUrl()));
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }

        // Trigger authentication using a hidden iframe. This way also Safari will show the login dialog
        setUrls(serverUrl);
        andThen.execute();
    }

    private void setUrls(String baseUrl) {
        String localBaseUrl = baseUrl;
        if (!localBaseUrl.endsWith("/")) {
            localBaseUrl += "/";
        }
        String domainApi = localBaseUrl + "management";
        String uploadApi = localBaseUrl + "management-upload";
        String logoutApi = localBaseUrl + "logout";
        String cspApi = localBaseUrl + "console/csp";

        System.out.println("Domain API Endpoint: " + domainApi);
        context.setProperty(DOMAIN_API, domainApi);
        context.setProperty(UPLOAD_API, uploadApi);
        context.setProperty(LOGOUT_API, logoutApi);
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
