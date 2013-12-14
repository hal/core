package org.jboss.as.console.client.tools.modelling.workbench.repository.vfs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 10/11/13
 */
public class Vfs {

    private static final String PUT = "PUT";
    private static final String DELETE = "PUT";
    private static final String METHOD_OVERRIDE = "X-HTTP-Method-Override";
    private static final String JSON = "application/json";
    private static final String ACCEPT = "Accept";
    private static final String BASE_URL = GWT.getHostPageBaseURL() + "repo";

    public Vfs() {

    }

    public void listEntries(AsyncCallback<List<Entry>> callback) {
        listEntries(Entry.ROOT, callback);
    }

    public void listEntries(Entry dir, final AsyncCallback<List<Entry>> callback) {
        final String url = BASE_URL + dir.getName();

        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        requestBuilder.setHeader(ACCEPT, JSON);

        try {
            requestBuilder.setCallback(new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {

                    if(200==response.getStatusCode())
                    {
                        List<Entry> entries = new LinkedList<Entry>();
                        JSONValue jso = JSONParser.parseLenient(response.getText());
                        JSONArray items = jso.isArray();
                        for(int i=0; i< items.size(); i++)
                        {
                            JSONObject item = items.get(i).isObject();
                            entries.add(
                                    new Entry(
                                            Entry.Type.valueOf(item.get("type").isString().stringValue().toUpperCase()),
                                            item.get("name").isString().stringValue(),
                                            item.get("link").isString().stringValue()
                                    )
                            );

                        }

                        callback.onSuccess(entries);
                    }
                    else
                    {
                        callback.onFailure(
                                new RuntimeException("Request failed "+ url + ", status="+response.getStatusCode()+":" +response.getStatusText())
                        );
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Console.error("Failed: "+exception.getMessage());
                }
            });

            requestBuilder.send();

        } catch (RequestException e) {
            throw new RuntimeException("VFS Error: "+ e.getMessage());
        }

    }

    public void save(final Entry entry, String  contents, final AsyncCallback<Boolean> callback) {

        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, entry.getLink());
        requestBuilder.setHeader(METHOD_OVERRIDE, PUT);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {

                if (200 == response.getStatusCode()) {

                    callback.onSuccess(true);
                } else {
                    callback.onFailure(
                            new RuntimeException("Request failed " + entry.getLink() + ", status=" + response.getStatusCode() + ":" + response.getStatusText())
                    );
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Console.error("Failed: " + exception.getMessage());
            }
        };


        try {
            requestBuilder.sendRequest(contents, requestCallback);
        } catch (RequestException e) {
            throw new RuntimeException("VFS Error: "+ e.getMessage());

        }

    }

    public void load(final Entry entry, final AsyncCallback<String> callback) {

        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, entry.getLink());
        requestBuilder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {

                if(200==response.getStatusCode())
                {

                    callback.onSuccess(response.getText());
                }
                else
                {
                    callback.onFailure(
                            new RuntimeException("Request failed "+ entry.getLink()+ ", status="+response.getStatusCode()+":" +response.getStatusText())
                    );
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Console.error("Failed: "+exception.getMessage());
            }
        });

        try {
            requestBuilder.send();
        } catch (RequestException e) {
            throw new RuntimeException("VFS Error: "+ e.getMessage());

        }

    }

    public void create(Entry entry){

    }

    public void delete(Entry entry){

    }

    private RequestBuilder createRequestBuilder(RequestBuilder.Method method) {
        return new RequestBuilder(method, BASE_URL);
    }
}
