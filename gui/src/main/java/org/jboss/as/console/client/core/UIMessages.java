/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.core;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * @author Heiko Braun
 * @author David Bosschaert
 * @date 5/2/11
 */
public interface UIMessages extends Messages {
    // @formatter:off
    SafeHtml access_control_provider();
    String addDeploymentToServerGroup(String serverGroup);
    String added(String name);
    String addGroup();
    String addGroupToRole();
    String addingFailed(String name);
    String addMember();
    String addUser();
    String addUserToRole();
    String administration_members(String name);
    String administration_scoped_role_in_use(int usage);
    String allPrincipalsAreAlreadyMembersOf(String name);
    String allRolesAreAlreadyAssignedTo(String nameAndRealm);
    String assignedToServerGroups(String name, String serverGroups);
    String assignment();
    String assignmentExcludePrincipalToRole(String userOrGroup, String principal, String role);
    String assignmentExcludeRoleToPrincipal(String role, String principal);
    String assignmentIncludePrincipalToRole(String userOrGroup, String principal, String role);
    String assignmentIncludeRoleToPrincipal(String role, String principal);
    String assignmentSuccessfullyDisabled(String name);
    String assignmentSuccessfullyEnabled(String name);
    String AssignmentSuccessfullyUnassigned(String name);
    String assignRole();
    String available(String s);
    String bs_interface_warning(String url);
    String chooseServerGroupsForAssigning(String name);
    String cloneProfile(String name);
    String common_validation_heapSize();
    String common_validation_notEmptyNoSpace();
    String common_validation_portOffsetUndefined(String errMessage);
    String contentAlreadyAssigned(String name);
    String contentFailedToAssignToServerGroups(String name);
    String contentFailedToUnassignFromServerGroups(String name);
    String contentNotAssignedToServerGroup(String name);
    String contentSuccessfullyAssignedToServerGroups(String name);
    String contentSuccessfullyUnassignedFromServerGroups(String name);
    String contentSuccessfullyUploaded(String name);
    String copyGroupDescription(String name);
    String createResource(String type);
    String createTitle(String itemName);
    String datasourceDescription();
    SafeHtml datasourceSummaryDescription();
    String deleteConfirm(String name);
    String deleted(String name);
    String deleteServerConfig();
    String deleteServerGroup();
    String deleteServerGroupConfirm(String groupName);
    String deleteTitle(String name);
    String deletionFailed(String name);
    String deploymentInUse(String name);
    String deploymentSuccessfullyDeployed(String name);
    String deploymentSuccessfullyReplaced(String name);
    String disableConfirm(String entity);
    String editRole();
    String enableConfirm(String entity);
    String environment_description();
    String excludedFrom();
    String excludeMember();
    String excludeRole();
    String extensions_description();
    String failed(String p0);
    String failedToCreateResource(String resource);
    String failedToDetectJdbcDriver(String reason);
    String failedToLoadCacheContainer(String container);
    String failedToLoadMessagingServerNames();
    String failedToLoadResource(String s);
    String failedToModifyMessagingProvider(String name);
    String failedToModifyResource(String resource);
    String failedToRemoveMessagingProvider(String name);
    String failedToRemoveResource(String resource);
    String flushConnectionsError(String dsName);
    String flushConnectionsSuccess(String dsName);
    String homepage_patching_domain_step_intro(String name);
    String homepage_patching_standalone_step_intro(String name);
    String homepage_patching_sub_header(String name);
    String membership();
    String messagingProvider();
    String modificationFailed(String name);
    String modified(String name);
    String modify(String name);
    String newMessagingProvider();
    String newTitle(String resourceName);
    String noConfigurableAttributes(String resource);
    String noConfigurableAttributesOnResources(String address);
    String patch_manager_applying_patch_body(String filename);
    SafeHtml patch_manager_desc_product();
    SafeHtml patch_manager_conflict_override_title();
    String patch_manager_error_parse_result(String exception, String payload);
    String patch_manager_restart_needed(String serverOrHost);
    String patch_manager_restart_verify(String host);
    String patch_manager_restart_yes(String serverOrHost);
    String patch_manager_rolling_back_body(String id);
    String patch_manager_stop_server_body(String host);
    String path_description();
    String pleaseChoseanItem();
    String principalSuccessfullyAdded(String userOrGroup);
    String principalSuccessfullyRemovedFromAssignment(String principal, String assignment);
    String profileAlreadyExists(String name);
    String profileUsedBy(String name);
    String providerSettings();
    String reallyRemoveProfile(String name);
    String removeProperty();
    String removePropertyConfirm(String key);
    String resource_already_exists(String name);
    String restartRequired();
    String restartRequiredConfirm();
    String role_is_used_in_assignments();
    String roleSuccessfullyRemovedFrom(String role, String assignment);
    String saved(String name);
    String savedSettings();
    String saveFailed(String name);
    String scopedRoleSuccessfullyAdded(String roleId);
    SafeHtml search_index_pending();
    SafeHtml search_n_results(int n);
    String search_popup_description(String shortcut);
    String securityDomainDescription(String name);
    String server_config_stillRunning(String name);
    String server_reload_confirm(String p0);
    String subsys_configadmin_addNoPIDselected();
    String subsys_configadmin_oneValueRequired();
    String subsys_configadmin_remove();
    String subsys_configadmin_removeConfirm(String pid);
    String subsys_configadmin_removeOnLastValueConfirm(String pid);
    String subsys_jca_err_ds_enabled(String name);
    String subsys_jca_err_ds_notEnabled(String name);
    String subsys_jpa_err_mericDisabled(String s);
    String successDisabled(String entity);
    String successEnabled(String entity);
    String successful(String p0);
    String successfullyAdded(String resource);
    String successfullyAddedMessagingProvider(String name);
    String successfullyAddedServer(String name);
    String successfullyCreated(String resource);
    String successfullyModifiedMessagingProvider(String name);
    String successfullyModifiedResource(String resource);
    String successfullyRemoved(String resource);
    String successfullyRemovedMessagingProvider(String name);
    String successfullyRemovedServer(String serverName);
    SafeHtml testConnectionDomainDescription();
    SafeHtml testConnectionStandaloneDescription();
    String topology_description();
    String transaction_log_description();
    String unassignAssignment(String name);
    String unassignContent(String name);
    String unknown_error();
    String verify_datasource_failed_message(String datasource);
    String verify_datasource_successful_message(String datasource);
    String wantToReloadServerGroup(String name);
    String wantToRestartServerGroup(String name);
    String wantToResumeServerGroup(String name);
    String wantToStartServerGroup(String name);
    String wantToStopServerGroup(String name);
    String xaDatasourceDescription();
    String domainMandatoryForNonExternalProvider();
    // @formatter:on
}
