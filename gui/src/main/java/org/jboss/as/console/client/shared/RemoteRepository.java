package org.jboss.as.console.client.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.mbui.DialogRepository;
import org.jboss.as.console.mbui.marshall.DialogXML;
import org.useware.kernel.model.Dialog;

/**
 * @author Heiko Braun
 * @date 18/11/13
 */
public class RemoteRepository implements DialogRepository {

    private final String repoUrl;

    public RemoteRepository() {
        repoUrl = GWT.getHostPageBaseURL()+"repo";
    }

    @Override
    public void getDialog(String name, final AsyncCallback<Dialog> callback) {

        if(!name.startsWith("/")) name = ("/"+name);

        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, repoUrl+name);
        requestBuilder.setIncludeCredentials(true);
        requestBuilder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                if(200==response.getStatusCode())
                {
                    Dialog dialog = new DialogXML().unmarshall(response.getText());
                    callback.onSuccess(dialog);
                }
                else
                {
                    callback.onFailure(new RuntimeException("Failed to load dialog: HTTP "+response.getStatusCode()));
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                callback.onFailure(exception);
            }
        });
        try {
            Request req = requestBuilder.send();

        } catch (RequestException e) {
            callback.onFailure(new RuntimeException("Unknown error:"+ e.getMessage()));
        }

    }
}
