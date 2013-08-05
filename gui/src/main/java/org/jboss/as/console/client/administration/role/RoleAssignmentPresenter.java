package org.jboss.as.console.client.administration.role;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Outcome;

/**
 * @author Harald Pehl
 * @date 07/22/2013
 */
public class RoleAssignmentPresenter
        extends Presenter<RoleAssignmentPresenter.MyView, RoleAssignmentPresenter.MyProxy> {

    private final DispatchAsync dispatcher;
    private final RevealStrategy revealStrategy;
    private DefaultWindow window;


    @Inject
    public RoleAssignmentPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final DispatchAsync dispatcher, final RevealStrategy revealStrategy) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInAdministration(this);
    }

    public void launchAddDialg(final StandardRole role, final RoleAssignment roleAssignment,
            final Principal.Type principalType) {
        String title = principalType == Principal.Type.USER ? Console.CONSTANTS.role_assignment_add_user() : Console
                .CONSTANTS.role_assignment_add_group();
        window = new DefaultWindow(title);
        window.setWidth(480);
        window.setHeight(300);
        window.trapWidget(new AddPrincipalWizard(this, role, roleAssignment, principalType).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onAdd(final StandardRole role, final RoleAssignment roleAssignment, final Principal principal) {
        closeDialog();
        System.out.println("About to add " + principal.getType() + " " + principal
                .getName() + " to role " + role + " / " + (roleAssignment.isInclude() ? "includes" : "exludes"));

        AddPrincipalOperation addPrincipalOperation = new AddPrincipalOperation(dispatcher);
        addPrincipalOperation.extecute(role, roleAssignment, principal, new Outcome<StringBuilder>() {
            @Override
            public void onFailure(final StringBuilder context) {
                // TODO Error handling
                Console.MODULES.getMessageCenter().notify(new Message("Cannot add principal", Message.Severity.Error));
            }

            @Override
            public void onSuccess(final StringBuilder context) {
                getView().refresh();
            }
        });
    }

    public void onDelete(final StandardRole role, final RoleAssignment roleAssignment, final Principal principal) {
        System.out.println("About to delete " + principal.getType() + " " + principal
                .getName() + " from role " + role + " / " + (roleAssignment.isInclude() ? "includes" : "exludes"));

        final ModelNode operation = new ModelNode();
        StringBuilder principalKey = new StringBuilder();
        boolean realmGiven = principal.getRealm() != null && principal.getRealm().length() != 0;
        principalKey.append(principal.getType().name().toLowerCase()).append("-").append(principal.getName());
        if (realmGiven) {
            principalKey.append("@").append(principal.getRealm());
        }
        operation.get(ADDRESS).add("core-service", "management");
        operation.get(ADDRESS).add("access", "authorization");
        operation.get(ADDRESS).add("role-mapping", role.name());
        operation.get(ADDRESS).add(roleAssignment.isInclude() ? "include" : "exclude", principalKey.toString());
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse response) {
                getView().refresh();
            }
        });
    }

    public void closeDialog() {
        if (window != null) {
            window.hide();
        }
    }

    @ProxyCodeSplit
    @NameToken(NameTokens.RoleAssignmentPresenter)
    @AccessControl(resources = {"/core-service=management/access=authorization"})
    public interface MyProxy extends Proxy<RoleAssignmentPresenter>, Place {
    }

    public interface MyView extends View {

        void setPresenter(RoleAssignmentPresenter presenter);

        void refresh();
    }
}
