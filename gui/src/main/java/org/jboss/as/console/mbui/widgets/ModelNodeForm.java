package org.jboss.as.console.mbui.widgets;

import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.AbstractForm;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.PlainFormView;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 11/12/12
 */
public class ModelNodeForm extends AbstractForm<ModelNode> {

    private final String address;
    private final SecurityContext securityContext;
    private ModelNode editedEntity = null;
    private Map<String, ModelNode> defaults = Collections.EMPTY_MAP;
    private boolean hasWritableAttributes;
    private final static ModelNode UNDEFINED = new ModelNode();

    public ModelNodeForm(String address, SecurityContext securityContext) {
        this.address = address;
        this.securityContext = securityContext;
    }

    @Override
    public void editTransient(ModelNode newBean) {
        isTransient = true;
        edit(newBean);
    }

    @Override
    public void edit(ModelNode bean) {

        // Needs to be declared (i.e. when creating new instances)
        if(null==bean)
            throw new IllegalArgumentException("Invalid entity: null");

        // the edit buttons becomes visible
        setOperational(true);

        this.editedEntity = bean;

        // prevent modification of the source
        // the DMR getter otherwise mutate the bean
        this.editedEntity.protect();

        // clear previous values
        clearItems();

        final Map<String, String> exprMap = getExpressions(editedEntity);

        //final List<ModelNode> filteredDMRNames = bean.hasDefined("_filtered-attributes") ?
        //        bean.get("_filtered-attributes").asList() : Collections.EMPTY_LIST;

        // visit form
        ModelNodeInspector inspector = new ModelNodeInspector(bean);
        inspector.accept(new ModelNodeVisitor()
        {

            private boolean isComplex = false;

            @Override
            public boolean visitValueProperty(
                    final String propertyName, final ModelNode value, final PropertyContext ctx) {

                if(isComplex ) return true; // skip complex types

                visitItem(propertyName, new FormItemVisitor() {

                    public void visit(FormItem item) {

                        item.resetMetaData();

                        // expressions
                        String exprValue = exprMap.get(propertyName);
                        if(exprValue!=null)
                        {
                            item.setUndefined(false);
                            item.setExpressionValue(exprValue);
                        }

                        // values
                        else if(value.isDefined()) {
                            item.setUndefined(false);
                            Object castedValue = downCast(value, (ModelNode)item.getMetadata());
                            item.setValue(castedValue);
                        }
                        else if(defaults.containsKey(propertyName))
                        {
                            item.setUndefined(false);
                            item.setValue(downCast(defaults.get(propertyName), (ModelNode) item.getMetadata()));
                        }
                        else
                        {
                            // when no value is given we still need to validate the input
                            item.setUndefined(true);
                            item.setModified(true); // don't escape validation
                        }

                        // RBAC: attribute level constraints

                        /*for(ModelNode att : filteredDMRNames)
                        {
                            if(att.asString().equals(propertyName))
                            {
                                item.setFiltered(true);
                                break;
                            }
                        } */
                    }
                });

                return true;
            }

            @Override
            public boolean visitReferenceProperty(String propertyName, ModelNode value, PropertyContext ctx) {
                isComplex = true;
                return true;
            }

            @Override
            public void endVisitReferenceProperty(String propertyName, ModelNode value, PropertyContext ctx) {
                isComplex = false;
            }

            @Override
            public boolean visitCollectionProperty(String propertyName, final ModelNode value, PropertyContext ctx) {
                visitItem(propertyName, new FormItemVisitor() {

                    public void visit(FormItem item) {

                        item.resetMetaData();

                        if(value!=null)
                        {
                            item.setUndefined(false);
                            //TODO: item.setValue(value.asList());
                            item.setValue(Collections.EMPTY_LIST);
                        }
                        else
                        {
                            item.setUndefined(true);
                            item.setModified(true); // don't escape validation
                        }
                    }
                });

                return true;
            }
        });

        // plain views
        refreshPlainView();
    }

    protected void refreshPlainView() {
        for(PlainFormView view : plainViews)
            view.refresh(getEditedEntity()!=null && getEditedEntity().isDefined());
    }

    /**
     * The MBUI kernel provides the context
     * @return
     */
    @Override
    protected SecurityContext getSecurityContext() {
        return this.securityContext;
    }

    @Override
    public Set<String> getReadOnlyNames() {

        Set<String> readOnly = new HashSet<String>();
        for(String item : getFormItemNames())
        {
            if(!securityContext.getAttributeWritePriviledge(item).isGranted())
                readOnly.add(item);
        }
        return readOnly;
    }

    @Override
    public Set<String> getFilteredNames() {
        Set<String> filtered = new HashSet<String>();
        for(String item : getFormItemNames())
        {
            boolean writePriv = securityContext.getAttributeWritePriviledge(item).isGranted();
            boolean readPriv = securityContext.getAttributeReadPriviledge(item).isGranted();
            if(!writePriv && !readPriv)
                filtered.add(item);
        }
        return filtered;
    }


    public static Object downCast(ModelNode value, ModelNode metadata)
    {
        Object result = null;

        if (!value.isDefined()) { // value.asXxx() throws IllegalArgumentException when value is undefined
            return FormItem.VALUE_SEMANTICS.UNDEFINED;
        }

        ModelType targetType = resolveTypeFromMetaData(metadata);
        switch (targetType)
        {
            case STRING:
                result = value.asString();
                break;
            case INT:
                result = value.asInt();
                break;
            case LONG:
                result = value.asLong();
                break;
            case BOOLEAN:
                result = value.asBoolean();
                break;
            case BIG_DECIMAL:
                result = value.asBigDecimal();
                break;
            case BIG_INTEGER:
                result = value.asBigInteger();
                break;
            case DOUBLE:
                result = value.asDouble();
                break;
            case LIST: {

                try {

                    List<ModelNode> items = value.asList();
                    List<String> list = new ArrayList<String>(items.size());
                    for (ModelNode item : items)
                        list.add(item.asString());
                    result = list;

                } catch (Throwable t) {
                    t.printStackTrace();
                    result = new ArrayList<>(); // syntax errors

                }
                break;
            }
            case PROPERTY: {  // it's actually interpreted as a property list, but that ttype doesn'ty really exist (yet)

                try {

                    List<Property> properties = value.asPropertyList();
                    Map<String, String> map = new HashMap<>();
                    for (Property item : properties)
                        map.put(item.getName(), item.getValue().asString());
                    result = map;

                } catch (Throwable t) {
                    t.printStackTrace();
                    result = new HashMap(); // syntax errors
                }
                break;
            }
            case UNDEFINED:
                break;
            default:
                throw new RuntimeException("Unexpected targetType "+targetType);

        }
        return result;
    }

    private static ModelType resolveTypeFromMetaData(ModelNode metadata) {

        if(!metadata.has("type"))
            throw new IllegalArgumentException("Illegal meta data:" + metadata.toString());

        ModelType result = null;
        ModelType type = ModelType.valueOf(metadata.get("type").asString());
        ModelType valueType = (metadata.has("value-type")
            && metadata.get("value-type").getType()!=ModelType.OBJECT) ?
                ModelType.valueOf(metadata.get("value-type").asString()) : null;

        switch (type) {
            case OBJECT:
                if(valueType!=null && ModelType.STRING == valueType)
                    result = ModelType.PROPERTY;  // we abuse this type, actually its a property list
                else if(null==valueType)
                    result = type;
                break;
            default:
                result = type; // ignore the value type setting
        }
        return result;
    }

    void visitItem(final String name, FormItemVisitor visitor) {
        String namePrefix = name + "_";
        for(Map<String, FormItem> groupItems : formItems.values())
        {
            for(String key : groupItems.keySet())
            {
                if(key.equals(name) || key.startsWith(namePrefix))
                {
                    visitor.visit(groupItems.get(key));
                }
            }
        }
    }

    private Map<String, String> getExpressions(ModelNode bean) {
        final Map<String, String> exprMap = new HashMap<String,String>();

        // parse expressions
        ModelNodeInspector inspector = new ModelNodeInspector(bean);
        inspector.accept(new ModelNodeVisitor()
        {
            @Override
            public boolean visitValueProperty(String propertyName, ModelNode value, PropertyContext ctx) {
                if(value.getType() == ModelType.EXPRESSION)
                {
                    exprMap.put(propertyName, value.asString());
                }
                return true;
            }
        });

        bean.setTag(EXPR_TAG, exprMap);

        return exprMap;
    }

    @Override
    public void cancel() {
        //clearValues();
        if(editedEntity!=null && editedEntity.isDefined()) edit(editedEntity);
    }

    @Override
    public Map<String, Object> getChangedValues() {

        final Map<String,Object> changedValues = new HashMap<String, Object>();
        final ModelNode src = editedEntity == null ? new ModelNode() : editedEntity;
        final ModelNode dest = getUpdatedEntity();

        ModelNodeInspector inspector = new ModelNodeInspector(this.getUpdatedEntity());
        inspector.accept(
                new ModelNodeVisitor()
                {
                    @Override
                    public boolean visitValueProperty(String propertyName, ModelNode value, PropertyContext ctx) {

                        // protected mode (see edit() ) requires us to prevent mutation
                        ModelNode modelNode = src.hasDefined(propertyName) ?  src.get(propertyName) : UNDEFINED;

                        if(!modelNode.equals(dest.get(propertyName))) {
                            Object castedValue = downCast(dest.get(propertyName), getAttributeMetaData(propertyName));
                            changedValues.put(propertyName, castedValue);
                        }

                        return true;
                    }
                }
        );

        Map<String, Object> finalDiff = new HashMap<String,Object>();

        // map changes, but skip unmodified fields
        for(Map<String, FormItem> groupItems : formItems.values())
        {
            for(FormItem item : groupItems.values())
            {
                Object val = changedValues.get(item.getName());

                // expression have precedence over real values
                if(item.isExpressionValue())
                {
                    finalDiff.put(item.getName(), item.asExpressionValue());
                }

                // regular values
                else if(val!=null && item.isModified())
                {
                    if(item.isUndefined())
                        finalDiff.put(item.getName(), FormItem.VALUE_SEMANTICS.UNDEFINED);
                    else
                        finalDiff.put(item.getName(), val);
                }
            }
        }

        return finalDiff;

    }

    @Override
    public ModelNode getUpdatedEntity() {

        final ModelNode updatedModel = getEditedEntity()==null ?
                new ModelNode() : getEditedEntity().clone();

        for(Map<String, FormItem> groupItems : formItems.values())
        {
            for(String key : groupItems.keySet())
            {
                visitItem(key, new FormItemVisitor() {
                    @Override
                    public void visit(FormItem item) {

                        ModelNode node = updatedModel.get(item.getName());
                        Object obj = item.getValue();
                        Class baseType = obj.getClass();

                        // UNDEFINED
                        if (obj.equals(FormItem.VALUE_SEMANTICS.UNDEFINED)) {
                            node.set(ModelType.UNDEFINED);
                        }

                        // STRING
                        else if (baseType == String.class) {
                            String stringValue = (String) obj;
                            if(stringValue.startsWith("$"))
                                node.setExpression(stringValue);
                            else if("".equals(stringValue))   // TODO better item.isUndefined() ?
                                node.clear(); // TODO: depends on nillable?
                            else
                                node.set(stringValue);
                        }

                        // Numeric Values
                        else if (baseType == Long.class) {
                            Long longValue = (Long) obj;
                            if(item.isUndefined())
                                node.clear();
                            else
                                node.set(longValue);
                        } else if (baseType == Integer.class) {
                            Integer intValue = (Integer) obj;
                            if(item.isUndefined())
                                node.clear();
                            else
                                node.set(intValue);
                        } else if (baseType == BigDecimal.class) {
                            BigDecimal bigValue = (BigDecimal) obj;
                            if(item.isUndefined())
                                node.clear();
                            else
                                node.set(bigValue);
                        } else if (baseType == Double.class) {
                            Double dValue = (Double) obj;
                            if(item.isUndefined())
                                node.clear();
                            else
                                node.set(dValue);
                        }

                        // BOOL
                        else if (baseType == Boolean.class) {
                            node.set((Boolean)obj);
                        }

                        // BYTE
                        else if (baseType == byte[].class) {
                            node.set((byte[]) obj);
                        }

                        // LIST
                        else if (baseType == ArrayList.class) {
                            node.clear();
                            List l = (List)obj;
                            for(Object o : l)
                                node.add(o.toString()); // TODO: type conversion ?

                        }

                        // MAP
                        else if (baseType == LinkedHashMap.class || baseType == HashMap.class) {
                            node.clear();
                            Map<String,String> m = (Map<String,String>)obj;
                            for(String k : m.keySet())
                                node.add(k, m.get(k));

                        }

                        else {
                            throw new IllegalArgumentException("Can not convert. This value is not of a recognized base type: " + obj.toString());
                        }
                    }
                });
            }
        }

        return updatedModel;
    }

    @Override
    public ModelNode getEditedEntity() {
        return editedEntity;
    }

    @Override
    public void clearValues() {

        editedEntity = null;

        clearItems();

        setOperational(false);
        if(toolsCallback !=null)
            setEnabled(false);
        refreshPlainView();
    }

    private void clearItems() {
        for(Map<String, FormItem> groupItems : formItems.values())
        {
            for(String key : groupItems.keySet())
            {
                visitItem(key, new FormItemVisitor() {
                    @Override
                    public void visit(FormItem item) {
                        item.clearValue();
                    }
                });
            }
        }
    }

    public void setDefaults(Map<String, ModelNode> defaults) {
        this.defaults = defaults;
    }

    public boolean hasWritableAttributes() {
        return hasWritableAttributes;
    }

    public ModelNode getAttributeMetaData(String name) {

        ModelNode metaData = null;

        for(Map<String, FormItem> groupItems : formItems.values()) {
            for (FormItem item : groupItems.values()) {
                if(name.equals(item.getName()))
                {
                    metaData = (ModelNode) item.getMetadata();
                    break;
                }
            }
        }

        return metaData;
    }

    interface FormItemVisitor {
        void visit(FormItem item);
    }


    // ---- deprecated, blow up -----

    @Override
    public Class<?> getConversionType() {
        throw new RuntimeException("API Incompatible: getConversionType() not supported on "+getClass().getName());
    }

    public void setHasWritableAttributes(boolean hasWritableAttributes) {
        this.hasWritableAttributes = hasWritableAttributes;
    }


}

