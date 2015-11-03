package org.jboss.as.console.client.shared.subsys.logger.wizard;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.widgets.forms.FormValidation;

/**
 * @author Heiko Braun
 * @since 08/09/15
 */
public class FileAttributeStep extends WizardStep<HandlerContext, HandlerSteps> {

    private final TwoStepWizard wizard;
    private ModelNodeFormBuilder.FormAssets formAssets;

    public FileAttributeStep(TwoStepWizard wizard, String title) {
        super(wizard, title);
        this.wizard = wizard;
    }

    @Override
    public Widget asWidget() {
        ComplexAttributeForm fileAttributeForm = new ComplexAttributeForm(
                "file",
                wizard.getSecurityContext(),
                wizard.getResourceDescription()
        );
        formAssets = fileAttributeForm.build();

        FlowPanel layout = new FlowPanel();

        ContentDescription text = new ContentDescription("The filesystem path for the log file. 'jboss.server.log.dir' is a common 'relative-to' attribute value.");
        layout.add(text);
        layout.add(formAssets.asWidget());
        formAssets.getForm().setEnabled(true);

        return layout;
    }

    @Override
    protected boolean onNext(HandlerContext context) {

        ModelNodeForm form = formAssets.getForm();
        FormValidation validate = form.validate();
        if(!validate.hasErrors())
        {
            context.setFileAttribute(form.getUpdatedEntity());
            return true;
        }
        return false;
    }

    @Override
    protected boolean onBack(HandlerContext context) {
        formAssets.getForm().clearValues();
        return true;
    }
}
