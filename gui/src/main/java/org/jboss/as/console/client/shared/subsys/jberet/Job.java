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
package org.jboss.as.console.client.shared.subsys.jberet;

import org.jboss.dmr.client.ModelNode;

/**
 * @author Claudio Miranda
 */
public class Job extends ModelNode {

    public Job(ModelNode modelNode) {
        set(modelNode);
    }

    public String getName() {
        return failSafeGet("name");
    }

    public void setName(final String name) {
        get("name").set(name);
    }

    public void setJobXmlName(final String name) {
        get("job-xml-name").set(name);
    }

    public String getJobXmlName() {
        return failSafeGet("job-xml-name");
    }

    public String getDeploymentName() {
        return failSafeGet("deployment");
    }

    public void setDeploymentName(final String deploymentName) {
        get("deployment").set(deploymentName);
    }

    public String getSubdeploymentName() {
        return failSafeGet("subdeployment");
    }

    public void setSubdeploymentName(final String subdeploymentName) {
        get("subdeployment").set(subdeploymentName);
    }

    public String getInstanceId() {
        return failSafeGet("instance-id");
    }

    public void setExecutionId(String execId) {
        get("execution-id").set(execId);
    }

    public String getExecutionId() {
        return failSafeGet("execution-id");
    }

    public String getCurrentStatus() {
        return failSafeGet("batch-status");
    }

    public String getExitStatus() {
        return failSafeGet("exit-status");
    }

    public String getCreateTime() {
        return failSafeGet("create-time");
    }

    public String getEndTime() {
        return failSafeGet("end-time");
    }

    public String getStartTime() {
        return failSafeGet("start-time");
    }

    public String getLastUpdatedTime() {
        return failSafeGet("last-updated-time");
    }

    private String failSafeGet(String attribute) {
        String val = "";
        if (hasDefined(attribute)) {
            val = get(attribute).asString(); 
        }
        return val;
    }

    @Override
    public String toString() {
        return "Job [deployment=" + getDeploymentName() + ", job=" +getName() + ", instance-id=" + getInstanceId() 
                + ", batch-status=" + getCurrentStatus() + ", exit-status=" + getExitStatus() 
                + ", create-time=" + getCreateTime() + ", end-time=" + getEndTime() + ", start-time=" + getStartTime() +"]";
    }
}
