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
package org.jboss.as.console.client.tools.modelling.workbench.repository;

import org.jboss.as.console.mbui.model.StereoTypes;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Mapping;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.Select;
import org.useware.kernel.model.structure.Trigger;
import org.useware.kernel.model.structure.builder.Builder;

import static org.useware.kernel.model.structure.TemporalOperator.Choice;

import static org.jboss.as.console.mbui.model.StereoTypes.*;

/**
 * @author Heiko Braun
 * @date 08/27/2013
 */
public class IOSubsystemExample implements Sample
{

    private Dialog dialog;

    public IOSubsystemExample() {
        this.dialog = build();
    }

    @Override
    public String getName()
    {
        return "IO Subsystem";
    }

    @Override
    public Dialog getDialog() {
        return dialog;
    }

    public Dialog build()
    {
        String ns = "org.jboss.io";

        // common mappings
        Mapping global = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=io");

        Mapping workerMapping = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=io/worker=*")
                .addAttributes("entity.key");

        Mapping bufferPoolMapping = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=io/buffer-pool=*")
                .addAttributes("entity.key");

        // top level containers
        Container overview = new Container(ns, "io-subsystem", "IO Subsystem", EditorPanel);
        Container editors = new Container(ns, "editors", "Editors", Choice);

        // worker master detail
        Select workerList = new Select(ns, "worker", "Worker");
        Container workerDetails = new Container(ns, "workerDetails", "Details", Form);

        // buffer pool master detail
        Select bufferList = new Select(ns, "buffer", "Buffer Pool");
        Container bufferDetails = new Container(ns, "bufferDetails", "Details", Form);

        DMRMapping workerInstance = new DMRMapping().setAddress("/{selected.profile}/subsystem=io/worker={selected.entity}");
        DMRMapping bufferInstance = new DMRMapping().setAddress("/{selected.profile}/subsystem=io/buffer-pool={selected.entity}");

        // structure & mapping

        InteractionUnit root = new Builder()
                .start(overview).mappedBy(global)
                    .start(editors)
                        .start(new Container(ns,"workers", "Worker"))


                .start(new Container<StereoTypes>(ns, "worker-tools", "Tools", Toolstrip))
                    .mappedBy(workerInstance)
                    .add(new Trigger(
                            QName.valueOf(ns+".worker:add"),
                            QName.valueOf("org.jboss.as:resource-operation#add"),
                            "Add"))
                            .mappedBy(workerMapping)

                    .add(new Trigger(
                            QName.valueOf(ns+"worker:remove"),
                            QName.valueOf("org.jboss.as:resource-operation#remove"),
                            "Remove"))
                .end()

                .add(workerList).mappedBy(workerMapping)
                            .add(workerDetails).mappedBy(workerInstance)
                        .end()
                        .start(new Container(ns,"buffers", "Buffer Pools"))

                .start(new Container<StereoTypes>(ns, "buffer-tools", "Tools", Toolstrip))
                    .mappedBy(bufferInstance)
                        .add(new Trigger(
                                QName.valueOf(ns+".buffer:add"),
                                QName.valueOf("org.jboss.as:resource-operation#add"),
                                "Add"))
                        .mappedBy(bufferPoolMapping)

                    .add(new Trigger(
                            QName.valueOf(ns+"buffer:remove"),
                            QName.valueOf("org.jboss.as:resource-operation#remove"),
                            "Remove"))
                .end()

                .add(bufferList).mappedBy(bufferPoolMapping)
                            .add(bufferDetails).mappedBy(bufferInstance)
                        .end()
                    .end()
                .end()
                .build();

        Dialog dialog = new Dialog(QName.valueOf("org.jboss.as:io-subsystem"), root);
        return dialog;
    }



}

