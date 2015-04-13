package org.jboss.as.console.mbui.widgets;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 02/09/14
 */
public abstract class ModelDrivenWidget extends LazyPanel {

    private static final String ACCESS_CONTROL = "access-control";
    private static final String TRIM_DESCRIPTIONS = "trim-descriptions";

    private final String addressTemplate;
    private final ResourceAddress address;
    private ResourceDefinition definition;
    private HTML errorWidget = null;

    public ModelDrivenWidget(String address, StatementContext statementContext) {
        this.addressTemplate = address;
        this.address = new ResourceAddress(addressTemplate, statementContext);
        init();
    }

    public ModelDrivenWidget(String address) {
        this(address, Console.MODULES.getCoreGUIContext());
    }

    private void init() {

        // load data
        ModelNode op = address.clone();
        op.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        op.get(OPERATIONS).set(true);

        Console.MODULES.getDispatchAsync().execute(
                new DMRAction(op), new SimpleCallback<DMRResponse>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        Console.error("Failed to load resource definition for "+address);
                        errorWidget = new HTML("<pre class='rhs-content-panel'>Failed to load resource definition for "+address+":\n"+caught.getMessage()+"</pre>");
                        ensureWidget();
                    }

                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {


                        ModelNode response = dmrResponse.get();

                        if(response.isFailure())
                        {
                            Console.error("Failed to load resource definition for "+address, response.getFailureDescription());
                            errorWidget = new HTML("<pre class='rhs-content-panel'>Failed to load resource definition for "+address+":\n"+response.getFailureDescription()+"</pre>");
                            ensureWidget();
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
                        ModelDrivenWidget.this.definition = new ResourceDefinition(description);
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
        return errorWidget!=null ? errorWidget : buildWidget(address, definition);
    }

    /**
     * Callback to construct the actual widget once the resource definition is loaded.
     *
     * @param address
     * @param definition
     * @return the actual widget to be embedded
     */
    public abstract Widget buildWidget(ResourceAddress address, ResourceDefinition definition);

    public boolean isInitialised() {
        return getWidget()!=null;
    }

    public String getAddressTemplate() {
        return addressTemplate;
    }


}
