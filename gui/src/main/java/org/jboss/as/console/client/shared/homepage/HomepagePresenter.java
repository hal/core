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

        void addInfoBoxes(List<InfoBox> infoBoxes);

        void addContentBoxes(List<ContentBox> contentBoxes);

        void addSidebarSections(List<SidebarSection> sidebarSections);
    }


    public static final Object SECTION_INFO_SLOT = new Object();
    public static final Object CONTENT_BOX_SLOT = new Object();
    public static final Object SIDEBAR_SLOT = new Object();
    private final List<InfoBox> infoBoxes;
    private final List<ContentBox> contentBoxes;
    private final List<SidebarSection> sidebarSections;
    private final Header header;

    @Inject
    public HomepagePresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final BootstrapContext bootstrapContext, final ProductConfig productConfig, final Header header) {

        super(eventBus, view, proxy, MainLayoutPresenter.TYPE_MainContent);
        this.infoBoxes = setupInfoBoxes(bootstrapContext.isStandalone());
        this.contentBoxes = setupContentBoxes(bootstrapContext.isStandalone());
        this.sidebarSections = setupSidebarSection(productConfig.getProfile());
        this.header = header;
    }

    private List<InfoBox> setupInfoBoxes(final boolean standalone) {
        List<InfoBox> infoBoxes = new LinkedList<InfoBox>();

        if (standalone) {
            infoBoxes.add(new InfoBox(NameTokens.serverConfig, Console.CONSTANTS.common_label_configuration(),
                    Console.CONSTANTS.section_configuration_intro()));
            infoBoxes.add(new InfoBox(NameTokens.StandaloneRuntimePresenter, "Runtime",
                    Console.CONSTANTS.section_runtime_intro()));
        } else {
            infoBoxes.add(new InfoBox(NameTokens.ProfileMgmtPresenter,
                    Console.CONSTANTS.common_label_configuration(), Console.CONSTANTS.section_configuration_intro()));
            infoBoxes.add(new InfoBox(NameTokens.HostMgmtPresenter, "Domain",
                    Console.CONSTANTS.section_domain_intro()));
            infoBoxes.add(new InfoBox(NameTokens.DomainRuntimePresenter, "Runtime",
                    Console.CONSTANTS.section_runtime_intro()));
        }
        infoBoxes.add(new InfoBox(NameTokens.AdministrationPresenter, "Administration",
                Console.CONSTANTS.section_administration_intro()));

        return infoBoxes;
    }

    private List<ContentBox> setupContentBoxes(final boolean standalone) {
        List<ContentBox> contentBoxes = new LinkedList<ContentBox>();

        if (standalone) {
            contentBoxes.add(new ContentBox("NewDeployment",
                    Console.CONSTANTS.content_box_new_deployment_title(),
                    Console.MESSAGES.content_box_new_deployment_body_standalone(),
                    Console.CONSTANTS.content_box_new_deployment_link(), NameTokens.DeploymentBrowserPresenter));
            contentBoxes.add(new ContentBox("Datasources",
                    Console.CONSTANTS.content_box_create_datasource_title(),
                    Console.MESSAGES.content_box_create_datasource_body_standalone(),
                    "Datasources", NameTokens.DataSourcePresenter));
            contentBoxes.add(new ContentBox("ApplyPath",
                    Console.CONSTANTS.content_box_apply_patch_title(),
                    Console.MESSAGES.content_box_apply_patch_body_standalone(),
                    "Patch Management", NameTokens.PatchingPresenter));
        } else {
            contentBoxes.add(new ContentBox("NewDeployment",
                    Console.CONSTANTS.content_box_new_deployment_title(),
                    Console.MESSAGES.content_box_new_deployment_body_domain(),
                    Console.CONSTANTS.content_box_new_deployment_link(), NameTokens.DeploymentsPresenter));
            contentBoxes.add(new ContentBox("Datasources",
                    Console.CONSTANTS.content_box_create_datasource_title(),
                    Console.MESSAGES.content_box_create_datasource_body_domain(),
                    "Datasources", NameTokens.DataSourcePresenter));
            contentBoxes.add(new ContentBox("Topology",
                    Console.CONSTANTS.content_box_topology_title(),
                    Console.MESSAGES.content_box_topology_body(),
                    Console.CONSTANTS.content_box_topology_link(), NameTokens.Topology));
            contentBoxes.add(new ContentBox("CreateServerGroup",
                    Console.CONSTANTS.content_box_create_server_group_title(),
                    Console.MESSAGES.content_box_create_server_group_body(),
                    Console.CONSTANTS.content_box_create_server_group_link(), NameTokens.ServerGroupPresenter));
            contentBoxes.add(new ContentBox("ApplyPath",
                    Console.CONSTANTS.content_box_apply_patch_title(),
                    Console.MESSAGES.content_box_apply_patch_body_domain(),
                    "Patch Management", NameTokens.PatchingPresenter));
        }
        contentBoxes.add(new ContentBox("Administration",
                Console.CONSTANTS.content_box_role_assignment_title(),
                Console.MESSAGES.content_box_role_assignment_body(),
                Console.CONSTANTS.content_box_role_assignment_link(), NameTokens.RoleAssignmentPresenter));

        return contentBoxes;
    }

    private List<SidebarSection> setupSidebarSection(ProductConfig.Profile profile) {
        List<SidebarSection> sections = new LinkedList<SidebarSection>();

        if (profile == COMMUNITY) {
            SidebarSection general = new SidebarSection(Console.CONSTANTS.sidebar_general_resources());
            general.addLink("http://wildfly.org/", Console.CONSTANTS.sidebar_wilfdfly_home_text());
            general.addLink("https://docs.jboss.org/author/display/WFLY8/Documentation",
                    Console.CONSTANTS.sidebar_wilfdfly_documentation_text());
            general.addLink("https://docs.jboss.org/author/display/WFLY8/Admin+Guide",
                    Console.CONSTANTS.sidebar_admin_guide_text());
            general.addLink("http://wildscribe.github.io/index.html", Console.CONSTANTS.sidebar_model_reference_text());
            general.addLink("https://issues.jboss.org/browse/WFLY", Console.CONSTANTS.sidebar_wildfly_issues_text());
            general.addLink("http://wildfly.org/news/", Console.CONSTANTS.sidebar_latest_news());
            sections.add(general);

            SidebarSection help = new SidebarSection(Console.CONSTANTS.sidebar_get_help());
            help.addLink("http://www.jboss.org/jdf/", Console.CONSTANTS.sidebar_tutorials_text());
            help.addLink("https://community.jboss.org/en/wildfly?view=discussions",
                    Console.CONSTANTS.sidebar_user_forums_text());
            help.addLink("irc://freenode.org/#wildfly", Console.CONSTANTS.sidebar_irc_text());
            help.addLink("https://lists.jboss.org/mailman/listinfo/wildfly-dev",
                    Console.CONSTANTS.sidebar_developers_mailing_list_text());
            sections.add(help);

        } else if (profile == PRODUCT) {
            SidebarSection general = new SidebarSection(Console.CONSTANTS.sidebar_general_resources());
            general.addLink(Console.CONSTANTS.sidebar_eap_documentation_link(),
                    Console.CONSTANTS.sidebar_eap_documentation_text());
            general.addLink(Console.CONSTANTS.sidebar_learn_more_eap_link(),
                    Console.CONSTANTS.sidebar_learn_more_eap_text());
            general.addLink(Console.CONSTANTS.sidebar_trouble_ticket_link(),
                    Console.CONSTANTS.sidebar_trouble_ticket_text());
            general.addLink(Console.CONSTANTS.sidebar_training_link(), Console.CONSTANTS.sidebar_training_text());
            sections.add(general);

            SidebarSection developer = new SidebarSection(Console.CONSTANTS.sidebar_developer_resources());
            developer.addLink(Console.CONSTANTS.sidebar_tutorials_link(), Console.CONSTANTS.sidebar_tutorials_text());
            developer.addLink(Console.CONSTANTS.sidebar_eap_community_link(),
                    Console.CONSTANTS.sidebar_eap_community_text());
            sections.add(developer);

            SidebarSection operational = new SidebarSection(Console.CONSTANTS.sidebar_operational_resources());
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
        getView().addInfoBoxes(infoBoxes);
        getView().addContentBoxes(contentBoxes);
        getView().addSidebarSections(sidebarSections);
    }

    @Override
    protected void onReset() {
        super.onReset();
        header.highlight(NameTokens.HomepagePresenter);
    }
}
