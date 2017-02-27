package org.jboss.as.console.client.shared.subsys.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceAlternativesFormValidation;
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceFormValidation;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.console.client.shared.subsys.mail.MailPresenter.MAIL_SMTP_SERVER_TEMPLATE;
import static org.jboss.dmr.client.ModelDescriptionConstants.CREDENTIAL_REFERENCE;

/**
 * @author Heiko Braun
 * @date 2/14/12
 */
public class ServerConfigView {

    private HTML headline;
    private String description;
    private MailPresenter presenter;
    private ListDataProvider<MailServerDefinition> dataProvider;
    private DefaultCellTable<MailServerDefinition> table;
    private MailSession session;
    private SingleSelectionModel<MailServerDefinition> selectionModel;
    private ModelNodeFormBuilder.FormAssets credentialReferenceFormAsset;
    private ModelNodeFormBuilder.FormAssets mailFormAsset;

    public ServerConfigView(String description, MailPresenter presenter) {
        this.description = description;
        this.presenter = presenter;
    }

    Widget asWidget() {

        ProvidesKey<MailServerDefinition> providesKey = MailServerDefinition::getType;
        table = new DefaultCellTable<>(3, providesKey);

        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);

        selectionModel = new SingleSelectionModel<>(providesKey);
        table.setSelectionModel(selectionModel);

        TextColumn<MailServerDefinition> nameColumn = new TextColumn<MailServerDefinition>() {
            @Override
            public String getValue(MailServerDefinition record) {
                return record.getType().name().toUpperCase();

            }
        };

        table.addColumn(nameColumn, "Type");

        ToolStrip tableTools = new ToolStrip();
        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.launchNewServerWizard(session));

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_remove(),
                event -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle(Console.CONSTANTS.common_label_item()),
                        Console.MESSAGES.deleteConfirm(Console.CONSTANTS.common_label_item()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onRemoveServer(session.getName(), selectionModel.getSelectedObject());
                            }
                        }));

        tableTools.addToolButtonRight(addBtn);
        tableTools.addToolButtonRight(removeBtn);

        SecurityFramework securityFramework = Console.MODULES.getSecurityFramework();
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription resourceDescription = presenter.getResourceDescriptionRegistry().lookup(
                MAIL_SMTP_SERVER_TEMPLATE);

        mailFormAsset = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext)
                .createValidators(true)
                .exclude(CREDENTIAL_REFERENCE)
                .addFactory("outbound-socket-binding-ref",
                        attributeDescription -> new SuggestionResource("outbound-socket-binding-ref",
                                "Outbound Socket Binding Ref", true,
                                Console.MODULES.getCapabilities().lookup(NETWORK_OUTBOUND_SOCKET_BINDING))
                                .buildFormItem())
                .addFactory("password", attributeDescription -> new PasswordBoxItem("password", "Password", false))
                .build();

        mailFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                presenter.onSaveServer(session.getName(), selectionModel.getSelectedObject().getType(), changeset);
            }

            @Override
            public void onCancel(final Object entity) {
                mailFormAsset.getForm().cancel();
            }
        });


        // credential-reference attribute
        credentialReferenceFormAsset = new ComplexAttributeForm(CREDENTIAL_REFERENCE,
                securityContext, resourceDescription).build();
        credentialReferenceFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                ModelNode updatedEntity = credentialReferenceFormAsset.getForm().getUpdatedEntity();
                presenter.onSaveComplexAttribute(session.getName(), selectionModel.getSelectedObject().getType(),
                        updatedEntity);
            }

            @Override
            public void onCancel(final Object entity) {
                credentialReferenceFormAsset.getForm().cancel();
            }
        });
        credentialReferenceFormAsset.getForm().addFormValidator(new CredentialReferenceFormValidation());

        selectionModel.addSelectionChangeHandler(event -> {
            MailServerDefinition mailDefinition = selectionModel.getSelectedObject();
            mailFormAsset.getForm().edit(presenter.getServerAdapter().fromEntity(mailDefinition));
            if (mailDefinition.getCredentialReference() != null) {
                ModelNode bean = presenter.getCredentialReferenceAdapter()
                        .fromEntity(mailDefinition.getCredentialReference());
                credentialReferenceFormAsset.getForm().edit(bean);
            } else {
                // if there is no credential-reference in the model, an empty one allows for edit operation.
                credentialReferenceFormAsset.getForm().edit(new ModelNode());
            }
        });

        // cross validate the forms, as there are "alternatives" metadata for the password.
        mailFormAsset.getForm().addFormValidator(
                new CredentialReferenceAlternativesFormValidation("password", credentialReferenceFormAsset.getForm(),
                        "Credential Reference", true));
        credentialReferenceFormAsset.getForm().addFormValidator(
                new CredentialReferenceAlternativesFormValidation("password", mailFormAsset.getForm(), "Attributes",
                        false));


        headline = new HTML();
        headline.setStyleName("content-header-label");

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setTitle("Mail Session")
                .setHeadlineWidget(headline)
                .setDescription(description)
                .setMaster(Console.MESSAGES.available("Mail Server"), table)
                .setMasterTools(tableTools)
                .addDetail(Console.CONSTANTS.common_label_attributes(), mailFormAsset.asWidget())
                .addDetail("Credential Reference", credentialReferenceFormAsset.asWidget());


        return layout.build();
    }

    public void updateFrom(MailSession session) {
        this.session = session;
        headline.setText("Mail Session: " + session.getName());


        List<MailServerDefinition> server = new ArrayList<>();
        if (session.getImapServer() != null) {
            server.add(session.getImapServer());
        }

        if (session.getSmtpServer() != null) {
            server.add(session.getSmtpServer());
        }

        if (session.getPopServer() != null) {
            server.add(session.getPopServer());
        }

        dataProvider.setList(server);
        mailFormAsset.getForm().clearValues();
        credentialReferenceFormAsset.getForm().clearValues();
        table.selectDefaultEntity();
        SelectionChangeEvent.fire(selectionModel);
    }
}
