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
package org.jboss.as.console.client.shared.subsys.activemq;

import java.util.Map;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.SECURITY_DOMAIN;

public class ProviderView implements MessagingAddress {

    private final static String[] COMMON = new String[]{
            "management-address",
            "management-notification-address",
            "statistics-enabled",
            "thread-pool-max-size",
            "scheduled-thread-pool-max-size",
            "transaction-timeout",
            "transaction-timeout-scan-period",
            "wild-card-routing-enabled",
            "persistence-enabled",
            "persist-id-cache",
    };

    private final static String[] SECURITY = new String[]{
            "security-domain",
            "security-enabled",
            "security-invalidation-interval",
            "cluster-user",
            "cluster-password"
    };

    private final static String[] JOURNAL = new String[]{
            "journal-buffer-size",
            "journal-buffer-timeout",
            "journal-compact-min-files",
            "journal-compact-percentage",
            "journal-file-size",
            "journal-max-io",
            "journal-min-files",
            "journal-sync-non-transactional",
            "journal-sync-transactional",
            "journal-type",
            "journal-datasource",
            "journal-database",
            "journal-messages-table",
            "journal-large-messages-table",
            "journal-bindings-table",
            "create-journal-dir",
    };

    private final static String[] DIRECTORY = new String[]{
            "path",
            "relative-to"
    };

    private final ActivemqFinder presenter;
    private ModelNodeFormBuilder.FormAssets commonForm;
    private ModelNodeFormBuilder.FormAssets secForm;
    private ModelNodeFormBuilder.FormAssets journalForm;
    private ModelNodeFormBuilder.FormAssets bindingsDirForm;
    private ModelNodeFormBuilder.FormAssets journalDirForm;
    private ModelNodeFormBuilder.FormAssets largeMessagesDirForm;
    private ModelNodeFormBuilder.FormAssets pagingDirForm;
    private Property provider;
    private HTML title;

    public ProviderView(ActivemqFinder presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {
        SecurityContext securityContext = presenter.getSecurityFramework()
                .getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(PROVIDER_TEMPLATE);
        ResourceDescription pathDefinition = presenter.getDescriptionRegistry().lookup(PATH_TEMPLATE);

        FormCallback callback = new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeset) {
                presenter.onSaveProvider(provider, changeset);
            }

            @Override
            public void onCancel(Object entity) {
            }
        };

        // common
        commonForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .include(COMMON)
                .setSecurityContext(securityContext)
                .addFactory(
                        "thread-pool-max-size",
                        new ModelNodeFormBuilder.FormItemFactory(){
                            @Override
                            public FormItem create(Property attr) {
                                FormItem formItem = new NumberBoxItem(attr.getName(), "Thread Pool Size", true);
                                formItem.setRequired(false);
                                formItem.setEnabled(true);
                                return formItem;
                            }
                        }
                ).build();
        commonForm.getForm().setToolsCallback(callback);

        // security
        secForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(SECURITY)
                .addFactory("security-domain", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("security-domain", "Security domain", true,
                            Console.MODULES.getCapabilities().lookup(SECURITY_DOMAIN));
                    return suggestionResource.buildFormItem();
                })
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();
        secForm.getForm().setToolsCallback(callback);

        // journal
        journalForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(JOURNAL)
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();
        journalForm.getForm().setToolsCallback(callback);

        // bindings directory
        bindingsDirForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(DIRECTORY)
                .setResourceDescription(pathDefinition)
                .setSecurityContext(securityContext).build();
        bindingsDirForm.getForm().setToolsCallback(createPathCallback("bindings-directory"));

        // journal directory
        journalDirForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(DIRECTORY)
                .setResourceDescription(pathDefinition)
                .setSecurityContext(securityContext).build();
        journalDirForm.getForm().setToolsCallback(createPathCallback("journal-directory"));

        // large messages directory
        largeMessagesDirForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(DIRECTORY)
                .setResourceDescription(pathDefinition)
                .setSecurityContext(securityContext).build();
        largeMessagesDirForm.getForm().setToolsCallback(createPathCallback("large-messages-directory"));

        // paging directory
        pagingDirForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(DIRECTORY)
                .setResourceDescription(pathDefinition)
                .setSecurityContext(securityContext).build();
        pagingDirForm.getForm().setToolsCallback(createPathCallback("paging-directory"));

        title = new HTML();
        title.setStyleName("content-header-label");

        OneToOneLayout layoutBuilder = new OneToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(title)
                .setDescription(definition.get("description").asString())
                .addDetail(Console.CONSTANTS.common_label_attributes(), commonForm.asWidget())
                .addDetail("Security", secForm.asWidget())
                .addDetail("Journal", journalForm.asWidget())
                .addDetail("Bindings Directory", bindingsDirForm.asWidget())
                .addDetail("Journal Directory", journalDirForm.asWidget())
                .addDetail("Large Messages Directory", largeMessagesDirForm.asWidget())
                .addDetail("Paging Directory", pagingDirForm.asWidget());
        return layoutBuilder.build();
    }

    public void updateFrom(Property provider) {
        this.provider = provider;

        title.setHTML("JMS Messaging Provider: " + provider.getName());
        commonForm.getForm().edit(provider.getValue());
        secForm.getForm().edit(provider.getValue());
        journalForm.getForm().edit(provider.getValue());
        ModelNode path = provider.getValue().get("path");
        if (path.isDefined()) {
            bindingsDirForm.getForm().edit(path.get("bindings-directory"));
            journalDirForm.getForm().edit(path.get("journal-directory"));
            largeMessagesDirForm.getForm().edit(path.get("large-messages-directory"));
            pagingDirForm.getForm().edit(path.get("paging-directory"));
        }
    }

    private FormCallback createPathCallback(String directory) {
        return new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeset) {
                presenter.onSaveProvider(provider, changeset, PROVIDER_TEMPLATE.append("path=" + directory));
            }

            @Override
            public void onCancel(Object entity) {
            }
        };
    }
}
