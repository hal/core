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

import com.google.common.base.Supplier;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.*;

/**
 * An abstract annotation processor with some convenience methods and an implicit workflow. The processor uses
 * Freemarker templates to generate source code and resource files.
 *
 * @author Harald Pehl
 */
public abstract class AbstractHalProcessor extends AbstractProcessor {

    protected final Configuration config;

    private int round;
    protected Types typeUtils;
    protected Elements elementUtils;
    protected Filer filer;
    protected Messager messager;


    // ------------------------------------------------------ initialization

    public AbstractHalProcessor() {

        Version version = new Version(2, 3, 22);
        config = new Configuration(version);
        config.setDefaultEncoding("UTF-8");
        config.setClassForTemplateLoading(getClass(), "templates");
        config.setObjectWrapper(new DefaultObjectWrapperBuilder(version).build());
    }

    @Override
    public final synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        round = 0;
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        afterInit(processingEnv);
    }

    /**
     * This method is called after {@link #init(ProcessingEnvironment)} has finished.
     * All protected fields are initialized when this method is called.
     *
     * @param processingEnv the processing environment
     */
    protected void afterInit(ProcessingEnvironment processingEnv) {
    }


    // ------------------------------------------------------ processing

    /**
     * As long as the processing isn't over {@link #onProcess(Set, RoundEnvironment)} will be called, otherwise
     * {@link #onLastRound(RoundEnvironment)} will be called. If there's a {@link GenerationException} thrown,
     * {@link #onError(GenerationException)} will be called and this method returns {@code true}.
     *
     * @param annotations the annotation types requested to be processed
     * @param roundEnv    environment for information about the current and prior round
     * @return whether or not the set of annotations are claimed by this processor
     */
    @Override
    public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        boolean result;
        if (!roundEnv.processingOver()) {
            try {
                result = onProcess(annotations, roundEnv);
            } catch (GenerationException e) {
                onError(e);
                result = true;
            }
        } else {
            try {
                result = onLastRound(roundEnv);
            } catch (GenerationException e) {
                onError(e);
                result = true;
            }
        }
        round++;
        return result;
    }

    /**
     * Called as long as the processing isn't over. The default implementation just returns {@code true}.
     *
     * @param annotations the annotation types requested to be processed
     * @param roundEnv    environment for information about the current and prior round
     * @return whether or not the set of annotations are claimed by this processor
     */
    protected boolean onProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return true;
    }

    /**
     * Called if the processing is over (last round). The default implementation just returns {@code true}.
     * <p/>
     * It's not recommended to use this method to generate java source code. Code generated by this method will not be
     * processed by the compiler! You can generate resource files here or cleanup internal state.
     *
     * @param roundEnv environment for information about the current and prior round
     * @return whether or not the set of annotations are claimed by this processor
     */
    protected boolean onLastRound(RoundEnvironment roundEnv) {
        return true;
    }

    /**
     * Called when a {@link #onLastRound(RoundEnvironment)} was catched in {@link #process(Set, RoundEnvironment)}. The
     * default implementation logs the exception using {@link #error(GenerationException)}.
     *
     * @param e the exception
     */
    protected void onError(GenerationException e) {
        error(e);
    }

    protected int round() {
        return round;
    }


    // ------------------------------------------------------ configuration

    /**
     * @return {@link SourceVersion#getLatestSupported()}
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    // ------------------------------------------------------ code generation

    /**
     * Generates and writes java code in one call.
     *
     * @param template    the relative template name (w/o path or package name)
     * @param packageName the package name
     * @param className   the class name
     * @param context     a function to create the templates' context
     */
    protected void code(String template, String packageName, String className, Supplier<Map<String, Object>> context) {
        StringBuffer code = generate(template, context);
        writeCode(packageName, className, code);
    }

    /**
     * Generates and writes a resource in one call.
     *
     * @param template     the relative template name (w/o path or package name)
     * @param packageName  the package name
     * @param resourceName the resource name
     * @param context      a function to create the templates' context
     */
    protected void resource(String template, String packageName, String resourceName, Supplier<Map<String, Object>> context) {
        StringBuffer code = generate(template, context);
        writeResource(packageName, resourceName, code);
    }

    /**
     * Generates the code using the specified context and freemarker template. Wraps any kind of error inside a
     * {@code GenerationException}.
     *
     * @param template the relative template name (w/o path or package name)
     * @param context  a function to create the templates' context
     * @return the generated content
     */
    protected StringBuffer generate(String template, Supplier<Map<String, Object>> context) {
        final StringWriter sw = new StringWriter();
        final BufferedWriter bw = new BufferedWriter(sw);
        try {
            final Template t = config.getTemplate(template);
            t.process(context.get(), bw);
        } catch (IOException | TemplateException ioe) {
            throw new GenerationException("Error generating template " + template + ": " + ioe.getMessage());
        } finally {
            try {
                bw.close();
                sw.close();
            } catch (IOException ioe) {
                //noinspection ThrowFromFinallyBlock
                throw new GenerationException("Error generating template " + template + ": " + ioe.getMessage());
            }
        }
        return sw.getBuffer();
    }

    /**
     * Writes the specified source code and wraps any {@code IOException} as {@link GenerationException}.
     *
     * @param packageName the package name
     * @param className   the class name
     * @param code        the source code
     * @throws GenerationException if an {@code IOException occurs}
     */
    protected void writeCode(final String packageName, final String className, final StringBuffer code) {
        try {
            JavaFileObject jfo = filer.createSourceFile(packageName + "." + className);
            Writer w = jfo.openWriter();
            BufferedWriter bw = new BufferedWriter(w);
            bw.append(code);
            bw.close();
            w.close();
        } catch (IOException e) {
            throw new GenerationException(String.format("Error writing code for %s.%s: %s",
                    packageName, className, e.getMessage()));
        }
    }

    /**
     * Writes the specified resource and wraps any {@code IOException} as {@link GenerationException}.
     *
     * @param packageName  the package name
     * @param resourceName the resource name
     * @param content      the content
     * @throws GenerationException if an {@code IOException occurs}
     */
    protected void writeResource(final String packageName, final String resourceName, final StringBuffer content) {
        try {
            FileObject mf = filer.createResource(StandardLocation.SOURCE_OUTPUT, packageName, resourceName);
            Writer w = mf.openWriter();
            BufferedWriter bw = new BufferedWriter(w);
            bw.append(content);
            bw.close();
            w.close();
        } catch (IOException e) {
            throw new GenerationException(String.format("Error writing content for %s.%s: %s",
                    packageName, resourceName, e.getMessage()));
        }
    }


    // ------------------------------------------------------ logging

    protected void debug(String msg, Object... args) {
        if (processingEnv.getOptions().containsKey("debug")) {
            messager.printMessage(NOTE, String.format(msg, args));
        }
    }

    protected void info(String msg, Object... args) {
        messager.printMessage(NOTE, String.format(msg, args));
    }

    protected void warning(Element element, String msg, Object... args) {
        messager.printMessage(WARNING, String.format(msg, args), element);
    }

    protected void error(String msg, Object... args) {
        messager.printMessage(ERROR, String.format(msg, args));
    }

    protected void error(GenerationException generationException) {
        if (generationException.getElement() != null) {
            messager.printMessage(ERROR, generationException.getMessage(), generationException.getElement());
        } else {
            messager.printMessage(ERROR, generationException.getMessage());
        }
    }
}
