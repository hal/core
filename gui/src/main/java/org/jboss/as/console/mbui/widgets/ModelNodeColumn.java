package org.jboss.as.console.mbui.widgets;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @since 24/07/14
 */
public class ModelNodeColumn extends Column<ModelNode, String> {

    private String attributeName;
    private ValueAdapter adapter;

    public ModelNodeColumn(String attributeName) {
        super(new TextCell());
        this.attributeName = attributeName;
    }

    public ModelNodeColumn(ValueAdapter adapter) {
        super(new TextCell());
        this.adapter = adapter;
    }

    @Override
    public String getValue(ModelNode model) {
        return adapter!=null ? adapter.getValue(model) : model.get(attributeName).asString();
    }

    public interface ValueAdapter {
        String getValue(ModelNode model);
    }
}
