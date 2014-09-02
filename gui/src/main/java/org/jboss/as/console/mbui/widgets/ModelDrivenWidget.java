package org.jboss.as.console.mbui.widgets;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefiniton;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 02/09/14
 */
public abstract class ModelDrivenWidget extends LazyPanel {

    private static final String ACCESS_CONTROL = "access-control";
    private static final String TRIM_DESCRIPTIONS = "trim-descriptions";

    private ResourceAddress address;
    private ResourceDefiniton definition;

    public ModelDrivenWidget(String address) {
        this.address = new ResourceAddress(address, Console.MODULES.getCoreGUIContext());
        init();
    }

    public ModelDrivenWidget(ResourceAddress address) {
        this.address = address;
        init();
    }

    private void init() {

        // load data
        ModelNode op = address.clone();
        op.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        op.get(OPERATIONS).set(true);

        Console.MODULES.getDispatchAsync().execute(
                new DMRAction(op), new SimpleCallback<DMRResponse>() {
                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {


                        ModelNode response = dmrResponse.get();

                        if(response.isFailure())
                        {
                            Console.error("Failed to load resource definition for "+address, response.getFailureDescription());
                            setWidget(new HTML("Failed to load resource definition for "+address));
                            return;
                        }

                        ModelNode result = response.get(RESULT);
                        ModelNode description = null;

                        // it's a List response when asking for '<resourceType>=*"
                        if(result.getType() == ModelType.LIST)
                        {
                            List<ModelNode> nodes = result.asList();

                            // TODO: exactly match and verify address (this is an assumption)
                            description = nodes.get(0).get(RESULT);

                            if(description == null)
                            {
                                //System.out.println(ref.address+" -> "+result);
                                throw new RuntimeException("Unexpected response format");
                            }

                        }
                        else
                        {
                            description = result;
                        }

                        // capture payload and construct actual widget
                        ModelDrivenWidget.this.definition = new ResourceDefiniton(description);
                        ensureWidget();
                    }
                }
        );
    }

    @Override
    public void setVisible(boolean visible) {
        if(getWidget()!=null)
            getWidget().setVisible(visible);
    }

    @Override
    protected Widget createWidget() {
        return buildWidget(address, definition);
    }

    /**
     * Callback to construct the actual widget once the resource definition is loaded.
     *
     * @param address
     * @param definition
     * @return the actual widget to be embedded
     */
    public abstract Widget buildWidget(ResourceAddress address, ResourceDefiniton definition);

    public boolean isInitialised() {
        return getWidget()!=null;
    }

}
