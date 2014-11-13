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
package org.jboss.as.console.tools.scanner;

import org.reflections.Reflections;

import java.util.Arrays;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * General purpose tool which scans classes in {@link Main#BASE_PACKAGE} and generates reports to stdout.
 *
 * @author Harald Pehl
 */
public class Main {

    public final static String BASE_PACKAGE = "org.jboss.as.console.client";

    public static void main(String[] args) {
        new Main().run(args);
    }

    private final SortedMap<String, Generator> generators;

    public Main() {
        this.generators = new TreeMap<>();

        Reflections reflections = new Reflections(Main.class.getPackage().getName());
        Set<Class<? extends Generator>> generatorTypes = reflections.getSubTypesOf(Generator.class);
        for (Class<? extends Generator> generatorType : generatorTypes) {
            try {
                Generator generator = generatorType.newInstance();
                generators.put(generator.getName(), generator);
            } catch (InstantiationException | IllegalAccessException e) {
                System.err.printf("Cannot create predefined generator '%s': %s\n", generatorType.getName(), e.getMessage());
            }
        }
    }

    private void run(String[] args) {
        if (!validate(args)) {
            usage();
            System.exit(1);
        }

        String[] generatorArgs = new String[]{};
        Generator generator = generators.get(args[1]);
        if (args.length > 2) {
            generatorArgs = Arrays.copyOfRange(args, 2, args.length);
        }
        if (!generator.validate(generatorArgs)) {
            usage();
            System.exit(1);
        }

        generator.generateContent(generatorArgs);
    }

    private boolean validate(String[] args) {
        return args.length >= 2 && args[0].equals("-g") && generators.containsKey(args[1]);
    }

    private void usage() {
        System.err.printf("Usage:\n\n\t%s -g <generator> [generator args]\n\n", Main.class.getName());
        System.err.printf("Where generator is one of:\n\n");
        for (Generator generator : generators.values()) {
            System.err.printf("\t%s: %s\n", generator.getName(), generator.getDescription());
        }
    }
}
