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
package org.jboss.dmr.client;

import org.jboss.dmr.client.dispatch.Result;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * A static DMR response for unit tests.
 *
 * @author Harald Pehl
 */
public class StaticDmrResponse extends DMRResponse {

    /**
     * Creates a successful DMR response with the given result as payload.
     * The returned model contains an outcome and the specified result:
     * <pre>
     * {
     *     "outcome" =&gt; "success",
     *     "result" =&gt; ${result}
     * }
     * </pre>
     */
    public static Result<ModelNode> ok(ModelNode result) {
        return new StaticDmrResponse(result);
    }

    /**
     * Creates a failed DMR response with the given description.
     * The returned model contains an outcome and the specified failure description:
     * <pre>
     * {
     *     "outcome" =&gt; "failed",
     *     "failure-description" =&gt; "${failureDescription}"
     * }
     * </pre>
     */
    public static Result<ModelNode> failure(String failureDescription) {
        return new StaticDmrResponse(failureDescription);
    }

    private final ModelNode result;

    private StaticDmrResponse(ModelNode result) {
        super("POST", "", "UTF-8");
        this.result = new ModelNode();
        this.result.get(OUTCOME).set(SUCCESS);
        this.result.get(RESULT).set(result);
    }

    private StaticDmrResponse(String failureDescription) {
        super("POST", "", "UTF-8");
        result = new ModelNode();
        result.get(OUTCOME).set("failed");
        result.get(FAILURE_DESCRIPTION).set(failureDescription);
    }

    @Override
    public ModelNode get() {
        return result;
    }
}
