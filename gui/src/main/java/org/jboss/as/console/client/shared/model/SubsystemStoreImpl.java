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

package org.jboss.as.console.client.shared.model;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 3/18/11
 */
public class SubsystemStoreImpl implements SubsystemLoader {

    private DispatchAsync dispatcher;
    private BeanFactory factory;
    private ApplicationProperties bootstrap;

    @Inject
    public SubsystemStoreImpl(DispatchAsync dispatcher, ApplicationProperties bootstrap, BeanFactory factory) {
        this.dispatcher = dispatcher;
        this.bootstrap = bootstrap;
        this.factory = factory;
    }

    @Override
    public void loadSubsystems(final String profileName, final AsyncCallback<List<SubsystemRecord>> callback) {

        assert profileName!=null && !profileName.equals("") : "Illegal profile name: "+profileName;

        ModelNode extensionOp = new ModelNode();
        extensionOp.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        extensionOp.get(CHILD_TYPE).set("extension");
        extensionOp.get(RECURSIVE).set(true);
        extensionOp.get(ADDRESS).setEmptyList();

        ModelNode subsysOp = new ModelNode();
        subsysOp.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        subsysOp.get(CHILD_TYPE).set("subsystem");
        subsysOp.get(ADDRESS).setEmptyList();

        if(bootstrap.getProperty(ApplicationProperties.STANDALONE).equals("false"))
        {
            subsysOp.get(ADDRESS).add("profile", profileName);
        }


        ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(ADDRESS).setEmptyList();

        List<ModelNode> steps = new ArrayList<>();
        steps.add(extensionOp);
        steps.add(subsysOp);

        composite.get(STEPS).set(steps);

        dispatcher.execute(new DMRAction(composite), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    callback.onFailure(new RuntimeException(
                            "Failed to load profile "+profileName+": " +response.getFailureDescription()
                    ));
                }
                else
                {
                    Map<String, int[]> extensionMap = new HashMap<String, int[]>();

                    ModelNode extensionResult = response.get(RESULT).get("step-1").get(RESULT);

                    List<Property> extensions = extensionResult.asPropertyList();
                    for (Property extension : extensions) {
                        List<Property> subsystems = extension.getValue().get("subsystem").asPropertyList();
                        for (Property subsystem : subsystems) {
                            String subsystemName = subsystem.getName();
                            ModelNode value = subsystem.getValue();
                            int major = value.get("management-major-version").asInt();
                            int minor = value.get("management-minor-version").asInt();
                            int micro = value.get("management-micro-version").asInt();

                            extensionMap.put(subsystemName, new int[] {major, minor, micro});
                        }
                    }

                    ModelNode subsystemResult = response.get(RESULT).get("step-2").get(RESULT);
                    List<ModelNode> subsystems = subsystemResult.asList();
                    List<SubsystemRecord> records = new ArrayList<SubsystemRecord>(subsystems.size());
                    for(int i=0; i<subsystems.size(); i++)
                    {
                        ModelNode model = subsystems.get(i);
                        String subsystemName = model.asString();
                        int[] version = extensionMap.get(subsystemName);

                        SubsystemRecord record = factory.subsystem().as();
                        record.setKey(subsystemName);
                        record.setMajor(version[0]);
                        record.setMinor(version[1]);
                        record.setMicro(version[2]);
                        records.add(record);
                    }

                    callback.onSuccess(records);


                }
            }
        });
    }
}
