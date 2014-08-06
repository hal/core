package org.jboss.as.console.client.tools.mapping;

import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 7/16/12
 */
public class DescriptionMapper {

    private ModelNode description;
    private ModelNode address;

    private final static Set<String> DEFAULT_OPS = new HashSet<String>();

    static {
        DEFAULT_OPS.add("whoami");
        DEFAULT_OPS.add("read-children-names");
        DEFAULT_OPS.add("read-operation-description");
        DEFAULT_OPS.add("read-operation-names");
        DEFAULT_OPS.add("read-children-types");
        DEFAULT_OPS.add("read-children-resources");
        DEFAULT_OPS.add("read-resource-description");
        DEFAULT_OPS.add("read-resource");
        DEFAULT_OPS.add("read-resource-description");
        DEFAULT_OPS.add("read-attribute");
        DEFAULT_OPS.add("write-attribute");
        DEFAULT_OPS.add("undefine-attribute");
    }

    public DescriptionMapper(ModelNode address, ModelNode description) {
        this.address = address;
        this.description = description;
    }

    public interface Mapping {
        void onAttribute(String name, String description, String type, boolean required, boolean expressions, boolean runtime, boolean readOnly, String deprecationReason);
        void onOperation(String name, String description, List<RequestParameter> parameter, ResponseParameter response, boolean isDefault);

        void onChild(String name, String description);
        void onBegin(int numAttributes, int numOperations);

        void onFinish();
    }

    public void map(Mapping mapping) {

        final List<Property> attributes = description.get("attributes").asPropertyList();
        final List<Property> operations = description.get("operations").asPropertyList();

        mapping.onBegin(attributes.size(), operations.size());

        // ---

        Collections.sort(attributes, new Comparator<Property>() {
            @Override
            public int compare(Property property, Property property1) {
                return property.getName().compareTo(property1.getName());
            }
        });

        if(!attributes.isEmpty())
        {

            for(Property att : attributes)
            {
                final String name = att.getName();
                final ModelNode attrValue = att.getValue();

                final String description = attrValue.get("description").asString();
                final String type = attrValue.get("type").asString();

                final boolean required = attrValue.hasDefined("required") ?
                        attrValue.get("required").asBoolean() : false;

                final boolean nillable = attrValue.hasDefined("nillable") ?
                        attrValue.get("nillable").asBoolean() : true;


                final boolean expressions = attrValue.hasDefined("expressions-allowed") ?
                        attrValue.get("expressions-allowed").asBoolean() : false;

                final boolean runtime = attrValue.hasDefined("storage") ?
                        attrValue.get("storage").asString().equals("runtime"): false;

                final boolean readOnly = attrValue.hasDefined("access-type") ?
                        attrValue.get("access-type").asString().equals("read-only"): false;

                final String deprecationReason = attrValue.hasDefined("deprecated") ?
                        attrValue.get("deprecated").get("reason").asString() : null;

                mapping.onAttribute(name, description, type, (!nillable||required), expressions,runtime, readOnly, deprecationReason);
            }

        }

        // -----------------


        Collections.sort(operations, new Comparator<Property>() {
            @Override
            public int compare(Property property, Property property1) {
                return property.getName().compareTo(property1.getName());
            }
        });

        if(!operations.isEmpty())
        {

            for(Property op : operations)
            {
                final String opName = op.getName();
                final String opDesc = op.getValue().get("description").asString();

                boolean isDefaultOp = DEFAULT_OPS.contains(opName);

                List<RequestParameter> parameters = new LinkedList<RequestParameter>();
                ResponseParameter response = null;

                // parameters
                if(op.getValue().hasDefined("request-properties"))
                {
                    for(Property param : op.getValue().get("request-properties").asPropertyList())
                    {
                        final ModelNode value = param.getValue();
                        final String paramDesc = value.get("description").asString();
                        final String paramName = param.getName();
                        final String paramType = value.get("type").asString();
                        boolean required = false;
                        if(value.hasDefined("required"))
                        {
                            required = value.get("required").asBoolean();
                        }

                        parameters.add(
                                new RequestParameter(
                                        paramDesc, paramName, paramType, required
                                )
                        );
                    }
                }

                // response
                if(op.getValue().hasDefined("reply-properties"))
                {
                    final ModelNode reply = op.getValue().get("reply-properties");
                    final String replyDesc = reply.get("description").isDefined() ? reply.get("description").asString() : "";
                    final String replyType = reply.get("type").isDefined() ? reply.get("type").asString() : "";

                    response = new ResponseParameter(replyDesc, replyType);
                }
                else
                {
                    response = new ResponseParameter("", "");
                }


                // sort order
                Collections.sort(parameters, new Comparator<RequestParameter>() {
                    @Override
                    public int compare(RequestParameter requestParameter, RequestParameter requestParameter1) {
                        return requestParameter.getParamName().compareTo(requestParameter1.getParamName());
                    }
                });

                mapping.onOperation(opName, opDesc, parameters, response, isDefaultOp);
            }

        }


        if(description.hasDefined("children"))
        {
            final List<Property> children = description.get("children").asPropertyList();

            if(!children.isEmpty())
            {

                for(Property child : children)
                {
                    final String childName = child.getName();
                    final String childDesc = child.getValue().get("description").asString();
                    mapping.onChild(childName, childDesc);
                }

            }
        }



        mapping.onFinish();
    }
}
