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
package org.jboss.as.console.client.shared.subsys.activemq.forms;

import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.FormValidator;

import java.util.List;

public class DiscoveryGroupFormValidator implements FormValidator {
    @Override
    public void validate(List<FormItem> formItems, FormValidation outcome) {
        FormItem<String> socketBinding = formItem(formItems, "socket-binding");
        FormItem<String> jGroupsChannel = formItem(formItems, "jgroups-channel");

        if (isEmpty(socketBinding) && isEmpty(jGroupsChannel)) {

            socketBinding.setErrMessage("Either Socket Binding or JGroup Channel has to be set.");
            jGroupsChannel.setErrMessage("Either Socket Binding or JGroup Channel has to be set.");
            socketBinding.setErroneous(true);
            jGroupsChannel.setErroneous(true);

            outcome.addError("socket-binding");
            outcome.addError("jgroups-channel");
        }
        if (!isEmpty(socketBinding) && !isEmpty(jGroupsChannel)) {
            socketBinding.setErrMessage("Only one of Socket Binding/JGroup Channel can be used at the same time.");
            jGroupsChannel.setErrMessage("Only one of Socket Binding/JGroup Channel can be used at the same time.");
            socketBinding.setErroneous(true);
            jGroupsChannel.setErroneous(true);

            outcome.addError("socket-binding");
            outcome.addError("jgroups-channel");
        }
    }
    private boolean isEmpty(FormItem<String> socketBinding) {
        return socketBinding == null || socketBinding.getValue() == null || socketBinding.getValue().length() <= 0;
    }

    private <T> FormItem<T> formItem(List<FormItem> formItems, String name) {
        for (FormItem formItem : formItems) {
            if (name.equals(formItem.getName())) {
                return formItem;
            }
        }
        return null;
    }
}
