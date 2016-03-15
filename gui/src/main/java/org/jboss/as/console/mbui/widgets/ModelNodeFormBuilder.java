package org.jboss.as.console.mbui.widgets;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.DisclosureGroupRenderer;
import org.jboss.ballroom.client.widgets.forms.DoubleFormItem;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.ListItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.PropertyListItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * @author Heiko Braun
 * @date 02/07/14
 */
public class ModelNodeFormBuilder {

    private ModelNodeForm form;
    private SecurityContext securityContext;
    private String address;
    private ModelNode modelDescription;
    private Set<String> includes = new LinkedHashSet<>();
    private Set<String> excludes = new HashSet<>();
    private SafeHtml help;

    private boolean runtimeAttributes = true;
    private boolean configAttributes = true;
    private boolean requiredOnly;
    private boolean createMode;
    private boolean unsorted = false;
    private boolean includeOptionals = true; // only important if createMode == true

    private Map<String, FormItemFactory> itemFactories = new HashMap<>();

    public interface FormItemFactory {

        FormItem create(Property attributeDescription);
    }

    public ModelNodeFormBuilder setSecurityContext(SecurityContext sc) {
        if(null==sc)
            throw new IllegalArgumentException("SecurityContext cannot be null!");
        this.securityContext = sc;
        return this;
    }

    public  ModelNodeFormBuilder setAddress(String addr) {
        this.address = addr;
        return this;
    }

    public ModelNodeFormBuilder setResourceDescription(ModelNode resourceDescription) {
        this.modelDescription = resourceDescription;
        return this;
    }

    public ModelNodeFormBuilder setRuntimeOnly() {
        this.configAttributes = false;
        this.runtimeAttributes= true;
        return this;
    }

    public ModelNodeFormBuilder setConfigOnly() {
        this.configAttributes = true;
        this.runtimeAttributes = false;
        return this;
    }

    public ModelNodeFormBuilder unsorted() {
        this.unsorted = true;
        return this;
    }

    public ModelNodeFormBuilder addFactory(String attributeName, FormItemFactory factory) {
        itemFactories.put(attributeName, factory);
        return this;
    }

    public ModelNodeFormBuilder include(String... attributeName) {
        if (attributeName != null && attributeName.length !=  0) {
            this.includes.addAll(asList(attributeName));
        }
        return this;
    }

    public ModelNodeFormBuilder include(String[]... attributes) {
        if (attributes != null && attributes.length !=  0) {

            for (String[] group : attributes) {
                this.includes.addAll(asList(group));
            }

        }
        return this;
    }

    public ModelNodeFormBuilder includeOptionals(boolean includeOptionals) {
        this.includeOptionals = includeOptionals;
        return this;
    }

    public ModelNodeFormBuilder exclude(String... attributeName) {
        if (attributeName != null && attributeName.length !=  0) {
            this.excludes.addAll(asList(attributeName));
        }
        return this;
    }

    public ModelNodeFormBuilder exclude(String[]... attributes) {
        if (attributes != null && attributes.length !=  0) {

            for (String[] group : attributes) {
                this.excludes.addAll(asList(group));
            }

        }
        return this;
    }

    public FormAssets build() {

        // pre-requisite
        if (createMode && !modelDescription.hasDefined("operations")) {
            throw new IllegalStateException("Operation descriptions not defined");
        }


        this.form = new ModelNodeForm(this.address, this.securityContext);
        this.form.setNumColumns(2);
        this.form.setEnabled(false);

        assert modelDescription.hasDefined("attributes") : "Invalid model description. Expected child 'attributes'";


        List<Property> attributeDescriptions = new ArrayList<Property>();
        if (createMode && modelDescription.get("operations").get("add").hasDefined("request-properties")) {
            attributeDescriptions = modelDescription.get("operations").get("add").get("request-properties").asPropertyList();
        } else if (!createMode) {
            attributeDescriptions = modelDescription.get("attributes").asPropertyList();
        }

        // sort fields
        if (!unsorted) {
            Collections.sort(attributeDescriptions, new Comparator<Property>() {
                @Override
                public int compare(Property property, Property property1) {
                    return property.getName().compareTo(property1.getName());
                }
            });
        }

        // catch-all directive, if no explicit attributes given
        if (includes.isEmpty()) {
            for (Property attr : attributeDescriptions) {
                includes.add(attr.getName());
            }
        }
        // in any case remove attributes marked for exclusion
        includes.removeAll(excludes);


        LinkedList<FormItem> requiredItems = new LinkedList<FormItem>();
        LinkedList<FormItem> optionalItems = new LinkedList<FormItem>();

        SafeHtmlBuilder helpTexts = new SafeHtmlBuilder();
        helpTexts.appendHtmlConstant("<table class='help-attribute-descriptions'>");

        Map<String, ModelNode> defaultValues = new HashMap<String, ModelNode>();
        int numWritable = 0;

        boolean hasRequired = false;

        // for some decision below we need to know wether or not required attributes exist at all
        if (requiredOnly) {
            for (Property attr : attributeDescriptions) {
                ModelNode value = attr.getValue();
                boolean required = isRequired(value);
                boolean readOnly = value.get("access-type").asString().equals("read-only");
                if (required & !readOnly) {
                    hasRequired = true;
                    break;

                }
            }
        }

        Set<String[]> unsupportedTypes = new HashSet<>();
        for (String attribute : includes) {
            for (Property attr : attributeDescriptions) {

                boolean isRuntime = attr.getValue().get("storage").asString().equals("runtime");
                boolean isConfig = !attr.getValue().get("storage").asString().equals("runtime"); // TODO: verify statement

                if (runtimeAttributes == false && isRuntime) {
                    continue;
                }

                if (configAttributes == false && isConfig) {
                    continue;
                }

                if (!attr.getName().equals(attribute))
                    continue;

                // -------
                // Attribute meta data

                // name
                char[] attrName = attr.getName().toCharArray();
                attrName[0] = Character.toUpperCase(attrName[0]);

                // field label
                String label = new String(attrName).replace("-", " ");
                ModelNode attrDesc = attr.getValue();

                // skip deprecated attributes
                if(attrDesc.hasDefined("deprecated"))
                {
                    //Log.error("Skip deprecated attribute '" + attr.getName() + "'");
                    continue;
                }

                // type
                ModelType type = ModelType.valueOf(attrDesc.get("type").asString().toUpperCase());

                // default value
                if (attrDesc.hasDefined("default")) {
                    ModelNode defaultValue = attrDesc.get("default");
                    ModelNode value = new ModelNode();
                    //value.set(type, ModelNodeForm.downCast(defaultValue));
                    setValue(value, type, ModelNodeForm.downCast(defaultValue, attrDesc)); // workaround for numeric types


                    defaultValues.put(attr.getName(), value);
                }


                // read-only
                final boolean readOnly = attrDesc.hasDefined("access-type") ?
                        attrDesc.get("access-type").asString().equals("read-only") : false;

                // nillable
                boolean isRequired = isRequired(attrDesc);

                // createMode flag
                if ((createMode && readOnly))
                    continue;

                // requiredOnly flag
                if (requiredOnly && hasRequired && !isRequired)
                    continue;


                // count writable attributes
                if (!readOnly && !isRuntime) numWritable++;

                // -------
                // help

                helpTexts.appendHtmlConstant("<tr class='help-field-row'>");
                helpTexts.appendHtmlConstant("<td class='help-field-name'>");
                helpTexts.appendEscaped(label).appendEscaped(": ");
                helpTexts.appendHtmlConstant("</td>");
                helpTexts.appendHtmlConstant("<td class='help-field-desc'>");
                try {
                    String descWorkaround = attrDesc.get("description").asString();

                    helpTexts.appendHtmlConstant(descWorkaround.equals("null") ? "n/a" : descWorkaround);
                } catch (Throwable e) {
                    // ignore parse errors
                    helpTexts.appendHtmlConstant("<i>Failed to parse description</i>");
                }
                helpTexts.appendHtmlConstant("</td>");
                helpTexts.appendHtmlConstant("</tr>");

                FormItem formItem = null;

                // exopliciyly created form items (monkey patching)
                if(itemFactories.containsKey(attr.getName()))
                {
                    formItem = itemFactories.get(attr.getName()).create(attr);
                }

                // not created by explicit factory
                if(null==formItem) {
                    switch (type) {
                        case BOOLEAN:
                            formItem = new CheckBoxItem(attr.getName(), label);
                            formItem.setRequired(isRequired);
                            formItem.setEnabled(!readOnly && !isRuntime);
                            break;
                        case DOUBLE:
                            formItem = new DoubleFormItem(attr.getName(), label);
                            formItem.setRequired(isRequired);
                            formItem.setEnabled(!readOnly && !isRuntime);
                            break;
                        case LONG:
                            boolean allowNegativeValues = false;
                            if (attrDesc.hasDefined("default"))
                                allowNegativeValues = attrDesc.get("default").asLong() < 0;

                            formItem = new NumberBoxItem(attr.getName(), label, allowNegativeValues);
                            formItem.setRequired(isRequired);
                            formItem.setEnabled(!readOnly && !isRuntime);
                            break;
                        case BIG_DECIMAL:
                            formItem = new NumberBoxItem(attr.getName(), label);
                            formItem.setRequired(isRequired);
                            formItem.setEnabled(!readOnly && !isRuntime);
                            break;
                        case INT:
                            if(attrDesc.hasDefined("min") && attrDesc.hasDefined("max"))
                            {
                                formItem = new NumberBoxItem(
                                        attr.getName(), label,
                                        attrDesc.get("min").asLong(),
                                        attrDesc.get("max").asLong()
                                );
                            }
                            else
                            {
                                formItem = new NumberBoxItem(attr.getName(), label);
                            }

                            formItem.setRequired(isRequired);
                            formItem.setEnabled(!readOnly && !isRuntime);


                            break;
                        case LIST:
                            formItem = new ListItem(attr.getName(), label);
                            formItem.setRequired(isRequired);
                            formItem.setEnabled(!readOnly && !isRuntime);
                            break;
                        case STRING:
                            if (attrDesc.get("allowed").isDefined()) {
                                List<ModelNode> allowed = attrDesc.get("allowed").asList();
                                Set<String> allowedValues = new HashSet<String>(allowed.size());
                                for (ModelNode value : allowed)
                                    allowedValues.add(value.asString());

                                final boolean isNillable = attrDesc.hasDefined(ModelDescriptionConstants.NILLABLE) && attrDesc.get(ModelDescriptionConstants.NILLABLE).asBoolean();
                                ComboBoxItem combo = new ComboBoxItem(attr.getName(), label, isNillable);
                                combo.setValueMap(allowedValues);
                                combo.setEnabled(!readOnly && !isRuntime);
                                combo.setRequired(isRequired);

                                formItem = combo;
                            } else {
                                TextBoxItem textBoxItem = new TextBoxItem(attr.getName(), label);
                                textBoxItem.setAllowWhiteSpace(true);

                                textBoxItem.setRequired(isRequired);
                                textBoxItem.setEnabled(!readOnly && !isRuntime);

                                formItem = textBoxItem;
                            }

                            // TODO: Support for TextAreaItem

                            break;
                        case OBJECT:
                            if (attrDesc.has("value-type") && attrDesc.get("value-type").asString().equals("STRING")) {
                                PropertyListItem propList = new PropertyListItem(attr.getName(), label);
                                propList.setRequired(isRequired);
                                propList.setEnabled(!readOnly && !isRuntime);

                                formItem = propList;
                                break;
                            }
                        default: {
                            unsupportedTypes.add(new String[]{attr.getName(), type.toString()});
                            Log.error("Unsupported ModelType " + type + ", attribute '" + attr.getName() + "'");
                        }
                    }
                }

                if (formItem != null) {
                    if (createMode) {
                        if (isRequired && includeOptionals)
                            requiredItems.add(formItem);
                        else
                            optionalItems.add(formItem);
                    } else {
                        requiredItems.add(formItem);
                    }


                    // attribute meta data attached to form item
                    formItem.setMetadata(attrDesc);
                }

            }
        }

        // some resources already contain a name attribute

        FormItem nameItem = null;
        if(createMode) {
            for (FormItem item : requiredItems) {
                if ("name".equals(item.getName())) {
                    nameItem = item;
                    break;
                }
            }
            for (FormItem item : optionalItems) {
                if ("name".equals(item.getName())) {
                    nameItem = item;
                    break;
                }
            }
        }

        // remove so it can be prepended
        if(nameItem!=null)
        {
            requiredItems.remove(nameItem);
            optionalItems.remove(nameItem);
        }

        // distinguish required and optional fields (createMode)
        if (requiredItems.isEmpty()) {
            // no required fields explicitly given, treat all fields as required
            if (createMode) {
                optionalItems.addFirst(new TextBoxItem("name", "Name", true));
                numWritable++;
            }
            form.setFields(optionalItems.toArray(new FormItem[]{}));
        } else {
            if (createMode) {
                requiredItems.addFirst(new TextBoxItem("name", "Name", true));
                numWritable++;
            }

            form.setFields(requiredItems.toArray(new FormItem[]{}));

            if (optionalItems.size() > 0)
                form.setFieldsInGroup("Optional Fields", new DisclosureGroupRenderer(), optionalItems.toArray(new FormItem[]{}));
        }

        // form meta data
        form.setDefaults(defaultValues);
        form.setHasWritableAttributes(numWritable > 0);

        FormAssets formAssets = new FormAssets(form, helpTexts.toSafeHtml());
        formAssets.setUnsupportedTypes(unsupportedTypes);
        return formAssets;
    }

    private boolean isRequired(ModelNode attributeDescription) {
        boolean required = attributeDescription.hasDefined("nillable") && !attributeDescription.get("nillable")
                .asBoolean();
        if (attributeDescription.hasDefined("alternatives") &&
                !attributeDescription.get("alternatives").asList().isEmpty()) {
            required = false;
        }
        return required;
    }

    public ModelNodeFormBuilder setRequiredOnly(boolean requiredOnly) {
        this.requiredOnly = requiredOnly;
        return this;
    }

    /**
     * In create mode we consider the paramter for the 'add' operations for building the form.
     *
     * @param createMode
     * @return
     */
    public ModelNodeFormBuilder setCreateMode(boolean createMode) {
        this.createMode = createMode;

        return this;
    }

    public final class FormAssets {
        private ModelNodeForm form;
        private SafeHtml help;
        private Set<String[]> unsupportedTypes = Collections.EMPTY_SET;

        public FormAssets(ModelNodeForm form, SafeHtml help) {
            this.form = form;
            this.help = help;
        }

        public ModelNodeForm getForm() {
            return form;
        }

        public StaticHelpPanel getHelp() {
            return new StaticHelpPanel(help);
        }

        public void setUnsupportedTypes(Set<String[]> unsupportedTypes) {
            this.unsupportedTypes = unsupportedTypes;
        }

        public Set<String[]> getUnsupportedTypes() {
            return unsupportedTypes;
        }

        public Widget asWidget() {

            VerticalPanel formPanel = new VerticalPanel();
            formPanel.setStyleName("fill-layout-width");
            formPanel.add(getHelp().asWidget());
            formPanel.add(getForm().asWidget());
            return formPanel;
        }
    }

    /**
     * a more lenient way to update values by type
     */
    public static void setValue(ModelNode target, ModelType type, Object propValue ) {

        if(type.equals(ModelType.STRING))
        {
            target.set((String)propValue);
        }
        else if(type.equals(ModelType.INT))
        {
            target.set((Integer)propValue);
        }
        else if(type.equals(ModelType.DOUBLE))
        {
            target.set((Double)propValue);
        }
        else if(type.equals(ModelType.LONG))
        {
            // in some cases the server returns the wrong model type for numeric values
            // i.e the description affords a ModelType.LONG, but a ModelType.INTEGER is returned
            try {
                target.set((Long)propValue);
            } catch (Throwable e) { // ClassCastException
                target.set(Integer.valueOf((Integer)propValue));
            }
        }
        else if(type.equals(ModelType.BIG_DECIMAL))
        {
            // in some cases the server returns the wrong model type for numeric values
            // i.e the description affords a ModelType.LONG, but a ModelType.INTEGER is returned
            try {
                target.set((BigDecimal)propValue);
            } catch (Throwable e) { // ClassCastException
                target.set(Double.valueOf((Double)propValue));
            }
        }
        else if(type.equals(ModelType.BOOLEAN))
        {
            target.set((Boolean)propValue);
        }
        else if(type.equals(ModelType.LIST))
        {
            target.setEmptyList();
            List list = (List)propValue;

            for(Object item : list)
                target.add(String.valueOf(item));
        }
        else
        {
            Log.warn("Type conversionnot supported for "+ type);
            target.setEmptyObject();
        }

    }

}
