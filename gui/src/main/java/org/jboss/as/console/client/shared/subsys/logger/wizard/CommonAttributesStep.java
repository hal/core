package org.jboss.as.console.client.shared.subsys.logger.wizard;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @since 08/09/15
 */
public class CommonAttributesStep extends WizardStep<HandlerContext, HandlerSteps>{

    private final TwoStepWizard wizard;
    private ModelNodeForm form;

    public CommonAttributesStep(TwoStepWizard wizard, String title) {
        super(wizard, title);
        this.wizard = wizard;
    }

    @Override
    public Widget asWidget() {
        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setCreateMode(true)
                .setResourceDescription(wizard.getResourceDescription())
                .setRequiredOnly(true)
                .setSecurityContext(wizard.getSecurityContext());


        ModelNodeFormBuilder.FormAssets assets = builder.build();
        form = assets.getForm();
        form.setEnabled(true);

        if (form.hasWritableAttributes()) {


            FlowPanel layout = new FlowPanel();

            Widget formWidget = form.asWidget();
            ModelNode opDescription = wizard.getResourceDescription().get("operations").get("add").get("description");
            ContentDescription text = new ContentDescription(opDescription.asString());
            layout.add(text);
            layout.add(assets.getHelp().asWidget());
            layout.add(formWidget);

            return layout;

        } else {
            return new HTML(Console.CONSTANTS.noConfigurableAttributes());
        }
    }

    @Override
    protected boolean onNext(HandlerContext context) {
        FormValidation validation = form.validate();
        if (!validation.hasErrors()) {
            context.setAttributes(form.getUpdatedEntity());
            return true;
        }
        return false;
    }
}
