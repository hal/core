package org.jboss.as.console.client.shared.subsys.infinispan.v3;

import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;

public class CacheStoreContext {
    private String storeName;
    private ModelNodeFormBuilder.FormAssets formAssets;

    public static enum State {
        TYPE, ATTRIBUTES
    }

    public CacheStoreContext() {
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public ModelNodeFormBuilder.FormAssets getFormAssets() {
        return formAssets;
    }

    public void setFormAssets(ModelNodeFormBuilder.FormAssets formAssets) {
        this.formAssets = formAssets;
    }
}
