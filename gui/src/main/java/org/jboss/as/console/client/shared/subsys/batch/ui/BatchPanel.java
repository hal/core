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
package org.jboss.as.console.client.shared.subsys.batch.ui;

import com.google.gwt.user.client.ui.Composite;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.subsys.batch.BatchPresenter;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

import static org.jboss.as.console.client.shared.subsys.batch.store.BatchStore.BATCH_ADDRESS;
import static org.jboss.as.console.client.shared.subsys.batch.store.BatchStore.JOB_REPOSITORY_ADDRESS;

/**
 * @author Harald Pehl
 */
class BatchPanel extends Composite {

    private final BatchResourceForm batch;
    private final BatchResourceForm jobRepository;

    BatchPanel(final StatementContext statementContext, final SecurityContext securityContext,
               final BatchPresenter presenter) {
        batch = new BatchResourceForm(BATCH_ADDRESS, statementContext, securityContext) {
            @Override
            void onSave(Map<String, Object> changedValues) {
                presenter.modifyBatch(changedValues);
            }
        };
        jobRepository = new BatchResourceForm(JOB_REPOSITORY_ADDRESS, statementContext, securityContext) {
            @Override
            void onSave(Map<String, Object> changedValues) {
                presenter.modifyJobRepository(changedValues);
            }
        };

        OneToOneLayout layoutBuilder = new OneToOneLayout()
                .setPlain(true)
                .setHeadline("Batch Subsystem")
                .setDescription(Console.CONSTANTS.batchSubsystemDescription())
                .addDetail("Repository Type", batch)
                .addDetail("JDBC Job repository", jobRepository);

        initWidget(layoutBuilder.build());
    }

    void updateBatch(ModelNode model) {
        batch.update(model);
    }

    void updateJobRepository(ModelNode model) {
        jobRepository.update(model);
    }
}
