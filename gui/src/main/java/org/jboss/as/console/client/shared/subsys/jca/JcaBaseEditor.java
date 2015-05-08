package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;

import java.util.Map;

import static org.jboss.as.console.client.shared.subsys.jca.JcaPresenter.*;

/**
 * @author Heiko Braun
 * @date 11/29/11
 */
public class JcaBaseEditor {

    private final JcaPresenter presenter;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityContext securityContext;

    private ModelNodeFormBuilder.FormAssets archiveForm;
    private ModelNodeFormBuilder.FormAssets validationForm;
    private ModelNodeFormBuilder.FormAssets connectionManagerForm;


    public JcaBaseEditor(final JcaPresenter presenter, final ResourceDescriptionRegistry descriptionRegistry,
            final SecurityContext securityContext) {
        this.presenter = presenter;
        this.descriptionRegistry = descriptionRegistry;
        this.securityContext = securityContext;
    }

    Widget asWidget() {

        // archive validation
        ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(descriptionRegistry.lookup(ARCHIVE_VALIDATION_ADDRESS))
                .setSecurityContext(securityContext);
        archiveForm = formBuilder.build();
        archiveForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                presenter.onSaveArchiveSettings(changeset);
            }

            @Override
            public void onCancel(final Object o) {
                archiveForm.getForm().cancel();
            }
        });
        VerticalPanel archivePanel = new VerticalPanel();
        archivePanel.setStyleName("fill-layout-width");
        archivePanel.add(archiveForm.getHelp().asWidget());
        archivePanel.add(archiveForm.getForm().asWidget());

        // bean validation
        formBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(descriptionRegistry.lookup(BEAN_VALIDATION_ADDRESS))
                .setSecurityContext(securityContext);
        validationForm = formBuilder.build();
        validationForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                presenter.onSaveBeanSettings(changeset);
            }

            @Override
            public void onCancel(final Object o) {
                validationForm.getForm().cancel();
            }
        });
        VerticalPanel validationPanel = new VerticalPanel();
        validationPanel.setStyleName("fill-layout-width");
        validationPanel.add(validationForm.getHelp().asWidget());
        validationPanel.add(validationForm.getForm().asWidget());

        // cached connection manager
        formBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include("debug", "error", "ignore-unknown-connections")
                .setResourceDescription(descriptionRegistry.lookup(CACHED_CONNECTION_MANAGER_ADDRESS))
                .setSecurityContext(securityContext);
        connectionManagerForm = formBuilder.build();
        connectionManagerForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changedValues) {
                presenter.onSaveCCMSettings(changedValues);
            }

            @Override
            public void onCancel(final Object o) {
                connectionManagerForm.getForm().cancel();
            }
        });
        VerticalPanel ccmPanel = new VerticalPanel();
        ccmPanel.setStyleName("fill-layout-width");
        ccmPanel.add(connectionManagerForm.getHelp().asWidget());
        ccmPanel.add(connectionManagerForm.getForm().asWidget());

        return new OneToOneLayout()
                .setPlain(true)
                .setTitle("JCA")
                .setHeadline("JCA Subsystem")
                .setDescription(Console.CONSTANTS.subsys_jca_common_config_desc())
                .setMaster("", new HTML())
                .addDetail("Cached Connection Manager", ccmPanel)
                .addDetail("Archive Validation", archivePanel)
                .addDetail("Bean Validation", validationPanel)
                .build();
    }

    public void setBeanSettings(ModelNode jcaBeanValidation) {
        validationForm.getForm().edit(jcaBeanValidation);
    }

    public void setArchiveSettings(ModelNode jcaArchiveValidation) {
        archiveForm.getForm().edit(jcaArchiveValidation);
    }

    public void setCCMSettings(ModelNode jcaConnectionManager) {
        connectionManagerForm.getForm().edit(jcaConnectionManager);
    }
}
