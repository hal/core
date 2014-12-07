package org.jboss.as.console.client.shared.subsys.mail;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.ManualRevealPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 */
public class MailPresenter extends ManualRevealPresenter<MailPresenter.MyView, MailPresenter.MyProxy> {

    @ProxyCodeSplit
    @NameToken(NameTokens.MailPresenter)
    @RequiredResources(resources = {"{selected.profile}/subsystem=mail/mail-session=*"})
    @SearchIndex(keywords = {"mail", "smtp", "imap", "channel"})
    public interface MyProxy extends ProxyPlace<MailPresenter> {}


    public interface MyView extends View {
        void setPresenter(MailPresenter presenter);
        void updateFrom(List<MailSession> list);
        void setSelectedSession(String selectedSession);
    }


    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final BeanFactory beanFactory;
    private final EntityAdapter<MailSession> adapter;
    private final BeanMetaData beanMetaData;
    private final EntityAdapter<MailServerDefinition> serverAdapter;

    private DefaultWindow window;
    private String selectedSession;


    @Inject
    public MailPresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            DispatchAsync dispatcher, RevealStrategy revealStrategy, ApplicationMetaData metaData,
            BeanFactory beanFactory) {

        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.beanFactory = beanFactory;
        this.beanMetaData = metaData.getBeanMetaData(MailSession.class);
        this.adapter = new EntityAdapter<MailSession>(MailSession.class, metaData);
        this.serverAdapter = new EntityAdapter<MailServerDefinition>(MailServerDefinition.class, metaData);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    @Override
    public void fromRequest(PlaceRequest request) {
        this.selectedSession = request.getParameter("name", null);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadMailSessions(true);
    }

    public void launchNewServerWizard(final MailSession selectedSession) {
        // TODO Read the outgoing socket bindings and replace the text input with a combo box
        window = new DefaultWindow(Console.MESSAGES.createTitle("Mail Server"));
        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new NewMailServerWizard(MailPresenter.this, selectedSession, beanFactory).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void launchNewSessionWizard() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Mail Session"));
        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new NewMailSessionWizard(MailPresenter.this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    private void loadMailSessions(final boolean refreshDetail) {
        ModelNode operation = beanMetaData.getAddress().asSubresource(Baseadress.get());
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Mail Sessions"));
                } else {
                    List<Property> items = response.get(RESULT).asPropertyList();
                    List<MailSession> sessions = new ArrayList<MailSession>(items.size());
                    for (Property item : items) {
                        ModelNode model = item.getValue();
                        MailSession mailSession = adapter.fromDMR(model);
                        mailSession.setName(item.getName());

                        if (model.hasDefined("server")) {
                            List<Property> serverList = model.get("server").asPropertyList();
                            for (Property server : serverList) {
                                if (server.getName().equals(ServerType.smtp.name())) {
                                    MailServerDefinition smtpServer = serverAdapter.fromDMR(server.getValue());
                                    smtpServer.setType(ServerType.smtp);
                                    mailSession.setSmtpServer(smtpServer);
                                } else if (server.getName().equals(ServerType.imap.name())) {
                                    MailServerDefinition imap = serverAdapter.fromDMR(server.getValue());
                                    imap.setType(ServerType.imap);
                                    mailSession.setImapServer(imap);
                                } else if (server.getName().equals(ServerType.pop3.name())) {
                                    MailServerDefinition pop = serverAdapter.fromDMR(server.getValue());
                                    pop.setType(ServerType.pop3);
                                    mailSession.setPopServer(pop);
                                }
                            }

                        }
                        sessions.add(mailSession);
                    }
                    getView().updateFrom(sessions);
                }
                if (refreshDetail)
                    getView().setSelectedSession(selectedSession);
            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public void closeDialoge() {
        window.hide();
    }


    public void onCreateSession(final MailSession entity) {
        closeDialoge();

        String name = entity.getName() != null ? entity.getName() : entity.getJndiName();
        ModelNode address = beanMetaData.getAddress().asResource(Baseadress.get(), name);

        ModelNode operation = adapter.fromEntity(entity);
        operation.get(ADDRESS).set(address.get(ADDRESS));
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response  = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Mail Session"), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Mail Session " + entity.getName()));
                }
                loadMailSessions(false);
            }
        });
    }

    public void onDelete(final MailSession entity) {
        ModelNode operation = beanMetaData.getAddress().asResource(Baseadress.get(), entity.getName());
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response  = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Mail Session"), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Mail Session " + entity.getName()));
                }
                loadMailSessions(false);
            }
        });
    }

    public void onSave(final MailSession editedEntity, Map<String, Object> changeset) {
        ModelNode address = beanMetaData.getAddress().asResource(
                Baseadress.get(),
                editedEntity.getName()
        );
        ModelNode operation = adapter.fromChangeset(changeset, address);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response  = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Mail Session"),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Mail Session " + editedEntity.getName()));
                }
                loadMailSessions(false);
            }
        });
    }

    public void onSaveServer(ServerType type, Map<String, Object> changeset) {
        ModelNode address = new ModelNode();
        address.get(ADDRESS).set(Baseadress.get());
        address.get(ADDRESS).add("subsystem", "mail");
        address.get(ADDRESS).add("mail-session", selectedSession);
        address.get(ADDRESS).add("server", type.name());
        ModelNode operation = serverAdapter.fromChangeset(changeset, address);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Mail Server"), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Mail Server"));
                }
                loadMailSessions(true);
            }
        });
    }

    public void onRemoveServer(MailServerDefinition entity) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "mail");
        operation.get(ADDRESS).add("mail-session", selectedSession);
        operation.get(ADDRESS).add("server", entity.getType().name());
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response  = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Mail Server"), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Mail Server"));
                }
                loadMailSessions(true);
            }
        });
    }

    public void onCreateServer(MailServerDefinition entity) {
        closeDialoge();

        ModelNode operation = serverAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "mail");
        operation.get(ADDRESS).add("mail-session", selectedSession);
        operation.get(ADDRESS).add("server", entity.getType().name());
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response  = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Mail Server"), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Mail Server"));
                }
                loadMailSessions(true);
            }
        });
    }
}
