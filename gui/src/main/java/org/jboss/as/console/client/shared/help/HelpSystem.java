package org.jboss.as.console.client.shared.help;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.shared.Preferences;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;
import org.jboss.ballroom.client.widgets.forms.FormAdapter;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 6/8/11
 */
public class HelpSystem {

    private DispatchAsync dispatcher;
    private ApplicationMetaData propertyMetaData;

    class Lookup
    {
        String detypedName;
        String javaName;

        Lookup(String detypedName, String javaName) {
            this.detypedName = detypedName;
            this.javaName = javaName;
        }

        public String getDetypedName() {
            return detypedName;
        }

        public String getJavaName() {
            return javaName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Lookup)) return false;

            Lookup lookup = (Lookup) o;

            if (!detypedName.equals(lookup.detypedName)) return false;
            if (!javaName.equals(lookup.javaName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = detypedName.hashCode();
            result = 31 * result + javaName.hashCode();
            return result;
        }
    }
    @Inject
    public HelpSystem(DispatchAsync dispatcher, ApplicationMetaData propertyMetaData) {
        this.dispatcher = dispatcher;
        this.propertyMetaData = propertyMetaData;
    }

    public void getAttributeDescriptions(
            ModelNode resourceAddress,
            final FormAdapter form,
            final AsyncCallback<List<FieldDesc>> callback)
    {


        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(ADDRESS).set(resourceAddress);
        operation.get(RECURSIVE).set(true);
        operation.get(LOCALE).set(getLocale());

        // build field name list

        List<String> formItemNames = form.getFormItemNames();
        BeanMetaData beanMetaData = propertyMetaData.getBeanMetaData(form.getConversionType());
        List<PropertyBinding> bindings = beanMetaData.getProperties();
        final LinkedList<Lookup> fieldNames = new LinkedList<Lookup>();


        for(String name : formItemNames)
        {

            for(PropertyBinding binding : bindings)
            {
                if(!binding.isKey() && binding.getJavaName().equals(name)) {
                    String[] splitDetypedNames = binding.getDetypedName().split("/");
                    // last one in the path is the attribute name
                    Lookup lookup = new Lookup(splitDetypedNames[splitDetypedNames.length - 1], binding.getJavaName());
                    if(!fieldNames.contains(lookup))
                        fieldNames.add(lookup);
                }
            }
        }

        dispatcher.execute(new DMRAction(operation), new DescriptionsCallback(fieldNames, callback));
    }

    public interface AddressCallback
    {
        ModelNode getAddress();
    }

    private String getLocale() {
        String locale = Preferences.get(Preferences.Key.LOCALE) != null ?
                Preferences.get(Preferences.Key.LOCALE) : "en";
        return locale;

    }
    public void getMetricDescriptions(
            AddressCallback address,
            Column[] columns,
            final AsyncCallback<List<FieldDesc>> callback)
    {

        final List<Lookup> attributeNames = new LinkedList<Lookup>();
        for(Column c : columns)
            attributeNames.add(new Lookup(c.getDeytpedName(), c.getLabel()));

        final ModelNode operation = address.getAddress();
        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(LOCALE).set(getLocale());

        dispatcher.execute(new DMRAction(operation), new DescriptionsCallback(attributeNames, callback));
    }

    private class DescriptionsCallback implements AsyncCallback<DMRResponse> {

        private List<Lookup> fieldNames;
        private AsyncCallback<List<FieldDesc>> callback;

        public DescriptionsCallback(List<Lookup> fieldNames, AsyncCallback<List<FieldDesc>> callback) {
            this.fieldNames = fieldNames;
            this.callback = callback;
        }

        @Override
        public void onSuccess(DMRResponse result) {
            ModelNode response = result.get();


            if(response.isFailure())
            {
                Log.debug(response.toString());
                onFailure(new Exception(response.getFailureDescription()));
            }
            else
            {
                LinkedList<FieldDesc> fields = new LinkedList<FieldDesc>();

                ModelNode payload = response.get(RESULT);

                ModelNode descriptionModel = null;
                if(ModelType.LIST.equals(payload.getType()))
                    descriptionModel = payload.asList().get(0);
                else
                    descriptionModel = payload;


                matchSubElements(descriptionModel, fieldNames, fields);

                callback.onSuccess(getOrderedFields(fieldNames, fields));
            }

        }

        @Override
        public void onFailure(Throwable caught) {
            callback.onFailure(caught);
        }
    }

    private static void matchSubElements(ModelNode descriptionModel, List<Lookup> fieldNames, LinkedList<FieldDesc> fields) {

        if (descriptionModel.hasDefined(RESULT))
            descriptionModel = descriptionModel.get(RESULT).asObject();

        try {

            // match attributes
            if(descriptionModel.hasDefined(ATTRIBUTES))
            {
                List<Property> elements = descriptionModel.get(ATTRIBUTES).asPropertyList();

                for(Lookup lookup : fieldNames)
                {

                    for(Property element : elements)
                    {
                        String childName = element.getName();
                        ModelNode value = element.getValue();

                        if(lookup.getDetypedName().equals(childName))
                        {
                            FieldDesc desc = new FieldDesc(lookup.getJavaName(), value.get("description").asString());
                            if(value.hasDefined("expressions-allowed"))
                            {
                                desc.setSupportExpressions(value.get("expressions-allowed").asBoolean());
                            }
                            if(!fields.contains(desc))
                                fields.add(desc);
                        }

                    }
                }


            }

            if(fieldNames.isEmpty())
                return;

            // visit child elements
            if (descriptionModel.hasDefined("children")) {
                //List<Property> children = descriptionModel.get("children").asPropertyList();
                ModelNode childrenModel = descriptionModel.get(CHILDREN);
                Set<String> children = childrenModel.keys();
                for(String child : children )
                {
                    ModelNode childDesc = childrenModel.get(child);
                    ModelNode desc = childDesc.get(MODEL_DESCRIPTION);

                    if(desc.isDefined()) // TOOD: How can this happen?
                    {
                        for (Property modDescProp : desc.asPropertyList()) {

                            matchSubElements(childDesc.get(MODEL_DESCRIPTION, modDescProp.getName()), fieldNames, fields);

                            // exit early
                            if(fieldNames.isEmpty())
                                return;

                        }
                    }
                }
            }


        } catch (IllegalArgumentException e) {
            Log.error("Failed to read help descriptionModel", e);
        }
    }

    /**
     * returns the fields in the requested order
     */
    private List<FieldDesc> getOrderedFields(List<Lookup> fieldNames, List<FieldDesc> fields) {
        List<FieldDesc> orderedFields = new LinkedList<>();
        for (Lookup fieldName : fieldNames) {
            for (FieldDesc field : fields) {
                if (fieldName.getJavaName().equals(field.getRef()))
                    orderedFields.add(field);
            }
        }

        return orderedFields;
    }
}
