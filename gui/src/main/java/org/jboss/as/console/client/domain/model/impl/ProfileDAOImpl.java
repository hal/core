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

package org.jboss.as.console.client.domain.model.impl;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.ProfileDAO;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 3/18/11
 */
public class ProfileDAOImpl implements ProfileDAO {

    private DispatchAsync dispatcher;
    private BeanFactory factory;


    @Inject
    public ProfileDAOImpl(DispatchAsync dispatcher, BeanFactory factory) {
        this.dispatcher = dispatcher;
        this.factory = factory;
    }

    @Override
    public void loadProfiles(final AsyncCallback<List<ProfileRecord>> callback) {

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("profile");
        operation.get(ADDRESS).setEmptyList();

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Console.error("Failed to load profiles:"+response.getFailureDescription());
                }
                else
                {

                    List<Property> payload = response.get("result").asPropertyList();

                    List<ProfileRecord> records = new ArrayList<ProfileRecord>(payload.size());
                    for(Property item : payload)
                    {

                        ModelNode atts = item.getValue();
                        ProfileRecord record = factory.profile().as();
                        record.setName(item.getName());

                        if(atts.hasDefined("includes"))
                        {
                            List<ModelNode> included = atts.get("includes").asList();
                            List<String> inclusions = new LinkedList<String>();
                            for (ModelNode inc : included) {
                                inclusions.add(inc.asString());
                            }
                            record.setIncludes(inclusions);
                        }
                        else
                        {
                            record.setIncludes(Collections.EMPTY_LIST);
                        }
                        records.add(record);
                    }


                    callback.onSuccess(records);
                }
            }
        });

    }
}
