/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.shared.subsys.elytron.ui;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class DirContextView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private ElytronGenericResourceView dirContextView;

    public DirContextView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }

    public Widget asWidget() {

        ResourceDescription keyStoreDescription = rootDescription.getChildDescription("dir-context");
        
        dirContextView = new ElytronGenericResourceView(circuit, keyStoreDescription, securityContext, "Dir Context", 
                ElytronStore.DIR_CONTEXT_ADDRESS);
        // necessary, as there are some read-only attributes, they should be filled at creation time.
        dirContextView.setOnAddFormRequiredOnly(false);
        
        PagedView panel = new PagedView(true);
        panel.addPage("Dir Context", dirContextView.asWidget());
        // default page
        panel.showPage(0);
        
        return panel.asWidget();
    }
    
    public void updateKeyStore(List<Property> keyStore) {
        dirContextView.update(keyStore);
    }


}
