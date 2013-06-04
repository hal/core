package org.jboss.as.console.rebind;

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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import java_cup.version;

import java.io.PrintWriter;

/**
 * @author Heiko Braun
 * @date 4/19/11
 */
public class ProductConfigGenerator extends Generator {

    /**
     * Simple name of class to be generated
     */
    private String className = null;

    /**
     * Package name of class to be generated
     */
    private String packageName = null;

    public String generate(TreeLogger logger, GeneratorContext context, String typeName)
            throws UnableToCompleteException
    {
        TypeOracle typeOracle = context.getTypeOracle();

        try
        {
            // get classType and save instance variables
            JClassType classType = typeOracle.getType(typeName);
            packageName = classType.getPackage().getName();
            className = classType.getSimpleSourceName() + "Impl";

            // Generate class source code
            generateClass(logger, context);

        }
        catch (Throwable e)
        {
            // record to logger that Map generation threw an exception
            e.printStackTrace(System.out);
            logger.log(TreeLogger.ERROR, "Failed to generate product config", e);
        }

        // return the fully qualified name of the class generated
        return packageName + "." + className;
    }

    /**
     * Generate source code for new class. Class extends
     * <code>HashMap</code>.
     *
     * @param logger  Logger object
     * @param context Generator context
     */
    private void generateClass(TreeLogger logger, GeneratorContext context) throws Throwable
    {

        // get print writer that receives the source code
        PrintWriter printWriter = context.tryCreate(logger, packageName, className);

        // print writer if null, source code has ALREADY been generated, return
        if (printWriter == null) return;

        // init composer, set class properties, create source writer
        ClassSourceFileComposerFactory composerFactory =
                new ClassSourceFileComposerFactory(packageName, className);

        // Imports
        composerFactory.addImport("org.jboss.as.console.client.Console");
        composerFactory.addImport("org.jboss.as.console.client.ProductConfig");

        composerFactory.addImport("java.util.*");

        // Interfaces
        composerFactory.addImplementedInterface("org.jboss.as.console.client.ProductConfig");

        // SourceWriter
        SourceWriter sourceWriter = composerFactory.createSourceWriter(context, printWriter);

        // ctor
        generateConstructor(sourceWriter);

        // Methods
        generateMethods(sourceWriter, context);

        // close generated class
        sourceWriter.outdent();
        sourceWriter.println("}");

        // commit generated class
        context.commit(logger, printWriter);
    }

    private void generateConstructor(SourceWriter sourceWriter)
    {
        // start constructor source generation
        sourceWriter.println("public " + className + "() { ");
        sourceWriter.indent();
        sourceWriter.println("super();");
        sourceWriter.outdent();
        sourceWriter.println("}");
    }


    private void generateMethods(SourceWriter sourceWriter, GeneratorContext context) throws Throwable
    {
        PropertyOracle propertyOracle = context.getPropertyOracle();
        String devHostProperty =
                       propertyOracle.getConfigurationProperty("console.dev.host").getValues().get(0);
        String consoleDevHost = (devHostProperty!= null) ? devHostProperty : "127.0.0.1";

        // most of the config attributes are by default empty
        // they need be overriden by custom gwt.xml descriptor on a project/product level
        sourceWriter.println("public String getCoreVersion() { ");
        sourceWriter.indent();
        sourceWriter.println("return org.jboss.as.console.client.Build.VERSION;");
        sourceWriter.outdent();
        sourceWriter.println("}");

        sourceWriter.println("public String getDevHost() { ");
        sourceWriter.indent();
        sourceWriter.println("return \""+consoleDevHost+"\";");
        sourceWriter.outdent();
        sourceWriter.println("}");
    }
}
