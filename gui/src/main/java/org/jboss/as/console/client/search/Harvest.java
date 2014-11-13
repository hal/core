package org.jboss.as.console.client.search;

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
import org.jboss.gwt.flow.client.*;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;

import javax.inject.Inject;
import java.util.*;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

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

        Set<Function<Map<String, SearchIndexData>>> functions = new HashSet<Function<Map<String, SearchIndexData>>>();
        for (final String token : searchIndexRegistry.getTokens(bootstrap.isStandalone())) {
            final Set<String> resources = searchIndexRegistry.getResources(token);
            final Set<String> keywords = searchIndexRegistry.getKeywords(token);
            for (final String resource : resources) {
                // TODO
                if (resource.startsWith("opt:")) { continue; }

                final ModelNode op = AddressMapping.fromString(resource).asResource(filteringStatementContext);
                // skip unrecognized addresses
                if (!op.get(ADDRESS).isDefined()) {
                    System.out.println("Skipped " + token + " > " + resource + ": No valid address could be resolved: " + op);
                    continue;
                }
                op.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);

                Function<Map<String, SearchIndexData>> f = new Function<Map<String, SearchIndexData>>() {
                    @Override
                    public void execute(final Control<Map<String, SearchIndexData>> control) {

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

                                                collectSearchIndex(token, resource, description, keywords);

                                                index.add(token, keywords, description);
                                                handler.onHarvest(token, address, description);
                                            } else {
                                                System.out.println("Denied by harvest handler " + token + " > " + resource);
                                            }
                                        }
                                    } catch (Throwable e) {
                                        System.out.println("Skipped " + token + " > " + resource + ": " + e.getMessage());
                                    }
                                }
                                control.proceed();
                            }

                            private void collectSearchIndex(String token, String resource, String description, Set<String> keywords) {
                                SearchIndexData sid = control.getContext().get(token);
                                if (sid == null) {
                                    sid = new SearchIndexData(token, keywords);
                                    control.getContext().put(token, sid);
                                }
                                sid.resources.add(resource);
                                sid.descriptions.add(description);
                            }
                        });
                    }
                };
                functions.add(f);
            }
        }

        //noinspection unchecked
        final Map<String, SearchIndexData> searchIndexDump = new HashMap<>();
        new Async<Map<String, SearchIndexData>>(progress).parallel(searchIndexDump, new Outcome<Map<String, SearchIndexData>>() {
            @Override
            public void onFailure(Map<String, SearchIndexData> context) {
                Console.error("Harvest failed");
            }

            @Override
            public void onSuccess(Map<String, SearchIndexData> context) {
                dumpSearchIndex(context);
                handler.onFinish();
            }

            private void dumpSearchIndex(Map<String, SearchIndexData> context) {
                System.out.println("token|resources|descriptions|keywords");
                for (SearchIndexData sid : context.values()) {
                    System.out.print(sid.token);
                    System.out.print("|");
                    for (String resource : sid.resources) {
                        System.out.print(resource + ";");
                    }
                    System.out.print("|");
                    for (String desc : sid.descriptions) {
                        System.out.print(desc + ";");
                    }
                    System.out.print("|");
                    for (String keyword : sid.keywords) {
                        System.out.print(keyword + ";");
                    }
                    System.out.println();
                }
            }
        }, functions.toArray(new Function[functions.size()]));
    }

    class SearchIndexData {
        private final String token;
        private final Set<String> keywords;
        private final List<String> resources;
        private final List<String> descriptions;

        SearchIndexData(final String token, Set<String> keywords) {
            this.token = token;
            this.keywords = keywords;
            this.resources = new ArrayList<>();
            this.descriptions = new ArrayList<>();
        }
    }

    public interface Handler {

        void onStart();

        boolean shouldHarvest(String token, String address, String description);

        void onHarvest(String token, String address, String description);

        void onFinish();

        void onError(Throwable t);
    }
}
