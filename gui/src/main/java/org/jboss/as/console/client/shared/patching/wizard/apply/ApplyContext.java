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
package org.jboss.as.console.client.shared.patching.wizard.apply;

import java.util.List;

import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Hidden;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.wizard.CommonPatchContext;
import org.jboss.dmr.client.ModelNode;

/**
 * Simple 'struct' for data exchange between apply wizard steps.
 *
 * @author Harald Pehl
 */
public class ApplyContext extends CommonPatchContext {

    // initial data
    final String patchUrl;
    // process slip
    FormPanel form;
    Hidden operation;
    String filename;
    PatchInfo patchInfo;
    boolean conflict;
    boolean patchFailed;
    String patchFailedDetails;
    boolean overrideConflict;

    public ApplyContext(final boolean standalone, final String host, final List<String> runningServers,
            final ModelNode patchAddress, final String patchUrl) {

        super(standalone, host, runningServers, patchAddress);
        this.patchUrl = patchUrl;

        this.form = null;
        this.operation = null;
        this.patchInfo = null;
        this.conflict = false;
        this.patchFailed = false;
        this.patchFailedDetails = null;
        this.overrideConflict = false;
    }
}
