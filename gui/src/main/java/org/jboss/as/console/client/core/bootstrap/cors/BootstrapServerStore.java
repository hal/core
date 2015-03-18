package org.jboss.as.console.client.core.bootstrap.cors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.storage.client.StorageMap;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import org.jboss.as.console.client.shared.BeanFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Store for the management servers which uses the local storage of the browser.
 *
 * @author Harald Pehl
 */
public class BootstrapServerStore {
    private static final String KEY = "org.jboss.as.console.bootstrap.servers";
    private static final String SELECTED = "org.jboss.as.console.bootstrap.selected";

    private final BeanFactory factory;
    private final Storage storage;

    public BootstrapServerStore() {
        factory = GWT.create(BeanFactory.class);
        storage = Storage.getLocalStorageIfSupported();
    }

    public List<BootstrapServer> load() {
        List<BootstrapServer> servers = new ArrayList<BootstrapServer>();
        if (storage != null) {
            //noinspection MismatchedQueryAndUpdateOfCollection
            StorageMap storageMap = new StorageMap(storage);
            if (storageMap.containsKey(KEY)) {
                String json = storageMap.get(KEY);
                if (json != null) {
                    JSONValue jsonValue = JSONParser.parseStrict(json);
                    if (jsonValue != null) {
                        JSONArray jsonArray = jsonValue.isArray();
                        if (jsonArray != null) {
                            for (int i = 0; i < jsonArray.size(); i++) {
                                AutoBean<BootstrapServer> bean = AutoBeanCodex
                                        .decode(factory, BootstrapServer.class, jsonArray.get(i).toString());
                                servers.add(bean.as());
                            }
                        }
                    }
                }
            }
        }
        return servers;
    }

    public List<BootstrapServer> add(BootstrapServer newServer) {
        List<BootstrapServer> servers = new ArrayList<BootstrapServer>();
        if (storage != null) {
            servers.addAll(load());
            servers.add(newServer);
            storage.setItem(KEY, toJson(servers));
        }
        return servers;
    }

    public List<BootstrapServer> remove(BootstrapServer removeServer) {
        List<BootstrapServer> servers = new ArrayList<BootstrapServer>();
        if (storage != null) {
            servers.addAll(load());
            for (Iterator<BootstrapServer> iterator = servers.iterator(); iterator.hasNext(); ) {
                BootstrapServer server = iterator.next();
                if (server.getName().equals(removeServer.getName())) {
                    iterator.remove();
                }
            }
            storage.setItem(KEY, toJson(servers));
        }
        return servers;
    }

    public BootstrapServer get(String name) {
        List<BootstrapServer> servers = load();
        for (BootstrapServer server : servers) {
            if (name.equals(server.getName())) {
                return server;
            }
        }
        return null;
    }

    public void storeSelection(BootstrapServer server) {
        if (storage != null) {
            storage.setItem(SELECTED, toJson(server));
        }
    }

    public BootstrapServer restoreSelection() {
        if (storage != null) {
            //noinspection MismatchedQueryAndUpdateOfCollection
            StorageMap storageMap = new StorageMap(storage);
            if (storageMap.containsKey(KEY)) {
                String json = storageMap.get(SELECTED);
                if (json != null) {
                    JSONValue jsonValue = JSONParser.parseStrict(json);
                    if (jsonValue != null) {
                        JSONObject jsonObject = jsonValue.isObject();
                        if (jsonObject != null) {
                            AutoBean<BootstrapServer> bean = AutoBeanCodex
                                    .decode(factory, BootstrapServer.class, jsonObject.toString());
                            return bean.as();
                        }
                    }
                }
            }
        }
        return null;
    }

    private String toJson(BootstrapServer server) {
        AutoBean<BootstrapServer> bean = AutoBeanUtils.getAutoBean(server);
        return AutoBeanCodex.encode(bean).getPayload();
    }

    private String toJson(List<BootstrapServer> servers) {
        StringBuilder json = new StringBuilder("[");
        for (Iterator<BootstrapServer> iterator = servers.iterator(); iterator.hasNext(); ) {
            BootstrapServer server = iterator.next();
            AutoBean<BootstrapServer> bean = AutoBeanUtils.getAutoBean(server);
            json.append(AutoBeanCodex.encode(bean).getPayload());
            if (iterator.hasNext()) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }
}
