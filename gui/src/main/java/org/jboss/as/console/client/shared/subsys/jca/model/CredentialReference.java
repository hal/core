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
package org.jboss.as.console.client.shared.subsys.jca.model;

import org.jboss.as.console.client.widgets.forms.Binding;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public interface CredentialReference {

    String getStore();
    void setStore(String newValue);

    String getAlias();
    void setAlias(String newValue);

    String getType();
    void setType(String newValue);

    @Binding(detypedName = "clear-text")
    String getClearText();
    void setClearText(String newValue);


}
