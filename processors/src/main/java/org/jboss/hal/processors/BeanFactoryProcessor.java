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

import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.common.base.Optional;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import org.jboss.as.console.spi.BeanFactoryExtension;
import org.jboss.gwt.circuit.meta.*;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.*;

/**
 * Processor for the composite AutoBean factory.
 *
 * @author Harald Pehl
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({"org.jboss.as.console.spi.BeanFactoryExtension",
        "com.google.web.bindery.autobean.shared.AutoBeanFactory.Category"})
public class BeanFactoryProcessor extends AbstractHalProcessor {

    static final String BEAN_FACTORY_TEMPLATE = "BeanFactory.ftl";
    static final String BEAN_FACTORY_PACKAGE = "org.jboss.as.console.client.shared";
    static final String BEAN_FACTORY_CLASS = "BeanFactory";

    private final Set<String> factories;
    private final Set<String> categories;

    public BeanFactoryProcessor() {
        factories = new HashSet<>();
        categories = new HashSet<>();
    }

    @Override
    protected boolean onProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(BeanFactoryExtension.class)) {
            TypeElement factoryElement = (TypeElement) e;
            factories.add(factoryElement.getQualifiedName().toString());
        }

        for (Element e : roundEnv.getElementsAnnotatedWith(AutoBeanFactory.Category.class)) {
            AnnotationMirror categoryAnnotation = MoreElements.getAnnotationMirror(e, AutoBeanFactory.Category.class).get();
            AnnotationValue value = AnnotationMirrors.getAnnotationValue(categoryAnnotation, "value");
            Collection<String> categories = extractValue(value);
            this.categories.addAll(categories);
        }

        if (!factories.isEmpty()) {
            debug("Generating composite bean factory");
            code(BEAN_FACTORY_TEMPLATE, BEAN_FACTORY_PACKAGE, BEAN_FACTORY_CLASS, () -> {
                Map<String, Object> context = new HashMap<>();
                context.put("packageName", BEAN_FACTORY_PACKAGE);
                context.put("className", BEAN_FACTORY_CLASS);
                context.put("factories", factories);
                context.put("categories", categories);
                return context;
            });

            info("Successfully generated composite bean factory [%s].", BEAN_FACTORY_CLASS);
            factories.clear();
            categories.clear();
        }
        return false;
    }

    private Collection<String> extractValue(final AnnotationValue value) {
        if (value.getValue() instanceof Collection) {
            final Collection<?> varray = (List<?>) value.getValue();
            final ArrayList<String> result = new ArrayList<>(varray.size());
            for (final Object active : varray) {
                result.addAll(extractValue((AnnotationValue) active));
            }
            return result;
        }
        return Collections.singleton(value.getValue().toString());
    }
}
