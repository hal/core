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
package org.jboss.as.console.client.shared.util;

/**
 * @author Harald Pehl
 */
public final class Trim {

    private static final int MAX_LENGTH = 15;
    private static final String ELLIPSIS = "...";

    private Trim() {
    }

    public static String abbreviateMiddle(String str) {
        return abbreviateMiddle(str, MAX_LENGTH);
    }

    public static String abbreviateMiddle(String str, int maxLength) {
        if (str == null || maxLength >= str.length()) {
            return str;
        }

        final int targetSting = maxLength - ELLIPSIS.length();
        final int startOffset = targetSting / 2 + targetSting % 2;
        final int endOffset = str.length() - targetSting / 2;

        return str.substring(0, startOffset) + ELLIPSIS + str.substring(endOffset);
    }
}
