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
package org.jboss.dmr.client.dispatch.impl;

import com.google.gwt.dom.client.Element;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.Action;
import org.jboss.dmr.client.dispatch.ActionType;

/**
 * @author Harald Pehl
 */
public class UploadAction implements Action<UploadResponse> {

    private final Element fileInput;
    private final ModelNode operation;

    public UploadAction(final Element fileInput, final ModelNode operation) {
        this.fileInput = fileInput;
        this.operation = operation;
    }

    @Override
    public ActionType getType() {
        return ActionType.UPLOAD;
    }

    @Override
    public Object getAddress() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean isSecured() {
        return false;
    }

    public Element getFileInput() {
        return fileInput;
    }

    public ModelNode getOperation() {
        return operation;
    }
}
