package org.jboss.as.console.client.shared.subsys.elytron.ui;

import java.util.Map;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.wizard.Wizard;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.shared.subsys.elytron.ui.NewPolicyWizard.State.CUSTOM_POLICY;
import static org.jboss.as.console.client.shared.subsys.elytron.ui.NewPolicyWizard.State.JACC_POLICY;

class NewPolicyWizard extends Wizard<NewPolicyWizard.Context, NewPolicyWizard.State> {

    @FunctionalInterface
    interface FinishCallback {

        void onFinish(boolean customPolicy, ModelNode payload);
    }

    static class Context {

        boolean customPolicy = true;
        ModelNode payload;
    }


    enum State {CHOOSE, CUSTOM_POLICY, JACC_POLICY}


    static class ChooseStep extends WizardStep<Context, State> {

        ChooseStep(Wizard<Context, State> wizard) {
            super(wizard, Console.CONSTANTS.pleaseChoose());
        }

        @Override
        protected Widget asWidget(Context context) {
            VerticalPanel body = new VerticalPanel();
            RadioButton customButton = new RadioButton("policy", "Custom Policy");
            customButton.getElement().setId("custom");
            customButton.setStyleName("choose_template");
            customButton.setValue(true);
            customButton.addClickHandler(event -> context.customPolicy = true);
            customButton.setFocus(true);
            body.add(customButton);

            RadioButton jaccButton = new RadioButton("policy", "JACC Policy");
            jaccButton.getElement().setId("jacc");
            jaccButton.setStyleName("choose_template");
            jaccButton.addClickHandler(event -> context.customPolicy = false);
            body.add(jaccButton);

            return body;
        }
    }


    static class PolicyStep extends WizardStep<Context, State> {

        private final ModelNodeFormBuilder.FormAssets formAssets;

        PolicyStep(Wizard<Context, State> wizard, String title, String[] attributes,
                ResourceDescription resourceDescription, SecurityContext securityContext) {
            super(wizard, title);
            formAssets = new ModelNodeFormBuilder()
                    .setResourceDescription(resourceDescription)
                    .setSecurityContext(securityContext)
                    .include(attributes)
                    .unsorted()
                    .build();
        }

        @Override
        protected Widget asWidget(Context context) {
            FlowPanel layout = new FlowPanel();
            Widget formWidget = formAssets.getForm().asWidget();
            formWidget.getElement().getStyle().setBackgroundColor("yellow");
            layout.add(formAssets.getHelp().asWidget());
            layout.add(formWidget);
            return layout;
        }

        @Override
        protected void onShow(Context context) {
            formAssets.getForm().setEnabled(true);
            formAssets.getForm().editTransient(new ModelNode());
        }

        @Override
        protected boolean onNext(Context context) {
            FormValidation validation = formAssets.getForm().validate();
            if (!validation.hasErrors()) {
                context.payload = new ModelNode();
                Map<String, Object> changedValues = formAssets.getForm().getChangedValues();
                for (Map.Entry<String, Object> entry : changedValues.entrySet()) {
                    context.payload.get(entry.getKey()).set(String.valueOf(entry.getValue()));
                }
                return true;
            }
            return false;
        }
    }


    private final ResourceDescription policyDescription;
    private final SecurityContext securityContext;
    private final FinishCallback callback;

    NewPolicyWizard(ResourceDescription policyDescription, SecurityContext securityContext,
            FinishCallback callback) {
        super("new_elytron_policy", new Context());
        this.policyDescription = policyDescription;
        this.securityContext = securityContext;
        this.callback = callback;

        addStep(State.CHOOSE, new ChooseStep(this));
        addStep(State.CUSTOM_POLICY, new PolicyStep(this, "Custom Policy",
                new String[]{"name", "class-name", "module"},
                complexAttributeDescription(policyDescription, "custom-policy"),
                securityContext));
        addStep(State.JACC_POLICY, new PolicyStep(this, "JACC Policy",
                new String[]{"name", "policy", "configuration-factory", "module"},
                complexAttributeDescription(policyDescription, "jacc-policy"),
                securityContext));
    }

    @Override
    protected State back(State state) {
        return state == State.CHOOSE ? null : State.CHOOSE;
    }

    @Override
    protected State next(State state) {
        switch (state) {
            case CHOOSE:
                if (context.customPolicy) {
                    return CUSTOM_POLICY;
                } else {
                    return JACC_POLICY;
                }
            case CUSTOM_POLICY:
            case JACC_POLICY:
                return null;
            default:
                return null;
        }
    }

    @Override
    protected void finish() {
        super.finish();
        this.callback.onFinish(context.customPolicy, context.payload);
    }

    private ResourceDescription complexAttributeDescription(ResourceDescription resourceDescription, String name) {
        ModelNode payload = new ModelNode();

        ModelNode attribute = resourceDescription.get("attributes").get(name);
        payload.get("description").set(attribute.get("description"));
        ModelNode nameDescription = new ModelNode();
        nameDescription.get("type").set("STRING");
        nameDescription.get("description").set("The name of the policy.");
        nameDescription.get("required").set(true);
        payload.get("attributes").get("name").set(nameDescription);
        for (Property property : attribute.get("value-type").asPropertyList()) {
            payload.get("attributes").get(property.getName()).set(property.getValue());
        }
        return new ResourceDescription(payload);
    }
}
