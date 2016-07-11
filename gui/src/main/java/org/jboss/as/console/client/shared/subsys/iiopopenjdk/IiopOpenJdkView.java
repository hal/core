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
package org.jboss.as.console.client.shared.subsys.iiopopenjdk;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.MapAttributeAddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.MapAttributePropertyManager;
import org.jboss.as.console.client.v3.widgets.PropertyEditor;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.SECURITY_DOMAIN;
import static org.jboss.as.console.client.shared.subsys.iiopopenjdk.IiopOpenJdkPresenter.IIOP_OPENJDK_SUBSYSTEM_TEMPLATE;

/**
 * @author Harald Pehl
 */
public class IiopOpenJdkView extends SuspendableViewImpl implements IiopOpenJdkPresenter.MyView {

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry descriptionRegistry;

    private IiopOpenJdkPresenter presenter;
    private ModelNodeFormBuilder.FormAssets formAssets;
    private PropertyEditor propertyEditor;

    @Inject
    public IiopOpenJdkView(DispatchAsync dispatcher, StatementContext statementContext,
            SecurityFramework securityFramework, ResourceDescriptionRegistry descriptionRegistry) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;
    }

    @Override
    public void setPresenter(final IiopOpenJdkPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription resourceDescription = descriptionRegistry.lookup(IIOP_OPENJDK_SUBSYSTEM_TEMPLATE);

        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .exclude("properties")
                .addFactory("socket-binding", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("socket-binding", "Socket binding", false,
                            Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("ssl-socket-binding", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("ssl-socket-binding", "Ssl socket binding", false,
                            Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
                    return suggestionResource.buildFormItem();
                })
                .addFactory("security-domain", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("security-domain", "Security domain", false,
                            Console.MODULES.getCapabilities().lookup(SECURITY_DOMAIN));
                    return suggestionResource.buildFormItem();
                })
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext);
        formAssets = builder.build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                presenter.save(changeSet);
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());

        MapAttributePropertyManager propertyManager = new MapAttributePropertyManager(IIOP_OPENJDK_SUBSYSTEM_TEMPLATE,
                "properties", statementContext, dispatcher);
        MapAttributeAddPropertyDialog addDialog = new MapAttributeAddPropertyDialog(propertyManager);
        propertyEditor = new PropertyEditor.Builder(propertyManager)
                .addDialog(addDialog)
                .build();

        return new OneToOneLayout()
                .setTitle("IIOP")
                .setHeadline("IIOP Subsystem")
                .setDescription(Console.CONSTANTS.subsys_iiop_openjdk_desc())
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel)
                .addDetail(Console.CONSTANTS.common_label_properties(), propertyEditor.asWidget())
                .build();
    }

    @Override
    public void update(ModelNode model) {
        formAssets.getForm().edit(model);
        List<Property> properties = model.get("properties").asPropertyList();
        propertyEditor.update(properties);
    }
}
