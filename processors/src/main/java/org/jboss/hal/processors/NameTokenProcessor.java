/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.hal.processors;

import com.google.auto.service.AutoService;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.client.plugins.SearchIndexRegistry;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;
import static org.jboss.as.console.spi.OperationMode.Mode.STANDALONE;

/**
 * Processor which scans all {@code @NameToken}s annotations and generates several registry and helper classes based
 * on the additional annotations bound to each place.
 * <dl>
 * <dt>{@code @RequiredResources}</dt>
 * <dd>All required resources are collected and stored in an implementation of {@link RequiredResourcesRegistry}</dd>
 * <dt>{@code @AccessControl}</dt>
 * <dd>Same as above. Used for backward compatibility.
 * The {@code @AccessControl} is deprecated and should be replaced by {@code @RequiredResources}</dd>
 * <dt>{@code @SearchIndex}</dt>
 * <dd>All keywords are collected and stored in an implementation of {@link SearchIndexRegistry}</dd>
 * </dl>
 *
 * @author Harald Pehl
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.gwtplatform.mvp.client.annotations.NameToken")
public class NameTokenProcessor extends AbstractHalProcessor {

    static final String REQUIRED_RESOURCES_TEMPLATE = "RequiredResources.ftl";
    static final String REQUIRED_RESOURCES_PACKAGE = "org.jboss.as.console.client.plugins";
    static final String REQUIRED_RESOURCES_CLASS = "RequiredResourcesRegistryImpl";

    static final String SEARCH_INDEX_TEMPLATE = "SearchIndex.ftl";
    static final String SEARCH_INDEX_PACKAGE = "org.jboss.as.console.client.plugins";
    static final String SEARCH_INDEX_CLASS = "SearchIndexRegistryImpl";

    private final Set<NameTokenInfo> tokenInfos;

    public NameTokenProcessor() {
        tokenInfos = new HashSet<>();
    }

    @Override
    protected boolean onProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(NameToken.class)) {
            TypeElement tokenElement = (TypeElement) e;
            NameToken nameToken = tokenElement.getAnnotation(NameToken.class);
            NameTokenInfo tokenInfo = new NameTokenInfo(nameToken.value()[0]);
            tokenInfos.add(tokenInfo);

            RequiredResources requiredResources = tokenElement.getAnnotation(RequiredResources.class);
            AccessControl accessControl = tokenElement.getAnnotation(AccessControl.class);
            NoGatekeeper noGatekeeper = tokenElement.getAnnotation(NoGatekeeper.class);
            if (accessControl != null && requiredResources != null) {
                warning(e, "Detected both @%s and @%s on proxy with token \"#%s\". Only @%1$s will be processed.",
                        RequiredResources.class.getSimpleName(), AccessControl.class.getSimpleName(), tokenInfo.getToken());
            }
            if (requiredResources != null) {
                tokenInfo.addResources(requiredResources.resources());
                tokenInfo.addOperations(requiredResources.operations());
                tokenInfo.setRecursive(requiredResources.recursive());
            } else if (accessControl != null) {
                tokenInfo.addResources(accessControl.resources());
                tokenInfo.addOperations(accessControl.operations());
                tokenInfo.setRecursive(accessControl.recursive());
            } else if (noGatekeeper == null) {
                warning(e, "Proxy with token \"#%s\" is missing @%s annotation.",
                        tokenInfo.getToken(), RequiredResources.class.getSimpleName());
            }

            SearchIndex searchIndex = tokenElement.getAnnotation(SearchIndex.class);
            if (searchIndex != null) {
                tokenInfo.addKeywords(searchIndex.keywords());
                tokenInfo.setExclude(searchIndex.exclude());
                OperationMode operationMode = tokenElement.getAnnotation(OperationMode.class);
                if (operationMode != null) {
                    tokenInfo.setDomainOnly(operationMode.value() == DOMAIN);
                    tokenInfo.setStandaloneOnly(operationMode.value() == STANDALONE);
                }
            }
        }

        if (!tokenInfos.isEmpty()) {
            debug("Generating code for required resources registry");
            code(REQUIRED_RESOURCES_TEMPLATE, REQUIRED_RESOURCES_PACKAGE, REQUIRED_RESOURCES_CLASS,
                    context(REQUIRED_RESOURCES_PACKAGE, REQUIRED_RESOURCES_CLASS));

            debug("Generating code for search index registry");
            code(SEARCH_INDEX_TEMPLATE, SEARCH_INDEX_PACKAGE, SEARCH_INDEX_CLASS,
                    context(SEARCH_INDEX_PACKAGE, SEARCH_INDEX_CLASS));

            info("Successfully generated name token registries [%s] and [%s].",
                    REQUIRED_RESOURCES_CLASS, SEARCH_INDEX_CLASS);
            tokenInfos.clear();
        }
        return false;
    }

    private Supplier<Map<String, Object>> context(final String packageName, final String className) {
        return () -> {
            Map<String, Object> context = new HashMap<>();
            context.put("packageName", packageName);
            context.put("className", className);
            context.put("tokenInfos", tokenInfos);
            return context;
        };
    }


    public static class NameTokenInfo {
        private final String token;

        // required resources
        private Set<String> resources;
        private Set<String> operations;
        private boolean recursive;

        // search index
        private Set<String> keywords;
        private boolean exclude;
        private boolean domainOnly;
        private boolean standaloneOnly;

        public NameTokenInfo(String token) {
            this.token = token;
            this.resources = new HashSet<>();
            this.operations = new HashSet<>();
            this.recursive = false;
            this.keywords = new HashSet<>();
            this.exclude = false;
            this.domainOnly = false;
            this.standaloneOnly = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NameTokenInfo)) return false;

            NameTokenInfo that = (NameTokenInfo) o;
            return token.equals(that.token);

        }

        @Override
        public int hashCode() {
            return token.hashCode();
        }

        @Override
        public String toString() {
            return "NameTokenInfo{" + token + "}";
        }

        public boolean isExclude() {
            return exclude;
        }

        public void setExclude(boolean exclude) {
            this.exclude = exclude;
        }

        public Set<String> getKeywords() {
            return keywords;
        }

        public void addKeywords(String[] keywords) {
            this.keywords.addAll(asList(keywords));
        }

        public Set<String> getOperations() {
            return operations;
        }

        public void addOperations(String[] operations) {
            this.operations.addAll(asList(operations));
        }

        public boolean isRecursive() {
            return recursive;
        }

        public void setRecursive(boolean recursive) {
            this.recursive = recursive;
        }

        public Set<String> getResources() {
            return resources;
        }

        public void addResources(String[] resources) {
            this.resources.addAll(asList(resources));
        }

        public String getToken() {
            return token;
        }

        public boolean isDomainOnly() {
            return domainOnly;
        }

        public void setDomainOnly(boolean domainOnly) {
            this.domainOnly = domainOnly;
        }

        public boolean isStandaloneOnly() {
            return standaloneOnly;
        }

        public void setStandaloneOnly(boolean standaloneOnly) {
            this.standaloneOnly = standaloneOnly;
        }
    }
}
