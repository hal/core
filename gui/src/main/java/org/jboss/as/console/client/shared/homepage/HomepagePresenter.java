/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.shared.homepage;

import static org.jboss.as.console.client.ProductConfig.Profile.COMMUNITY;
import static org.jboss.as.console.client.ProductConfig.Profile.PRODUCT;

import java.util.LinkedList;
import java.util.List;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;

/**
 * @author Harald Pehl
 */
public class HomepagePresenter extends Presenter<HomepagePresenter.MyView, HomepagePresenter.MyProxy> {

    @NoGatekeeper
    @ProxyCodeSplit
    @NameToken(NameTokens.HomepagePresenter)
    public interface MyProxy extends Proxy<HomepagePresenter>, Place {}


    public interface MyView extends View {

        void addSectionInfo(SectionInfo sectionInfo);

        void addContentBox(ContentBox contentBox);

        void addSidebarSection(SidebarSectionData sidebarSection);
    }


    public static final Object SECTION_INFO_SLOT = new Object();
    public static final Object CONTENT_BOX_SLOT = new Object();
    public static final Object SIDEBAR_SLOT = new Object();
    private final List<SectionInfo> sectionInfos;
    private final List<ContentBox> contentBoxes;
    private final List<SidebarSectionData> sidebarSections;
    private final Header header;

    @Inject
    public HomepagePresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final BootstrapContext bootstrapContext, final ProductConfig productConfig, final Header header) {

        super(eventBus, view, proxy, MainLayoutPresenter.TYPE_MainContent);
        this.sectionInfos = setupSectionInfos(bootstrapContext.isStandalone());
        this.contentBoxes = setupContentBoxes(bootstrapContext.isStandalone());
        this.sidebarSections = setupSidebarSection(productConfig.getProfile());
        this.header = header;
    }

    private List<SectionInfo> setupSectionInfos(final boolean standalone) {
        List<SectionInfo> sectionInfos = new LinkedList<SectionInfo>();

        if (standalone) {
            sectionInfos.add(new SectionInfo(NameTokens.serverConfig, Console.CONSTANTS.common_label_configuration(),
                    Console.CONSTANTS.section_configuration_intro()));
            sectionInfos.add(new SectionInfo(NameTokens.StandaloneRuntimePresenter, "Runtime",
                    Console.CONSTANTS.section_runtime_intro()));
        } else {
            sectionInfos.add(new SectionInfo(NameTokens.ProfileMgmtPresenter,
                    Console.CONSTANTS.common_label_configuration(), Console.CONSTANTS.section_configuration_intro()));
            sectionInfos.add(new SectionInfo(NameTokens.HostMgmtPresenter, "Domain",
                    Console.CONSTANTS.section_domain_intro()));
            sectionInfos.add(new SectionInfo(NameTokens.DomainRuntimePresenter, "Runtime",
                    Console.CONSTANTS.section_runtime_intro()));
        }
        sectionInfos.add(new SectionInfo(NameTokens.AdministrationPresenter, "Administration",
                Console.CONSTANTS.section_administration_intro()));

        return sectionInfos;
    }

    private List<ContentBox> setupContentBoxes(final boolean standalone) {
        List<ContentBox> contentBoxes = new LinkedList<ContentBox>();

        if (standalone) {
            contentBoxes.add(new SimpleContentBox("NewDeployment",
                    Console.CONSTANTS.content_box_new_deployment_title(),
                    Console.MESSAGES.content_box_new_deployment_body_standalone(),
                    Console.CONSTANTS.content_box_new_deployment_link(), NameTokens.DeploymentBrowserPresenter));
            contentBoxes.add(new SimpleContentBox("Datasources",
                    Console.CONSTANTS.content_box_create_datasource_title(),
                    Console.MESSAGES.content_box_create_datasource_body_standalone(),
                    "Datasources", NameTokens.DataSourcePresenter));
            contentBoxes.add(new SimpleContentBox("ApplyPath",
                    Console.CONSTANTS.content_box_apply_patch_title(),
                    Console.MESSAGES.content_box_apply_patch_body_standalone(),
                    "Patch Management", NameTokens.PatchingPresenter));
        } else {
            contentBoxes.add(new SimpleContentBox("NewDeployment",
                    Console.CONSTANTS.content_box_new_deployment_title(),
                    Console.MESSAGES.content_box_new_deployment_body_domain(),
                    Console.CONSTANTS.content_box_new_deployment_link(), NameTokens.DeploymentsPresenter));
            contentBoxes.add(new SimpleContentBox("Datasources",
                    Console.CONSTANTS.content_box_create_datasource_title(),
                    Console.MESSAGES.content_box_create_datasource_body_domain(),
                    "Datasources", NameTokens.DataSourcePresenter));
            contentBoxes.add(new SimpleContentBox("Topology",
                    Console.CONSTANTS.content_box_topology_title(),
                    Console.MESSAGES.content_box_topology_body(),
                    Console.CONSTANTS.content_box_topology_link(), NameTokens.Topology));
            contentBoxes.add(new SimpleContentBox("CreateServerGroup",
                    Console.CONSTANTS.content_box_create_server_group_title(),
                    Console.MESSAGES.content_box_create_server_group_body(),
                    Console.CONSTANTS.content_box_create_server_group_link(), NameTokens.ServerGroupPresenter));
            contentBoxes.add(new SimpleContentBox("ApplyPath",
                    Console.CONSTANTS.content_box_apply_patch_title(),
                    Console.MESSAGES.content_box_apply_patch_body_domain(),
                    "Patch Management", NameTokens.PatchingPresenter));
        }
        contentBoxes.add(new SimpleContentBox("Administration",
                Console.CONSTANTS.content_box_role_assignment_title(),
                Console.MESSAGES.content_box_role_assignment_body(),
                Console.CONSTANTS.content_box_role_assignment_link(), NameTokens.RoleAssignmentPresenter));

        return contentBoxes;
    }

    private List<SidebarSectionData> setupSidebarSection(ProductConfig.Profile profile) {
        List<SidebarSectionData> sections = new LinkedList<SidebarSectionData>();

        if (profile == COMMUNITY) {
            SidebarSectionData general = new SidebarSectionData(Console.CONSTANTS.sidebar_general_resources());
            general.addLink("http://wildfly.org/", Console.CONSTANTS.sidebar_wilfdfly_home_text());
            general.addLink("https://docs.jboss.org/author/display/WFLY8/Documentation",
                    Console.CONSTANTS.sidebar_wilfdfly_documentation_text());
            general.addLink("https://docs.jboss.org/author/display/WFLY8/Admin+Guide",
                    Console.CONSTANTS.sidebar_admin_guide_text());
            general.addLink("http://wildscribe.github.io/index.html", Console.CONSTANTS.sidebar_model_reference_text());
            general.addLink("https://issues.jboss.org/browse/WFLY", Console.CONSTANTS.sidebar_wildfly_issues_text());
            general.addLink("http://wildfly.org/news/", Console.CONSTANTS.sidebar_latest_news());
            sections.add(general);

            SidebarSectionData help = new SidebarSectionData(Console.CONSTANTS.sidebar_get_help());
            help.addLink("http://www.jboss.org/jdf/", Console.CONSTANTS.sidebar_tutorials_text());
            help.addLink("https://community.jboss.org/en/wildfly?view=discussions",
                    Console.CONSTANTS.sidebar_user_forums_text());
            help.addLink("irc://freenode.org/#wildfly", Console.CONSTANTS.sidebar_irc_text());
            help.addLink("https://lists.jboss.org/mailman/listinfo/wildfly-dev",
                    Console.CONSTANTS.sidebar_developers_mailing_list_text());
            sections.add(help);

        } else if (profile == PRODUCT) {
            SidebarSectionData general = new SidebarSectionData(Console.CONSTANTS.sidebar_general_resources());
            general.addLink(Console.CONSTANTS.sidebar_eap_documentation_link(),
                    Console.CONSTANTS.sidebar_eap_documentation_text());
            general.addLink(Console.CONSTANTS.sidebar_learn_more_eap_link(),
                    Console.CONSTANTS.sidebar_learn_more_eap_text());
            general.addLink(Console.CONSTANTS.sidebar_trouble_ticket_link(),
                    Console.CONSTANTS.sidebar_trouble_ticket_text());
            general.addLink(Console.CONSTANTS.sidebar_training_link(), Console.CONSTANTS.sidebar_training_text());
            sections.add(general);

            SidebarSectionData developer = new SidebarSectionData(Console.CONSTANTS.sidebar_developer_resources());
            developer.addLink(Console.CONSTANTS.sidebar_tutorials_link(), Console.CONSTANTS.sidebar_tutorials_text());
            developer.addLink(Console.CONSTANTS.sidebar_eap_community_link(),
                    Console.CONSTANTS.sidebar_eap_community_text());
            sections.add(developer);

            SidebarSectionData operational = new SidebarSectionData(Console.CONSTANTS.sidebar_operational_resources());
            operational.addLink(Console.CONSTANTS.sidebar_eap_configurations_link(),
                    Console.CONSTANTS.sidebar_eap_configurations_text());
            operational.addLink(Console.CONSTANTS.sidebar_knowledgebase_link(),
                    Console.CONSTANTS.sidebar_knowledgebase_text());
            operational
                    .addLink(Console.CONSTANTS.sidebar_consulting_link(), Console.CONSTANTS.sidebar_consulting_text());
            sections.add(operational);
        }
        return sections;
    }

    @Override
    protected void onBind() {
        super.onBind();
        for (SectionInfo sectionInfo : sectionInfos) {
            getView().addSectionInfo(sectionInfo);
        }
        for (ContentBox contentBox : contentBoxes) {
            getView().addContentBox(contentBox);
        }
        for (SidebarSectionData sidebarSection : sidebarSections) {
            getView().addSidebarSection(sidebarSection);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        header.highlight(NameTokens.HomepagePresenter);
    }
}
