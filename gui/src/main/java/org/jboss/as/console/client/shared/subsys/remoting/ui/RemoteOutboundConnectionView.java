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
package org.jboss.as.console.client.shared.subsys.remoting.ui;

import java.util.List;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class RemoteOutboundConnectionView extends ConnectionEditor {

    RemoteOutboundConnectionView(final DispatchAsync dispatcher,
            final Dispatcher circuit,
            final SecurityContext securityContext,
            final StatementContext statementContext,
            final AddressTemplate addressTemplate,
            final ResourceDescription resourceDescription, final String title) {
        super(dispatcher, circuit, securityContext, statementContext, addressTemplate, resourceDescription, title);
    }

    @Override
    protected void registerFormValidator(List<FormItem> formItemList, FormValidation formValidation) {
        FormItem authenticationCtxItem = findFormItem(formItemList, "authentication-context");
        FormItem usernameItem = findFormItem(formItemList, "username");
        FormItem securityRealmItem = findFormItem(formItemList, "security-realm");
        FormItem protocolItem = findFormItem(formItemList, "protocol");

        boolean authCtxDefined = !authenticationCtxItem.isUndefined();
        boolean othersDefined = !(usernameItem.isUndefined() && securityRealmItem.isUndefined() && protocolItem.isUndefined());
        if (authCtxDefined && othersDefined) {
            formValidation.addError("authentication-context");
            authenticationCtxItem.setErrMessage("Either authentication-context or username, protocol and security-realm should be filled.");
            authenticationCtxItem.setErroneous(true);
        }
    }

    <T> FormItem<T> findFormItem(List<FormItem> formItems, String name) {
        FormItem selectedFormItem = null;
        for (FormItem formItem : formItems) {
            if (name.equals(formItem.getName())) {
                selectedFormItem = formItem;
                break;
            }
        }
        return selectedFormItem;
    }
}
