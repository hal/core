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
package org.jboss.as.console.client.v3.deployment.wizard;

import com.google.gwt.user.client.ui.FileUpload;
import org.jboss.as.console.client.shared.deployment.DeploymentReference;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.v3.deployment.Content;
import org.jboss.as.console.client.widgets.forms.UploadForm;

import java.util.ArrayList;
import java.util.List;

/**
 * Common context for the deployment wizards.
 *
 * @author Harald Pehl
 */
public class Context {

    public final boolean standalone;
    public final List<Content> contentRepository;
    public String serverGroup;
    public boolean deployNew;
    public boolean deployExisting;
    public boolean deployUnmanaged;
    public FileUpload fileUpload;
    public UploadForm uploadForm;
    public DeploymentReference upload;
    public Content existingContent;
    public boolean enableExistingContent;
    public DeploymentRecord unmanagedDeployment;

    public Context(final boolean standalone) {
        this.standalone = standalone;
        this.contentRepository = new ArrayList<>();
    }
}
