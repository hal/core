package org.jboss.as.console.client.v3.behaviour;

import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

public class ModelNodeAdapter {

    private static final String UNDEFINE_ATTRIBUTE = "undefine-attribute";

    /**
     * Turns a change set into a composite write attribute operation.
     *
     * @param resourceAddress the address
     * @param changeSet       the changed attributes
     * @return composite operation
     */
    public ModelNode fromChangeSet(ResourceAddress resourceAddress, Map<String, Object> changeSet) {

        ModelNode define = new ModelNode();
        define.get(ADDRESS).set(resourceAddress);
        define.get(OP).set(WRITE_ATTRIBUTE_OPERATION);

        ModelNode undefine = new ModelNode();
        undefine.get(ADDRESS).set(resourceAddress);
        undefine.get(OP).set(UNDEFINE_ATTRIBUTE);

        ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();

        List<ModelNode> steps = new ArrayList<>();
        for (String key : changeSet.keySet()) {
            Object value = changeSet.get(key);

            ModelNode step;
            if (value.equals(FormItem.VALUE_SEMANTICS.UNDEFINED)) {
                step = undefine.clone();
                step.get(NAME).set(key);
            } else {
                step = define.clone();
                step.get(NAME).set(key);

                // set value, including type conversion
                ModelNode valueNode = step.get(VALUE);
                setValue(valueNode, value);
            }
            steps.add(step);
        }

        operation.get(STEPS).set(steps);
        return operation;
    }

    private void setValue(ModelNode nodeToSetValueUpon, Object value) {
        Class type = value.getClass();

        if (FormItem.VALUE_SEMANTICS.class == type) {
            // skip undefined form item values (FormItem.UNDEFINED.Value)
            // or persist as UNDEFINED
            if (value.equals(FormItem.VALUE_SEMANTICS.UNDEFINED)) {
                nodeToSetValueUpon.set(ModelType.UNDEFINED);
            }

        } else if (String.class == type) {
            String stringValue = (String) value;
            if (stringValue.startsWith("$")) {
                // TODO: further constraints
                nodeToSetValueUpon.setExpression(stringValue);
            } else {
                nodeToSetValueUpon.set(stringValue);
            }
        } else if (Boolean.class == type) {
            nodeToSetValueUpon.set((Boolean) value);
        } else if (Integer.class == type) {
            nodeToSetValueUpon.set((Integer) value);
        } else if (Double.class == type) {
            nodeToSetValueUpon.set((Double) value);
        } else if (Long.class == type) {
            nodeToSetValueUpon.set((Long) value);
        } else if (Float.class == type) {
            nodeToSetValueUpon.set((Float) value);
        } else if (ArrayList.class == type) {
            nodeToSetValueUpon.clear();
            List l = (List) value;
            for (Object o : l)
                nodeToSetValueUpon.add(o.toString()); // TODO: type conversion?
        } else if (HashMap.class == type) {
            nodeToSetValueUpon.clear();
            //noinspection unchecked
            Map<String, String> map = (Map<String, String>) value;
            for (String k : map.keySet())
                nodeToSetValueUpon.add(k, map.get(k));
        } else {
            throw new RuntimeException("Unsupported type: " + type);
        }
    }
}
