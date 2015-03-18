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
import org.jboss.as.console.client.plugins.RuntimeExtensionMetaData;
import org.jboss.as.console.client.plugins.SubsystemExtensionMetaData;
import org.jboss.as.console.spi.RuntimeExtension;
import org.jboss.as.console.spi.SubsystemExtension;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Harald Pehl
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({"org.jboss.as.console.spi.SubsystemExtension", "org.jboss.as.console.spi.RuntimeExtension"})
public class ExtensionProcessor extends AbstractHalProcessor {

    static final String SUBSYSTEM_EXTENSION_TEMPLATE = "SubsystemExtensions.ftl";
    static final String SUBSYSTEM_EXTENSION_PACKAGE = "org.jboss.as.console.client.plugins";
    static final String SUBSYSTEM_EXTENSION_CLASS = "SubsystemRegistryImpl";

    static final String RUNTIME_EXTENSION_TEMPLATE = "RuntimeExtensions.ftl";
    static final String RUNTIME_EXTENSION_PACKAGE = "org.jboss.as.console.client.plugins";
    static final String RUNTIME_EXTENSION_CLASS = "RuntimeLHSItemExtensionRegistryImpl";

    private final List<SubsystemExtensionMetaData> subsytemExtensions;
    private final List<RuntimeExtensionMetaData> runtimeExtensions;

    public ExtensionProcessor() {
        subsytemExtensions = new ArrayList<>();
        runtimeExtensions = new ArrayList<>();
    }

    @Override
    protected boolean onProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(SubsystemExtension.class)) {
            TypeElement element = (TypeElement) e;
            NameToken nameToken = element.getAnnotation(NameToken.class);
            SubsystemExtension subsystemExtension = element.getAnnotation(SubsystemExtension.class);
            if (nameToken != null && subsystemExtension != null) {
                debug("Processing subsystem extension %s -> %s", subsystemExtension.name(), nameToken.value()[0]);
                SubsystemExtensionMetaData smd = new SubsystemExtensionMetaData(subsystemExtension.name(),
                        nameToken.value()[0], subsystemExtension.group(), subsystemExtension.key());
                subsytemExtensions.add(smd);
            }
        }

        for (Element e : roundEnv.getElementsAnnotatedWith(RuntimeExtension.class)) {
            TypeElement element = (TypeElement) e;
            NameToken nameToken = element.getAnnotation(NameToken.class);
            RuntimeExtension runtimeExtension = element.getAnnotation(RuntimeExtension.class);
            if (nameToken != null && runtimeExtension != null) {
                debug("Processing runtime extension %s -> %s", runtimeExtension.name(), nameToken.value()[0]);
                RuntimeExtensionMetaData rmd = new RuntimeExtensionMetaData(runtimeExtension.name(), nameToken.value()[0],
                        runtimeExtension.group(), runtimeExtension.key());
                runtimeExtensions.add(rmd);
            }
        }

        if (!subsytemExtensions.isEmpty()) {
            debug("Generating subsystem extension registry");
            code(SUBSYSTEM_EXTENSION_TEMPLATE, SUBSYSTEM_EXTENSION_PACKAGE, SUBSYSTEM_EXTENSION_CLASS,
                    () -> {
                        Map<String, Object> context = new HashMap<>();
                        context.put("packageName", SUBSYSTEM_EXTENSION_PACKAGE);
                        context.put("className", SUBSYSTEM_EXTENSION_CLASS);
                        context.put("subsystemExtensions", subsytemExtensions);
                        return context;
                    });
            info("Successfully generated subsystem extension registry [%s].", SUBSYSTEM_EXTENSION_CLASS);
            subsytemExtensions.clear();
        }

        if (!runtimeExtensions.isEmpty()) {
            debug("Generating runtime extension registry");
            code(RUNTIME_EXTENSION_TEMPLATE, RUNTIME_EXTENSION_PACKAGE, RUNTIME_EXTENSION_CLASS,
                    () -> {
                        Map<String, Object> context = new HashMap<>();
                        context.put("packageName", RUNTIME_EXTENSION_PACKAGE);
                        context.put("className", RUNTIME_EXTENSION_CLASS);
                        context.put("runtimeExtensions", runtimeExtensions);
                        return context;
                    });
            info("Successfully generated runtime extension registry [%s].", RUNTIME_EXTENSION_CLASS);
            runtimeExtensions.clear();
        }
        return false;
    }
}
