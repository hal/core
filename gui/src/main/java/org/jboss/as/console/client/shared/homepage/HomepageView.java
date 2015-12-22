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

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import elemental.dom.Element;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.v3.elemento.Elements;

import static org.jboss.as.console.client.ProductConfig.Profile.COMMUNITY;
import static org.jboss.as.console.client.ProductConfig.Profile.PRODUCT;

/**
 * @author Harald Pehl
 */
public class HomepageView extends SuspendableViewImpl implements HomepagePresenter.MyView {

    private final ProductConfig productConfig;
    private final BootstrapContext bootstrapContext;
    private final UIConstants constants;
    private final UIMessages messages;

    @Inject
    public HomepageView(final ProductConfig productConfig, final BootstrapContext bootstrapContext,
            final UIConstants constants, UIMessages messages) {
        this.productConfig = productConfig;
        this.bootstrapContext = bootstrapContext;
        this.constants = constants;
        this.messages = messages;
    }

    @Override
    public Widget createWidget() {
        ScrollPanel scroller = new ScrollPanel();

        boolean standalone = bootstrapContext.isStandalone();
        boolean community = productConfig.getProfile() == COMMUNITY;
        boolean su = bootstrapContext.isAdmin() || bootstrapContext.isSuperUser();
        String name = community ? "WildFly" : "JBoss EAP";

        Element header;
        HomepageModule deploymentModule;
        HomepageModule configurationModule;
        HomepageModule runtimeModule;
        HomepageModule accessControlModule = HomepageModule.EMPTY; // to get rid of warning "might not be initialized"
        HomepageModule patchingModule = HomepageModule.EMPTY;
        Element help;

        if (community) {
            header = new Elements.Builder()
                    .div().css("eap-home-title")
                    .h(1).innerText("WildFly").end()
                    .end().build();
        } else {
            // @formatter:off
            Elements.Builder headerBuilder = new Elements.Builder()
                .div().css("eap-home-title")
                    .p()
                        .span().innerText(constants.homepage_new_to_eap() + " ").end()
                        .a()
                            .css("clickable")
                            .rememberAs("tour")
                            .innerText(constants.homepage_take_a_tour())
                        .end()
                    .end()
                    .h(1).innerText("Red Hat JBoss Enterprise Application Platform").end()
                .end();
            // @formatter:on
            wireTour(headerBuilder.referenceFor("tour"));
            header = headerBuilder.build();
        }

        if (standalone) {
            HomepageSection deploymentSection = new HomepageSection(NameTokens.StandaloneDeploymentFinder,
                    constants.homepage_deployments_section(),
                    constants.homepage_deployments_standalone_step_intro(),
                    constants.homepage_deployments_standalone_step_1(),
                    constants.homepage_deployments_step_enable());
            deploymentModule = new HomepageModule(NameTokens.StandaloneDeploymentFinder,
                    "images/homepage/deployments.png",
                    "Deployments",
                    constants.homepage_deployments_sub_header(),
                    deploymentSection);

            HomepageSection configurationSection = new HomepageSection(NameTokens.ServerProfile,
                    constants.homepage_configuration_section(),
                    constants.homepage_configuration_step_intro(),
                    constants.homepage_configuration_standalone_step1(),
                    constants.homepage_configuration_step2(),
                    constants.homepage_configuration_step3());
            HomepageSection jmsSection = new HomepageSection(NameTokens.ServerProfile,
                    constants.homepage_jms_section(),
                    constants.homepage_jms_step_intro(),
                    constants.homepage_jms_standalone_step1(),
                    constants.homepage_jms_step2(),
                    constants.homepage_jms_step3());
            jmsSection.toggle();
            configurationModule = new HomepageModule(NameTokens.ServerProfile,
                    "images/homepage/configuration.png",
                    "Configuration",
                    constants.homepage_configuration_standalone_sub_header(),
                    configurationSection, jmsSection);

            HomepageSection runtimeSection = new HomepageSection(NameTokens.StandaloneRuntimePresenter,
                    constants.homepage_runtime_standalone_section(),
                    constants.homepage_runtime_step_intro(),
                    constants.homepage_runtime_standalone_step1(),
                    constants.homepage_runtime_standalone_step2());
            runtimeModule = new HomepageModule(NameTokens.StandaloneRuntimePresenter,
                    "images/homepage/runtime.png",
                    "Runtime",
                    constants.homepage_runtime_standalone_sub_header(),
                    runtimeSection);

        } else {
            HomepageSection deploymentSection = new HomepageSection(NameTokens.DomainDeploymentFinder,
                    constants.homepage_deployments_section(),
                    constants.homepage_deployments_domain_step_intro(),
                    constants.homepage_deployments_domain_step_1(),
                    constants.homepage_deployments_domain_step_2(),
                    constants.homepage_deployments_step_enable());
            deploymentModule = new HomepageModule(NameTokens.DomainDeploymentFinder,
                    "images/homepage/deployments.png",
                    "Deployments",
                    constants.homepage_deployments_sub_header(),
                    deploymentSection);

            HomepageSection configurationSection = new HomepageSection(NameTokens.ProfileMgmtPresenter,
                    constants.homepage_configuration_section(),
                    constants.homepage_configuration_step_intro(),
                    constants.homepage_configuration_domain_step1(),
                    constants.homepage_configuration_step2(),
                    constants.homepage_configuration_step3());
            HomepageSection jmsSection = new HomepageSection(NameTokens.ProfileMgmtPresenter,
                    constants.homepage_jms_section(),
                    constants.homepage_jms_step_intro(),
                    constants.homepage_jms_domain_step1(),
                    constants.homepage_jms_step2(),
                    constants.homepage_jms_step3());
            jmsSection.toggle();
            configurationModule = new HomepageModule(NameTokens.ProfileMgmtPresenter,
                    "images/homepage/configuration.png",
                    "Configuration",
                    constants.homepage_configuration_domain_sub_header(),
                    configurationSection, jmsSection);

            HomepageSection serverGroupSection = new HomepageSection(NameTokens.HostMgmtPresenter,
                    constants.homepage_runtime_domain_server_group_section(),
                    constants.homepage_runtime_domain_server_group_step_intro(),
                    constants.homepage_runtime_domain_server_group_step1(),
                    constants.homepage_runtime_domain_server_group_step2());
            HomepageSection createServerSection = new HomepageSection(NameTokens.HostMgmtPresenter,
                    constants.homepage_runtime_domain_create_server_section(),
                    constants.homepage_runtime_domain_create_server_step_intro(),
                    constants.homepage_runtime_domain_create_server_step1(),
                    constants.homepage_runtime_domain_create_server_step2());
            createServerSection.toggle();
            HomepageSection monitorServerSection = new HomepageSection(NameTokens.HostMgmtPresenter,
                    constants.homepage_runtime_domain_monitor_server_section(),
                    constants.homepage_runtime_step_intro(),
                    constants.homepage_runtime_domain_monitor_server_step1(),
                    constants.homepage_runtime_domain_monitor_server_step2());
            monitorServerSection.toggle();
            runtimeModule = new HomepageModule(NameTokens.HostMgmtPresenter,
                    "images/homepage/runtime.png",
                    "Runtime",
                    constants.homepage_runtime_domain_sub_header(),
                    serverGroupSection, createServerSection, monitorServerSection);
        }

        if (su) {
            HomepageSection accessControlSection = new HomepageSection(NameTokens.AccessControlFinder,
                    constants.homepage_access_control_section(),
                    constants.homepage_access_control_step_intro(),
                    constants.homepage_access_control_step1(),
                    constants.homepage_access_control_step2());
            accessControlModule = new HomepageModule(
                    NameTokens.AccessControlFinder,
                    "images/homepage/access_control.png",
                    "Access Control",
                    constants.homepage_access_control_sub_header(),
                    accessControlSection);

            HomepageSection patchingSection;
            if (standalone) {
                patchingSection = new HomepageSection(NameTokens.PatchingPresenter,
                        constants.homepage_patching_section(),
                        messages.homepage_patching_standalone_step_intro(name),
                        constants.homepage_patching_step1(),
                        constants.homepage_patching_step_apply());
            } else {
                patchingSection = new HomepageSection(NameTokens.PatchingPresenter,
                        constants.homepage_patching_section(),
                        messages.homepage_patching_domain_step_intro(name),
                        constants.homepage_patching_step1(),
                        constants.homepage_patching_domain_step2(),
                        constants.homepage_patching_step_apply());
            }
            patchingModule = new HomepageModule(NameTokens.PatchingPresenter,
                    "images/homepage/patching.png",
                    "Patching",
                    messages.homepage_patching_sub_header(name),
                    patchingSection);
        }

        // @formatter:off
        Elements.Builder helpBuilder = new Elements.Builder()
            .div().css("eap-home-col")
                .div().css("eap-home-module-icon")
                    .add("img").attr("src", "images/homepage/help.png")
                .end()
                .div().css("eap-home-module-container")
                    .div().css("eap-home-module-header")
                        .h(2).innerText(constants.homepage_help_need_help()).end()
                    .end()
                    .div().css("eap-home-module-col")
                        .p().innerText(constants.homepage_help_general_resources()).end()
                        .ul().rememberAs("generalResources").end()
                    .end()
                    .div().css("eap-home-module-col")
                        .p().innerText(constants.homepage_help_get_help()).end()
                        .ul().rememberAs("getHelp").end()
                    .end()
                .end()
            .end();
        // @formatter:on
        Element generalResources = helpBuilder.referenceFor("generalResources");
        Element getHelp = helpBuilder.referenceFor("getHelp");
        if (community) {
            generalResources.appendChild(helpLink("http://www.wildfly.org",
                    constants.homepage_help_wilfdfly_home_text()));
            generalResources.appendChild(helpLink("https://docs.jboss.org/author/display/WFLY10/Documentation",
                    constants.homepage_help_wilfdfly_documentation_text()));
            generalResources.appendChild(helpLink("https://docs.jboss.org/author/display/WFLY10/Admin+Guide",
                    constants.homepage_help_admin_guide_text()));
            generalResources.appendChild(helpLink("http://wildscribe.github.io/index.html",
                    constants.homepage_help_model_reference_text()));
            generalResources.appendChild(helpLink("https://issues.jboss.org/browse/WFLY",
                    constants.homepage_help_wildfly_issues_text()));
            generalResources.appendChild(helpLink("http://wildfly.org/news/", constants.homepage_help_latest_news()));

            getHelp.appendChild(helpLink("http://www.jboss.org/developer-materials/",
                    constants.homepage_help_tutorials_text()));
            getHelp.appendChild(helpLink("https://community.jboss.org/en/wildfly?view=discussions",
                    constants.homepage_help_user_forums_text()));
            getHelp.appendChild(helpLink("irc://freenode.org/#wildfly", constants.homepage_help_irc_text()));
            getHelp.appendChild(helpLink("https://lists.jboss.org/mailman/listinfo/wildfly-dev",
                    constants.homepage_help_developers_mailing_list_text()));
        } else {
            generalResources.appendChild(helpLink(constants.homepage_help_eap_documentation_link(),
                    constants.homepage_help_eap_documentation_text()));
            generalResources.appendChild(helpLink(constants.homepage_help_learn_more_eap_link(),
                    constants.homepage_help_learn_more_eap_text()));
            generalResources.appendChild(helpLink(constants.homepage_help_trouble_ticket_link(),
                    constants.homepage_help_trouble_ticket_text()));
            generalResources.appendChild(helpLink(constants.homepage_help_training_link(),
                    constants.homepage_help_training_text()));

            getHelp.appendChild(helpLink(constants.homepage_help_tutorials_link(),
                    constants.homepage_help_tutorials_text()));
            getHelp.appendChild(helpLink(constants.homepage_help_eap_community_link(),
                    constants.homepage_help_eap_community_text()));
            getHelp.appendChild(helpLink(constants.homepage_help_eap_configurations_link(),
                    constants.homepage_help_eap_configurations_text()));
            getHelp.appendChild(helpLink(constants.homepage_help_knowledgebase_link(),
                    constants.homepage_help_knowledgebase_text()));
            getHelp.appendChild(helpLink(constants.homepage_help_consulting_link(),
                    constants.homepage_help_consulting_text()));
        }
        help = helpBuilder.build();

        Elements.Builder rootBuilder = new Elements.Builder().div()
                .div().css("eap-home-row")
                .add(header)
                .add(deploymentModule.asElement())
                .add(configurationModule.asElement())
                .end();
        if (su) {
            rootBuilder.div().css("eap-home-row")
                    .add(runtimeModule.asElement())
                    .add(accessControlModule.asElement())
                    .end()
                    .div().css("eap-home-row")
                    .add(patchingModule.asElement())
                    .add(help)
                    .end();
        } else {
            rootBuilder.div().css("eap-home-row")
                    .add(runtimeModule.asElement())
                    .add(help)
                    .end();
        }
        Element root = rootBuilder.end().build();
        scroller.add(Elements.asWidget(root));
        return scroller;
    }

    private Element helpLink(final String href, final String text) {
        return new Elements.Builder().li().a().attr("href", href).innerText(text).end().end().build();
    }

    native void wireTour(Element element) /*-{
        var that = this;
        element.onclick = function() {
            that.@org.jboss.as.console.client.shared.homepage.HomepageView::launchGuidedTour()();
        };
    }-*/;

    private void launchGuidedTour() {
        if (productConfig.getProfile() == PRODUCT) {
            GuidedTourHelper.open();
        }
    }
}
