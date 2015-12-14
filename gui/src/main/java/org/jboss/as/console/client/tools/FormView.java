package org.jboss.as.console.client.tools;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.apache.html.dom.HTMLBuilder;
import org.jboss.as.console.client.shared.util.LRUCache;
import org.jboss.as.console.mbui.widgets.AddressUtils;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 6/15/12
 */
public class FormView {

    private BrowserPresenter presenter;
    private ModelNode currentAddress;
    private VerticalPanel formContainer;
    private LRUCache<String, ModelNodeFormBuilder.FormAssets> widgetCache = new LRUCache<String, ModelNodeFormBuilder.FormAssets>(10);

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        formContainer = new VerticalPanel();
        formContainer.setStyleName("fill-layout-width");

        layout.add(formContainer);

        return layout;
    }

    public void display(ModelNode address, ModelNode description, SecurityContext securityContext, Property model)
    {
        formContainer.clear();
        currentAddress = address;


        String cacheKey = AddressUtils.asKey(address, true);
        ModelNodeFormBuilder.FormAssets formAssets = null;
        if(widgetCache.containsKey(cacheKey))
        {
            // retrieve from cache
            formAssets = widgetCache.get(cacheKey);
            formAssets.getForm().clearValues();
        }
        else {

            // construct new form (expensive)
            ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                    .setResourceDescription(description)
                    .setSecurityContext(securityContext);

            formAssets = builder.build();

            // cache it
            widgetCache.put(cacheKey, formAssets);
        }

        formAssets.getForm().setEnabled(false);

        List<Property> tuples = address.asPropertyList();
        boolean isPlaceholder = tuples.isEmpty() ? false : tuples.get(tuples.size()-1).getValue().asString().equals("*");

        // some resources only provide runtime attributes, hence the form might be empty
        if(!isPlaceholder && formAssets.getForm().getFormItemNames().size()>0) {
            // only provide tools when writable attributes are present
            if (formAssets.getForm().hasWritableAttributes())
                formAssets.getForm().setToolsCallback(new AddressableFormCallback(address));


            formContainer.add(formAssets.getForm());

            formAssets.getForm().edit(model.getValue());
        }
        else {
            formContainer.add(new HTML("No configurable attributes available."));
        }

    }

    public void clearDisplay()
    {
        currentAddress = null;
        formContainer.clear();
    }

    public void setPresenter(BrowserPresenter presenter) {
        this.presenter = presenter;
    }

    class AddressableFormCallback implements FormCallback<ModelNode> {

        private ModelNode address;

        AddressableFormCallback(ModelNode address) {
            this.address = address;
        }

        @Override
        public void onSave(Map<String, Object> changeset) {
            if(changeset.size()>0)
                presenter.onSaveResource(address, changeset);
        }

        @Override
        public void onCancel(ModelNode entity) {
            presenter.readResource(address, false);
        }
    }
}