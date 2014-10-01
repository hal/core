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
package org.jboss.as.console.client.shared.subsys.logging;

import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.FormValidator;

import java.util.List;

/**
* @author Harald Pehl
*/
class VerifyRelativeFilePath implements FormValidator {

    @Override
    public void validate(List<FormItem> items, FormValidation outcome) {
        FormItem filePath = findItem(items, "filePath");
        FormItem fileRelativeTo = findItem(items, "fileRelativeTo");

        if (filePath != null && fileRelativeTo != null) {
            if (isRelative(getValue(filePath)) && getValue(fileRelativeTo).length() == 0) {
                outcome.addError(fileRelativeTo.getName());
                fileRelativeTo.setErroneous(true);
            } else {
                fileRelativeTo.setErroneous(false);
            }
        }
    }

    private FormItem findItem(List<FormItem> items, String name) {
        for (FormItem item : items) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }

    private String getValue(FormItem item) {
        if (item != null && item.getValue() != null) {
            return item.getValue().toString();
        }
        return "";
    }

    private boolean isRelative(String path) {
        boolean absolute = path.startsWith("/") || path.startsWith("\\\\") || path.contains(":\\");
        return !absolute;
    }
}
