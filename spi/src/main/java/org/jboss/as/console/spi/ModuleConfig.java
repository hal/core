/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.spi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * @author Harald Pehl
 */
public class ModuleConfig {

    static final String MODULE_PACKAGE = "org.jboss.as.console.composite";

    private final Filer filer;
    private final String template;
    private final String filename;

    public ModuleConfig(final Filer filer, final String template, final String filename) {
        this.filer = filer;
        this.template = template;
        this.filename = filename;
    }

    public void writeModuleFile(Set<String> inheritFrom, Map<String, String> properties) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("modules", inheritFrom);
            model.put("properties", properties);

            FileObject sourceFile = filer.createResource(StandardLocation.SOURCE_OUTPUT, MODULE_PACKAGE, filename);
            OutputStream output = sourceFile.openOutputStream();
            new TemplateProcessor().process(template, model, output);
            output.flush();
            output.close();
            System.out.println("Written GWT module to " + sourceFile.toUri().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file", e);
        }
    }
}
