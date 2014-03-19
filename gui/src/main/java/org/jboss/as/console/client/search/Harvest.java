package org.jboss.as.console.client.search;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.plugins.SearchIndexRegistry;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;
import org.jboss.gwt.flow.client.Progress;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;

/**
 * Creates search indexes.
 *
 * @author Heiko Braun
 */
public class Harvest {

    private final SearchIndexRegistry searchIndexRegistry;
    private final DispatchAsync dispatcher;
    private final BootstrapContext bootstrap;
    private final Index index;
    private final FilteringStatementContext filteringStatementContext;

    @Inject
    public Harvest(SearchIndexRegistry searchIndexRegistry, DispatchAsync dispatcher,
            CoreGUIContext statementContext, final BootstrapContext bootstrap, Index index) {

        this.searchIndexRegistry = searchIndexRegistry;
        this.dispatcher = dispatcher;
        this.bootstrap = bootstrap;
        this.index = index;
        this.filteringStatementContext = new FilteringStatementContext(
                statementContext,
                new FilteringStatementContext.Filter() {
                    @Override
                    public String filter(String key) {
                        if ("selected.entity".equals(key)) {
                            return "*";
                        } else if ("addressable.group".equals(key)) {
                            return bootstrap.getAddressableGroups().isEmpty() ? "*" : bootstrap.getAddressableGroups()
                                    .iterator().next();
                        } else if ("addressable.host".equals(key)) {
                            return bootstrap.getAddressableHosts().isEmpty() ? "*" : bootstrap.getAddressableHosts()
                                    .iterator().next();
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public String[] filterTuple(String key) {
                        return null;
                    }
                }
        );
    }

    public void run(final Handler handler) {
        run(handler, Footer.PROGRESS_ELEMENT);
    }

    public void run(final Handler handler, Progress progress) {
        handler.onStart();

        Set<Function<Object>> functions = new HashSet<Function<Object>>();
        for (final String token : searchIndexRegistry.getTokens(bootstrap.isStandalone())) {
            Set<String> resources = searchIndexRegistry.getResources(token);
            final Set<String> keywords = searchIndexRegistry.getKeywords(token);
            for (final String resource : resources) {
                // TODO
                if (resource.startsWith("opt:")) { continue; }

                final ModelNode op = AddressMapping.fromString(resource).asResource(filteringStatementContext);
                op.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);

                Function<Object> f = new Function<Object>() {
                    @Override
                    public void execute(final Control control) {

                        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                System.out.println("Skipped " + token + " > " + resource);
                                handler.onError(caught);
                                control.proceed();
                            }

                            @Override
                            public void onSuccess(DMRResponse result) {
                                ModelNode response = result.get();
                                if (response.isFailure()) {
                                    handler.onError(new RuntimeException(response.getFailureDescription()));
                                    System.out.println("Skipped " + token + " > " + resource);
                                } else {
                                    ModelNode delegate = response.get(RESULT).getType().equals(ModelType.LIST) ?
                                            response.get(RESULT).asList().get(0) : response.get(RESULT);
                                    try {
                                        String description = delegate.hasDefined(DESCRIPTION) ?
                                                delegate.get(DESCRIPTION).asString() : delegate.get(RESULT)
                                                .get(DESCRIPTION).asString();

                                        // todo: cleanup
                                        if (description.equals("undefined")) {
                                            System.out.println("Undefined description " + token + " > " + resource);
                                        } else {
                                            String address = op.get(ADDRESS).asString();
                                            if (handler.shouldHarvest(token, address, description)) {
                                                index.add(token, keywords, description);
                                                handler.onHarvest(token, address, description);
                                            } else {
                                                System.out.println("Denied by harvest handler " + token + " > " + resource);
                                            }
                                        }
                                    } catch (Throwable e) {
                                        System.out.println("Skipped " + token + " > " + resource);
                                    }
                                }
                                control.proceed();
                            }
                        });
                    }
                };
                functions.add(f);
            }
        }

        //noinspection unchecked
        new Async<Object>(progress).parallel(null, new Outcome<Object>() {
            @Override
            public void onFailure(Object context) {
                Console.error("Harvest failed");
            }

            @Override
            public void onSuccess(Object context) {
                handler.onFinish();
            }
        }, functions.toArray(new Function[functions.size()]));
    }

    public interface Handler {

        void onStart();

        boolean shouldHarvest(String token, String address, String description);

        void onHarvest(String token, String address, String description);

        void onFinish();

        void onError(Throwable t);
    }
}
