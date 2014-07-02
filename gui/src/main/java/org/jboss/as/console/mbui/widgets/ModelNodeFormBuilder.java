package org.jboss.as.console.mbui.widgets;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 02/07/14
 */
public class ModelNodeFormBuilder {

    private ModelNodeForm form;
    private SecurityContext securityContext;
    private String address;
    private ModelNode modelDescription;
    private String[] attributeNames;
    private SafeHtml help;

    private boolean runtimeAttributes = true;
    private boolean configAttributes = true;

    public ModelNodeFormBuilder setSecurityContext(SecurityContext sc) {
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

    public ModelNodeFormBuilder setFields(String... attributeName) {
        this.attributeNames = attributeName;
        return this;
    }

    public FormAssets build()
    {

        this.form = new ModelNodeForm(this.address, this.securityContext);
        this.form.setNumColumns(2);
        this.form.setEnabled(false);

        assert modelDescription.hasDefined("attributes") : "Invalid model description. Expected child 'attributes'";

        List<Property> attributeDescriptions = modelDescription.get("attributes").asPropertyList();


        // catch-all directive, if no explicit attributes given
        if(null == attributeNames)
        {
            attributeNames = new String[attributeDescriptions.size()];
            int i=0;
            for(Property attr : attributeDescriptions)
            {
                attributeNames[i] = attr.getName();
                i++;
            }
        }


        List<FormItem> items = new ArrayList<FormItem>(attributeNames.length);

        SafeHtmlBuilder helpTexts = new SafeHtmlBuilder();
        helpTexts.appendHtmlConstant("<table class='help-attribute-descriptions'>");

        for (String attribute : attributeNames)
        {
            for(Property attr : attributeDescriptions)
            {

                boolean isRuntime = attr.getValue().get("storage").asString().equals("runtime");
                boolean isConfig = !attr.getValue().get("storage").asString().equals("runtime"); // TODO: verify statement

                if(runtimeAttributes == false && isRuntime)
                {
                    continue;
                }

                if(configAttributes == false && isConfig)
                {
                    continue;
                }

                if(!attr.getName().equals(attribute))
                    continue;


                char[] stringArray = attr.getName().toCharArray();
                stringArray[0] = Character.toUpperCase(stringArray[0]);

                String label = new String(stringArray).replace("-", " ");
                ModelNode attrValue = attr.getValue();

                // help
                helpTexts.appendHtmlConstant("<tr class='help-field-row'>");
                helpTexts.appendHtmlConstant("<td class='help-field-name'>");
                helpTexts.appendEscaped(label).appendEscaped(": ");
                helpTexts.appendHtmlConstant("</td>");
                helpTexts.appendHtmlConstant("<td class='help-field-desc'>");
                try {
                    String descWorkaround = attrValue.get("description").asString();

                    helpTexts.appendHtmlConstant(descWorkaround.equals("null") ? "n/a" : descWorkaround);
                } catch (Throwable e) {
                    // ignore parse errors
                    helpTexts.appendHtmlConstant("<i>Failed to parse description</i>");
                }
                helpTexts.appendHtmlConstant("</td>");
                helpTexts.appendHtmlConstant("</tr>");

                boolean required = !attr.getValue().get("nillable").asBoolean();
                ModelType type = ModelType.valueOf(attrValue.get("type").asString());
                //System.out.println(attr.getName()+">"+type);
                switch(type)
                {
                    case BOOLEAN:
                        CheckBoxItem checkBoxItem = new CheckBoxItem(attr.getName(), label);
                        items.add(checkBoxItem);
                        break;
                    case DOUBLE:
                        NumberBoxItem num = new NumberBoxItem(attr.getName(), label);
                        num.setRequired(required);
                        items.add(num);
                        break;
                    case LONG:
                        NumberBoxItem num2 = new NumberBoxItem(attr.getName(), label);
                        num2.setRequired(required);
                        items.add(num2);
                        break;
                    case INT:
                        NumberBoxItem num3 = new NumberBoxItem(attr.getName(), label);
                        num3.setRequired(required);
                        items.add(num3);
                        break;
                    case STRING:
                        if(attrValue.get("allowed").isDefined())
                        {
                            List<ModelNode> allowed = attrValue.get("allowed").asList();
                            Set<String> allowedValues = new HashSet<String>(allowed.size());
                            for(ModelNode value : allowed)
                                allowedValues.add(value.asString());

                            ComboBoxItem combo = new ComboBoxItem(attr.getName(), label);
                            combo.setValueMap(allowedValues);
                        }
                        else
                        {
                            TextBoxItem tb = new TextBoxItem(attr.getName(), label);
                            tb.setRequired(required);
                            items.add(tb);
                        }
                        break;
                    default:
                        Log.debug("Unsupported ModelType " + type);
                }

            }
        }

        form.setFields(items.toArray(new FormItem[]{}));

        return new FormAssets(form, helpTexts.toSafeHtml());
    }

    public final class FormAssets {
        private ModelNodeForm form;
        private SafeHtml help;

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
    }
}
