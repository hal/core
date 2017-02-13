package org.jboss.as.console.client.shared.subsys.infinispan.v3;

import org.jboss.as.console.client.shared.subsys.infinispan.v3.CacheStoreContext.State;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.widgets.wizard.Wizard;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;

import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NewCacheStoreWizard extends Wizard<CacheStoreContext, State> {
    private CachesPresenter presenter;
    private AddressTemplate address;
    private String name;

    public NewCacheStoreWizard(CachesPresenter presenter, AddressTemplate address, String name) {
        super("cache-store", new CacheStoreContext());
        this.presenter = presenter;
        this.address = address;
        this.name = name;

        final SecurityContext securityContext = presenter.getSecurityFramework()
                .getSecurityContext(presenter.getProxy().getNameToken());

        addStep(State.TYPE, new WizardStep<CacheStoreContext, State>(this, "Choose store type:") {

            @Override
            protected Widget asWidget(CacheStoreContext context) {
                String[] storeNames = new String[]{"binary-jdbc","custom","file","mixed-jdbc","remote","string-jdbc"};
                VerticalPanel panel = new VerticalPanel();
                for (String name : storeNames) {
                    RadioButton radioButton = new RadioButton("type", name);
                    radioButton.getElement().setId(name);
                    radioButton.setStyleName("choose_template");
                    radioButton.addClickHandler(event -> {
                        RadioButton button = (RadioButton) event.getSource();
                        context.setStoreName(button.getElement().getId());
                    });
                    panel.add(radioButton);
                }

                return panel;
            }
        });

        addStep(State.ATTRIBUTES, new WizardStep<CacheStoreContext, State>(this, "Store attributes") {

            private VerticalPanel panel;

            @Override
            protected Widget asWidget(CacheStoreContext context) {
                // only create the wrapper panel, the form depends on step one
                panel = new VerticalPanel();
                panel.setStyleName("fill-layout-width");
                return panel;
            }

            @Override
            protected void onShow(CacheStoreContext context) {
                panel.clear();
                ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                        .setAddress(address.getTemplate())
                        .setConfigOnly()
                        .setSecurityContext(securityContext)
                        .setResourceDescription(presenter.getDescriptionRegistry().lookup(address.append("store=" + context.getStoreName())))
                        .build();

                panel.add(formAssets.getHelp().asWidget());
                panel.add(formAssets.getForm().asWidget());
                formAssets.getForm().setEnabled(true);
                context.setFormAssets(formAssets);
            }
        });
    }

    @Override
    protected State back(State state) {
        return state == State.ATTRIBUTES ? State.TYPE : null;
    }

    @Override
    protected State next(State state) {
        return state == State.TYPE ? State.ATTRIBUTES : null;
    }

    @Override
    protected void finish() {
        super.finish();
        presenter.onCreate(address.append("store=" + context.getStoreName()), name, context.getFormAssets().getForm().getUpdatedEntity(), true);
    }
}
