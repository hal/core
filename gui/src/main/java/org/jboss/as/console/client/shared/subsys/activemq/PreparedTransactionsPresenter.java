/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared.subsys.activemq;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.activemq.model.PreparedTransaction;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;

public class PreparedTransactionsPresenter
        extends Presenter<PreparedTransactionsPresenter.MyView, PreparedTransactionsPresenter.MyProxy>
        implements MessagingAddress {

    @ProxyCodeSplit
    @NameToken(NameTokens.ActivemqTransactions)
    @SearchIndex(keywords = {"jms", "messaging", "transactions"})
    public interface MyProxy extends Proxy<PreparedTransactionsPresenter>, Place {}

    public interface MyView extends View, HasPresenter<PreparedTransactionsPresenter> {
        void setTransactions(List<PreparedTransaction> transactions);
    }

    private final DispatchAsync dispatcher;
    private final RevealStrategy revealStrategy;
    private final BootstrapContext bootstrapContext;

    private String currentServer;

    @Inject
    public PreparedTransactionsPresenter(EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher, RevealStrategy revealStrategy, BootstrapContext bootstrapContext) {
        super(eventBus, view, proxy);
        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.bootstrapContext = bootstrapContext;
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        currentServer = request.getParameter("name", null);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadTransactions();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    protected void onCommit(PreparedTransaction transaction) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(OP).set("commit-prepared-transaction");
        operation.get("transaction-as-base-64").set(transaction.getXid());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to commit transaction", response.getFailureDescription());
                }
                loadTransactions();
            }
        });
    }

    protected void onRollback(PreparedTransaction transaction) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(OP).set("rollback-prepared-transaction");
        operation.get("transaction-as-base-64").set(transaction.getXid());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to rollback transaction", response.getFailureDescription());
                }
                loadTransactions();
            }
        });
    }

    public String getCurrentServer() {
        return currentServer;
    }

    private List<PreparedTransaction> parseTransactions(List<ModelNode> transactions) {
        RegExp transactionPattern = RegExp.compile("^(.*) base64: ([^ ]*)");
        List<PreparedTransaction> preparedTransactions = new ArrayList<>();

        for(ModelNode t : transactions) {
            MatchResult match = transactionPattern.exec(t.asString());
            if (match == null) {
                Console.error("Error parsing prepared transactions");
                break;
            }
            preparedTransactions.add(new PreparedTransaction(match.getGroup(2), match.getGroup(1)));
        }
        return preparedTransactions;
    }

    private void loadTransactions() {
        // transaction control is not available in domain mode
        if (bootstrapContext.isStandalone()) {
            ModelNode operation = new ModelNode();
            operation.get(ADDRESS).set(Baseadress.get());
            operation.get(ADDRESS).add("subsystem", "messaging-activemq");
            operation.get(ADDRESS).add("server", getCurrentServer());
            operation.get(OP).set("list-prepared-transactions");

            dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(DMRResponse result) {
                    ModelNode response = result.get();
                    ModelNode transactions = response.get(RESULT);
                    if (response.isFailure()) {
                        Console.error("Unable to load transaction", response.getFailureDescription());
                    } else {
                        getView().setTransactions(parseTransactions(transactions.asList()));
                    }
                }
            });
        }
    }
}
