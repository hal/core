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
import com.google.common.base.Supplier;
import org.jboss.gwt.circuit.meta.Store;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Processor which automates initialization of stores and their adapters. The processor generates the following
 * artifacts:
 * <ol>
 * <li>GIN binding which binds each store and its adapter to scope singleton</li>
 * <li>GIN module with getters for the stores and their adapters</li>
 * <li>bootstrap step which initializes the store adapters (in order to register the store callback handlers)</li>
 * </ol>
 * <p/>
 * The processor makes some assumptions about the stores and their adapters:
 * <ul>
 * <li>They belong to the same package</li>
 * <li>The adapter's name follows the pattern {@code &lt;StoreName&gt;Adapter}</li>
 * </ul>
 *
 * @author Harald Pehl
 */
@AutoService(Processor.class)
@SupportedOptions("debug")
@SupportedAnnotationTypes("org.jboss.gwt.circuit.meta.Store")
public class StoreInitProcessor extends AbstractHalProcessor {

    static final String ADAPTER_SUFFIX = "Adapter";

    static final String STORE_BINDINGS_TEMPLATE = "StoreBindings.ftl";
    static final String STORE_BINDINGS_PACKAGE = "org.jboss.as.console.client.core.gin";
    static final String STORE_BINDINGS_CLASS = "StoreBindings";

    static final String STORE_MODULE_TEMPLATE = "StoreModule.ftl";
    static final String STORE_MODULE_PACKAGE = "org.jboss.as.console.client.core.gin";
    static final String STORE_MODULE_CLASS = "StoreModule";

    static final String STORE_BOOTSTRAP_TEMPLATE = "StoreBootstrap.ftl";
    static final String STORE_BOOTSTRAP_PACKAGE = "org.jboss.as.console.client.core.bootstrap";
    static final String STORE_BOOTSTRAP_CLASS = "StoreInit";

    private final Set<StoreInfo> storeInfos;

    public StoreInitProcessor() {
        storeInfos = new HashSet<>();
    }

    @Override
    protected boolean onProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(Store.class)) {
            TypeElement storeElement = (TypeElement) e;
            PackageElement packageElement = (PackageElement) storeElement.getEnclosingElement();
            String packageName = packageElement.getQualifiedName().toString();
            String storeDelegate = storeElement.getSimpleName().toString();
            String storeAdapter = storeDelegate + ADAPTER_SUFFIX;
            StoreInfo storeInfo = new StoreInfo(packageName, storeDelegate, storeAdapter);
            storeInfos.add(storeInfo);
            debug("Discovered store / store adapter [%s]", storeInfo);
        }

        // Don't generate files in onLastRound, since the generated GIN bindings and modules
        // need to be picked up by the GinProcessor!
        if (!storeInfos.isEmpty()) {
            debug("Generating code for store bindings");
            code(STORE_BINDINGS_TEMPLATE, STORE_BINDINGS_PACKAGE, STORE_BINDINGS_CLASS,
                    context(STORE_BINDINGS_PACKAGE, STORE_BINDINGS_CLASS));

            debug("Generating code for store module");
            code(STORE_MODULE_TEMPLATE, STORE_MODULE_PACKAGE, STORE_MODULE_CLASS,
                    context(STORE_MODULE_PACKAGE, STORE_MODULE_CLASS));

            debug("Generating code for store bootstrap");
            code(STORE_BOOTSTRAP_TEMPLATE, STORE_BOOTSTRAP_PACKAGE, STORE_BOOTSTRAP_CLASS,
                    context(STORE_BOOTSTRAP_PACKAGE, STORE_BOOTSTRAP_CLASS));

            info("Successfully generated store initialization classes [%s], [%s] and [%s].",
                    STORE_BINDINGS_CLASS, STORE_MODULE_CLASS, STORE_BOOTSTRAP_CLASS);
            storeInfos.clear();
        }
        return false;
    }

    private Supplier<Map<String, Object>> context(final String packageName, final String className) {
        return new Supplier<Map<String, Object>>() {
            @Override
            public Map<String, Object> get() {
                Map<String, Object> context = new HashMap<>();
                context.put("packageName", packageName);
                context.put("className", className);
                context.put("storeInfos", storeInfos);
                return context;
            }
        };
    }


    public static final class StoreInfo {
        private final String packageName;
        private final String storeDelegate;
        private final String storeAdapter;

        StoreInfo(String packageName, String storeDelegate, String storeAdapter) {
            this.packageName = packageName;
            this.storeDelegate = storeDelegate;
            this.storeAdapter = storeAdapter;
        }

        @Override
        @SuppressWarnings("SimplifiableIfStatement")
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StoreInfo)) return false;

            StoreInfo storeInfo = (StoreInfo) o;

            if (!packageName.equals(storeInfo.packageName)) return false;
            if (!storeDelegate.equals(storeInfo.storeDelegate)) return false;
            return storeAdapter.equals(storeInfo.storeAdapter);

        }

        @Override
        public int hashCode() {
            int result = packageName.hashCode();
            result = 31 * result + storeDelegate.hashCode();
            result = 31 * result + storeAdapter.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return storeDelegate + " / " + storeAdapter;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getStoreAdapter() {
            return storeAdapter;
        }

        public String getStoreDelegate() {
            return storeDelegate;
        }
    }
}
