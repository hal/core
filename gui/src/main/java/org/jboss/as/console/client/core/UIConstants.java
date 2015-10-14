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

import com.google.gwt.i18n.client.Constants;


/**
 * @author Heiko Braun
 * @author David Bosschaert
 * @date 5/2/11
 */
public interface UIConstants extends Constants {

    String common_error_contentStillAssignedToGroup();

    String common_error_unexpectedHttpResponse();

    String common_error_detailsMissing();

    String common_error_failedToDecode();

    String common_error_deploymentFailed();

    String common_error_unknownError();

    String common_error_failure();

    String common_label_addItem();

    String common_label_addProperty();

    String common_label_hostManagement();

    String common_label_profileManagement();

    String common_label_groupManagement();

    String common_label_runtimeStatus();

    String common_label_serverInstances();

    String common_label_serverGroup();

    String common_label_status();

    String common_label_server();

    String common_label_serverConfig();

    String common_label_serverInstance();

    String common_label_instanceDetails();

    String common_label_virtualMachine();

    String common_label_serverConfigs();

    String common_label_noRecords();

    String common_label_hostConfiguration();

    String common_label_systemProperties();

    String common_label_roles();

    String common_label_socketBindingGroups();

    String common_label_virtualMachines();

    String common_label_paths();

    String common_label_path();

    String common_label_serverGroups();

    String common_label_deployments();

    String common_label_deploymentContent();

    String common_label_manageDeployments();

    String common_label_edit();

    String common_label_save();

    String common_label_delete();

    String common_label_createNewServerConfig();

    String common_label_attributes();

    String common_label_details();

    String common_label_name();

    String common_label_autoStart();

    String common_label_socketBinding();

    String common_label_portOffset();

    String common_label_hosts();

    @Deprecated
    String common_label_profiles();

    String common_label_configuration();

    String common_label_properties();

    String common_label_noRecentMessages();

    String common_label_messageDetailTitle();

    String common_label_messageDetail();

    String common_label_messages();

    String common_label_newServerGroup();

    String common_label_profile();

    String common_label_add();

    String common_label_value();

    String common_label_key();

    String common_label_option();

    String common_label_cancel();

    String common_label_runtimeName();

    String common_label_verifyDeploymentNames();

    String common_label_settings();

    String common_label_generalConfig();

    String common_label_interfaces();

    String common_label_disable();

    String common_label_enable();

    String common_label_enOrDisable();

    String common_label_subsystems();

    String common_label_areYouSure();

    String common_label_addToGroup();

    String common_label_addToGroups();

    String common_label_updateContent();

    String common_label_useFileSystem();

    String common_label_user();

    String common_label_users();

    String common_label_group();

    String common_label_groups();

    String common_label_date();

    String common_label_operations();

    String common_label_exclude();

    // use the term "delete" instead
    @Deprecated
    String common_label_remove();

    String common_label_domain();

    String common_label_upload();

    String common_label_next();

    String common_label_deploymentSelection();

    String common_label_step();

    String common_label_chooseFile();

    String common_label_chooseServerGroup();

    String common_label_finish();

    String common_label_addContent();

    String common_label_contentRepository();

    String common_label_enabled();

    String common_label_lazyActivation();

    String common_label_changeActivation();

    String common_label_success();

    String common_label_reset();

    String common_label_reload();

    String common_label_selectedGroups();

    String subsys_jca_dataSources();

    String subsys_jca_dataSourcesXA();

    String subsys_jca_newDataSource();

    String subsys_jca_dataSource();

    String subsys_jca_existingDataSources();

    String subsys_logging_loggers();

    String subsys_logging_handlers();

    String subsys_logging_handler();

    String subsys_logging_addHandler();

    String subsys_logging_removeHandler();

    String subsys_logging_autoFlush();

    String subsys_logging_encoding();

    String subsys_logging_file();

    String subsys_logging_fileHandlers();

    String subsys_logging_periodic();

    String subsys_logging_periodicRotatingFileHandlers();

    String subsys_logging_periodicSize();

    String subsys_logging_periodicSizeRotatingFileHandlers();

    String subsys_logging_size();

    String subsys_logging_sizeRotatingFileHandlers();

    String subsys_logging_async();

    String subsys_logging_asyncHandlers();

    String subsys_logging_filter();

    String subsys_logging_formatter();

    String subsys_logging_append();

    String subsys_logging_fileRelativeTo();

    String subsys_logging_filePath();

    String subsys_logging_rotateSize();

    String subsys_logging_maxBackupIndex();

    String subsys_logging_target();

    String subsys_logging_type();

    String subsys_logging_logLevel();

    String subsys_logging_overflowAction();

    String subsys_logging_subhandlers();

    String subsys_logging_queueLength();

    String subsys_logging_suffix();

    String subsys_logging_handlerConfigurations();

    String subsys_logging_invalidByteSpec();

    String subsys_logging_className();

    String subsys_logging_module();

    String subsys_logging_handlerProperties();

    String subsys_logging_newHandlerProperty();

    String subsys_logging_useParentHandlers();

    String subsys_logging_category();

    String subsys_logging_console();

    String subsys_logging_consoleHandlers();

    String subsys_logging_custom();

    String subsys_logging_customHandlers();

    String subsys_logging_rootLogger();

    String subsys_deploymentscanner_scanners();

    String subsys_deploymentscanner_relativeTo();

    String subsys_deploymentscanner_scanEnabled();

    String subsys_deploymentscanner_scanInterval();

    String subsys_deploymentscanner_autoDeployZipped();

    String subsys_deploymentscanner_autoDeployExploded();

    String subsys_deploymentscanner_deploymentTimeout();

    String subsys_infinispan_cache_container();

    String subsys_infinispan_cache_containers();

    String subsys_infinispan_cluster();

    String subsys_infinispan_default_cache();

    String subsys_infinispan_jndiName();

    String subsys_infinispan_listenerExecutor();

    String subsys_infinispan_evictionExecutor();

    String subsys_infinispan_replicationQueueExecutor();

    String subsys_infinispan_isolation();

    String subsys_infinispan_striping();

    String subsys_infinispan_aquireTimeout();

    String subsys_infinispan_concurrencyLevel();

    String subsys_infinispan_evictionStrategy();

    String subsys_infinispan_transport();

    String subsys_infinispan_stateTransfer();

    String subsys_infinispan_locking();

    String subsys_infinispan_eviction();

    String subsys_infinispan_expiration();

    String subsys_infinispan_localCache();

    String subsys_infinispan_invalidationCache();

    String subsys_infinispan_replicatedCache();

    String subsys_infinispan_distributedCache();

    String subsys_infinispan_distributed();

    String subsys_infinispan_aliases();

    String subsys_infinispan_transaction();

    String subsys_infinispan_store();

    String subsys_infinispan_file_store();

    String subsys_infinispan_jdbc_store();

    String subsys_infinispan_remote_store();

    String subsys_infinispan_attrs();

    String subsys_infinispan_cache_container_desc();

    String subsys_infinispan_local_cache_desc();

    String subsys_infinispan_invalidation_cache_desc();

    String subsys_infinispan_replicated_cache_desc();

    String subsys_infinispan_distributed_cache_desc();

    String subsys_infinispan_cache_container_clear_caches();

    String subsys_infinispan_local_cache_clear_cache();

    String subsys_naming_jndiView();

    String subsys_naming_jndiBindings();

    String subsys_naming_selectedURI();

    String subsys_configadmin();

    String subsys_configadmin_add();

    String subsys_configadmin_editPID();

    String subsys_configadmin_header();

    String subsys_configadmin_PID();

    String subsys_configadmin_PIDLabel();

    String subsys_configadmin_PIDShort();

    String subsys_configadmin_valueAdd();

    String subsys_configadmin_valuesLabel();

    String subsys_osgi_capabilities();

    String subsys_osgi_capability();

    String subsys_osgi_capabilityAdd();

    String subsys_osgi_capabilityEdit();

    String subsys_osgi_capabilityId();

    String subsys_osgi_capabilityStartLevel();

    String subsys_osgi_framework();

    String subsys_osgi_frameworkConfiguration();

    String subsys_osgi_frameworkHeader();

    String subsys_osgi_frameworkProperties();

    String subsys_osgi_frameworkProperty();

    String subsys_osgi_frameworkPropertyAdd();

    String subsys_osgi_properties();

    String subsys_osgi_bundleID();

    String subsys_osgi_bundleLocation();

    String subsys_osgi_bundleSymbolicName();

    String subsys_osgi_bundleVersion();

    String subsys_osgi_bundleState();

    String subsys_osgi_bundles();

    String subsys_osgi();

    String subsys_threads_sizing();

    String subsys_messaging_jms_provider();

    String subsys_messaging_jms_destinations();

    String subsys_jca_dataSource_registered();

    String subsys_jca_dataSource_configurations();

    String subsys_jca_dataSource_xaprop_help();

    String subsys_jca_ra_configurations();

    String subsys_jca_ra_registered();

    String subsys_jca_dataSource_step1();

    String subsys_jca_dataSource_step2();

    String subsys_jca_dataSource_step3();

    String subsys_jca_dataSource_select_driver();

    String subsys_jca_xadataSource_step1();

    String subsys_jca_xadataSource_step2();

    String subsys_jca_xadataSource_step3();

    String subsys_jca_xadataSource_step4();

    String subsys_jca_ra_step1();

    String subsys_jca_ra_step2();

    String common_label_refresh();

    String common_label_clear();

    String common_label_hostVm();

    String common_label_action();

    String common_label_start();

    String common_label_stop();

    String common_label_type();

    String subsys_jca_dataSource_verify();

    String subsys_ejb3_asyncService();

    String subsys_ejb3_beanPoolTimeout();

    String subsys_ejb3_beanPools();

    String subsys_ejb3_container();

    String subsys_ejb3_defaultResourceAdapter();

    String subsys_ejb3_ejbServices();

    String subsys_ejb3_maxPoolSize();

    String subsys_ejb3_messageDrivenBeanPool();

    String subsys_ejb3_remoteService();

    String subsys_ejb3_remoteServiceConnector();

    String subsys_ejb3_services();

    String subsys_ejb3_singletonAccessTimeout();

    String subsys_ejb3_statefulAccessTimeout();

    String subsys_ejb3_statelessSessionBeanPool();

    String subsys_ejb3_threadPool();

    String subsys_ejb3_threadPoolKeepAliveTime();

    String subsys_ejb3_threadPoolMaxThreads();

    String subsys_ejb3_threadPools();

    String subsys_ejb3_timerService();

    String subsys_ejb3_timerServicePath();

    String subsys_ejb3_timerServiceRelativeTo();

    String subsys_security();

    String subsys_security_domains();

    String subsys_security_deepCopySubjects();

    String subsys_security_cacheType();

    String subsys_security_audit();

    String subsys_security_auditProviderModule();

    String subsys_security_providerModules();

    String subsys_security_codeField();

    String subsys_security_flagField();

    String subsys_security_typeField();

    String subsys_security_authentication();

    String subsys_security_authenticationLoginModule();

    String subsys_security_loginModules();

    String subsys_security_authorization();

    String subsys_security_authorizationPolicy();

    String subsys_security_policies();

    String subsys_security_mapping();

    String subsys_security_mappingModule();

    String subsys_security_modules();

    String subsys_jacorb_printVersion();

    String subsys_jacorb_useIMR();

    String subsys_jacorb_useBOM();

    String subsys_jacorb_cacheTypecodes();

    String subsys_jacorb_cachePOANames();

    String subsys_jacorb_GIOPMinorVersion();

    String subsys_jacorb_retries();

    String subsys_jacorb_retryInterval();

    String subsys_jacorb_clientTimeout();

    String subsys_jacorb_serverTimeout();

    String subsys_jacorb_maxServerConnections();

    String subsys_jacorb_maxManagedBufSize();

    String subsys_jacorb_outbufSize();

    String subsys_jacorb_outbufCacheTimeout();

    String subsys_jacorb_transactions();

    String subsys_jacorb_monitoring();

    String subsys_jacorb_queueWait();

    String subsys_jacorb_queueMin();

    String subsys_jacorb_queueMax();

    String subsys_jacorb_poolSize();

    String subsys_jacorb_security();

    String subsys_jacorb_maxThreads();

    String subsys_jacorb_rootContext();

    String subsys_jacorb_exportCorbaloc();

    String subsys_jacorb_sun();

    String subsys_jacorb_comet();

    String subsys_jacorb_iona();

    String subsys_jacorb_chunkCustomRMIValuetypes();

    String subsys_jacorb_laxBooleanEncoding();

    String subsys_jacorb_indirectionEncodingDisable();

    String subsys_jacorb_strictCheckOnTCCreation();

    String subsys_jacorb_supportSSL();

    String subsys_jacorb_securityDomain();

    String subsys_jacorb_addComponentViaInterceptor();

    String subsys_jacorb_clientSupports();

    String subsys_jacorb_clientRequires();

    String subsys_jacorb_serverSupports();

    String subsys_jacorb_serverRequires();

    String subsys_jacorb_orbTab();

    String subsys_jacorb_initializersTab();

    String subsys_jacorb_poaTab();

    String subsys_jacorb_namingTab();

    String subsys_jacorb_interoperabilityTab();

    String subsys_jacorb_securityTab();

    String common_label_serverGroupConfigurations();

    String common_label_serverGroupDeployments();

    String common_label_host();

    String common_label_item();

    String subsys_logging_customHandlers_desc();

    String subsys_logging_rootLogger_desc();

    String subsys_logging_fileHandlers_desc();

    String subsys_logging_periodicRotatingFileHandlers_desc();

    String subsys_logging_periodicSizeRotatingFileHandlers_desc();

    String subsys_logging_sizeRotatingFileHandlers_desc();

    String subsys_logging_asyncHandlers_desc();

    String subsys_logging_consoleHandlers_desc();

    String subsys_logging_loggers_desc();

    String subsys_threads_factory_desc();

    String subsys_threads_queueless_desc();

    String subsys_threads_blocking_queueless_desc();

    String subsys_threads_bounded_desc();

    String subsys_threads_blocking_bounded_desc();

    String subsys_threads_unbounded_desc();

    String subsys_threads_scheduled_desc();

    String subsys_jmx_desc();

    String subsys_jca_common_config_desc();

    String subsys_jca_boostrap_config_desc();

    String subsys_jca_workmanager_config_desc();

    String common_label_back();

    String common_label_view();

    String common_label_selection();

    String subsys_jca_threadpool_config_desc();

    String subsys_jca_error_default_workmanager_deletion();

    String subsys_jca_error_pool_removal();

    String subsys_jca_error_pool_removal_desc();

    String subsys_jca_error_pool_exist();

    String subsys_jca_error_pool_exist_desc();

    String subsys_jca_error_context_removal();

    String subsys_jca_error_context_removal_desc();

    String subsys_jca_dataSources_desc();

    String subsys_jca_xadataSources_desc();

    String subsys_jca_datasource_error_loadDriver();

    String subsys_jca_datasource_error_loadDriver_desc();

    String subsys_jca_error_datasource_notenabled();

    String subsys_jca_resource_adapter_desc();

    String subsys_jca_ra_connection_desc();

    String subsys_jca_adminobject_desc();

    String common_label_advanced();

    String subsys_messaging_jms_provider_desc();

    String subsys_messaging_jms_desc();

    String subsys_ejb3_tab_container();

    String subsys_ejb3_tab_services();

    String subsys_ejb3_tab_beanpools();

    String subsys_ejb3_tab_threadpools();

    String subsys_ejb3_container_desc();

    String subsys_ejb3_services_desc();

    String subsys_ejb3_beanpools_desc();

    String subsys_ejb3_threadpools_desc();

    String subsys_ee_desc();

    String subys_tx_desc();

    String subsys_jpa_desc();

    String subsys_jacorb_desc();

    String subsys_security_desc();

    String subsys_security_domains_desc();

    String subsys_security_authentication_desc();

    String subsys_security_authorization_desc();

    String subsys_security_audit_desc();

    String subsys_security_mapping_desc();

    String subsys_web_desc();

    String subsys_ws_desc();

    String subsys_ws_provider();

    String subsys_ws_endpoint_desc();

    String subsys_ws_endpoints();

    String common_label_setDefault();

    String common_serverGroups_desc();

    String interfaces_err_inetAddress_set();

    String interfaces_err_wildcard_nor_address_set();

    String interfaces_err_wildcard_set();

    String interfaces_err_loopback_set();

    String interfaces_err_loopback_nor_address_set();

    String interfaces_err_loopback_address_set();

    String interfaces_err_nic_set();

    String interfaces_err_nicmatch_set();

    String interfaces_err_not_set();

    String interfaces_desc();

    String common_socket_bindings_desc();

    String common_label_newGroup();

    String properties_global_desc();

    String common_label_active();

    String server_configuration_changed();

    String server_instance_reloadRequired();

    String server_instance_servers_needRestart();

    String server_instance_servers_needReload();

    String subsys_jpa_deployment_desc();

    String subsys_jpa_basicMetric_desc();

    String subsys_jpa_puList_desc();

    String subsys_web_socketInUse();

    String hosts_jvm_desc();

    String hosts_jvm_title();

    String hosts_jvm_err_deleteDefault();

    String common_serverConfig_desc();

    String host_interfaces_desc();

    String host_properties_desc();

    String server_instances_desc();

    String common_label_plaseWait();

    String common_label_requestProcessed();

    String common_err_server_not_active();

    String subsys_jca_dataSource_metric_desc();

    String subsys_messaging_queue_metric_desc();

    String subys_tx_metric_desc();

    String subsys_messaging_topic_metric_desc();

    String subys_web_metric_desc();

    String server_reload_title();

    String server_reload_desc();

    String server_config_uptodate();

    String server_config_desc();

    String server_instance_pleaseSelect();

    String common_label_logout();

    String logout_confirm();

    String subsys_mail_session_desc();

    String subsys_mail_server_desc();

    String subsys_jgroups_session_desc();

    String subsys_jgroups_transport_desc();

    String subsys_jgroups_protocol_desc();

    String common_label_append();

    String subsys_jgroups_step2();

    String subsys_jgroups_step1();

    String subsys_jgroups_err_protocols_required();

    String subsys_modcluster_desc();

    String subsys_jca_err_prop_required();

    String help_need_help();

    String help_close_help();

    String common_label_copy();

    String common_label_export();

    String common_label_replace();

    String common_label_assign();

    String common_label_done();

    String commom_label_filter();

    String common_label_probe();

    String common_label_recover();

    String administration_assignment_user_group_desc();

    String administration_assignment_realm_desc();

    String administration_assignment_type_desc();

    String administration_assignment_roles_desc();

    String administration_audit_log_desc();

    String administration_add_scoped_role();

    String administration_standard_roles_desc();

    String administration_scoped_roles_desc();

    String administration_scoped_role_base_role_desc();

    String administration_scoped_role_scope_desc();

    String administration_role_include_all_desc();

    String administration_group_assignment();

    String administration_user_assignment();

    String administration_members();

    String administration_add_user_assignment();

    String administration_add_group_assignment();

    String administration_available_roles();

    String administration_assigned_roles();

    String administration_excluded_roles();

    String common_label_basedOn();

    String unauthorized();

    String unauthorized_desc();

    String duplicate_mail_server_type();

    String duplicate_data_source_name();

    String duplicate_data_source_jndi();

    String subsys_jca_datasource_error_load();

    String verify_datasource_failed_header();

    String verify_datasource_successful_header();

    String verify_datasource_internal_error();

    String verify_datasource_disabled();

    String verify_datasource_no_running_servers();

    String verify_datasource_dependent_error();

    String patch_manager_latest();

    String patch_manager_apply_new();

    String patch_manager_rollback();

    String patch_manager_recently();

    String patch_manager_error();

    String patch_manager_stop_server_title();

    String patch_manager_stop_server_yes();

    String patch_manager_stop_server_no();

    String patch_manager_select_patch_title();

    String patch_manager_select_patch_body();

    String patch_manager_stopping_servers_body();

    String patch_manager_possible_actions();

    String patch_manager_conflict_title();

    String patch_manager_conflict_body();

    String patch_manager_conflict_cancel_title();

    String patch_manager_conflict_cancel_body();

    String patch_manager_conflict_override_body();

    String patch_manager_conflict_override_check();

    String patch_manager_error_title();

    String patch_manager_apply_error_body();

    String patch_manager_apply_error_cancel_title();

    String patch_manager_apply_error_cancel_body();

    String patch_manager_apply_error_select_title();

    String patch_manager_apply_error_select_body();

    String patch_manager_show_details();

    String patch_manager_hide_details();

    String patch_manager_stop_server_error();

    String patch_manager_stop_server_timeout();

    String patch_manager_stop_server_unknown_error();

    String patch_manager_stop_server_error_continue_title();

    String patch_manager_stop_server_error_continue_body();

    String patch_manager_stop_server_error_cancel_title();

    String patch_manager_stop_server_error_cancel_body();

    String patch_manager_select_file();

    String patch_manager_restart_pending();

    String patch_manager_restart_timeout();

    String patch_manager_restart_error();

    String patch_manager_desc_community();

    String patch_manager_toolstrip_desc();

    String patch_manager_patch_information();

    String patch_manager_applied_at();

    String patch_manager_apply_patch();

    String patch_manager_apply_new_wizard_error();

    String patch_manager_rollback_wizard_error();

    String patch_manager_stop_server_question_for_apply();

    String patch_manager_stop_server_question_for_rollback();

    String patch_manager_servers_shutdown();

    String patch_manager_select_patch_upload();

    String patch_manager_update();

    String patch_manager_restart_now();

    String patch_manager_restart_no();

    String patch_manager_applied_success();

    String patch_manager_rollback_options_title();

    String patch_manager_rollback_options_body();

    String patch_manager_rollback_options_reset_configuration();

    String patch_manager_rollback_options_reset_configuration_desc();

    String patch_manager_rollback_options_override_all();

    String patch_manager_rollback_options_override_all_desc();

    String patch_manager_rollback_confirm_title();

    String patch_manager_rollback_confirm_body();

    String patch_manager_rolled_back_success_title();

    String patch_manager_rolled_back_success_body();

    String patch_manager_rollback_error_body();

    String patch_manager_rollback_error_cancel_title();

    String patch_manager_rollback_error_cancel_body();

    String patch_manager_rollback_error_select_title();

    String patch_manager_rollback_error_select_body();

    String homepage_new_to_eap();
    String homepage_take_a_tour();

    String homepage_deployments_sub_header();
    String homepage_deployments_section();
    String homepage_deployments_standalone_step_intro();
    String homepage_deployments_standalone_step_1();
    String homepage_deployments_step_enable();
    String homepage_deployments_domain_step_intro();
    String homepage_deployments_domain_step_1();
    String homepage_deployments_domain_step_2();

    String homepage_configuration_standalone_sub_header();
    String homepage_configuration_domain_sub_header();
    String homepage_configuration_section();
    String homepage_configuration_step_intro();
    String homepage_configuration_standalone_step1();
    String homepage_configuration_domain_step1();
    String homepage_configuration_step2();
    String homepage_configuration_step3();

    String homepage_runtime_standalone_sub_header();
    String homepage_runtime_standalone_section();
    String homepage_runtime_step_intro();
    String homepage_runtime_standalone_step1();
    String homepage_runtime_standalone_step2();
    String homepage_runtime_domain_sub_header();
    String homepage_runtime_domain_server_group_section();
    String homepage_runtime_domain_server_group_step_intro();
    String homepage_runtime_domain_server_group_step1();
    String homepage_runtime_domain_server_group_step2();
    String homepage_runtime_domain_create_server_section();
    String homepage_runtime_domain_create_server_step_intro();
    String homepage_runtime_domain_create_server_step1();
    String homepage_runtime_domain_create_server_step2();
    String homepage_runtime_domain_monitor_server_section();
    String homepage_runtime_domain_monitor_server_step1();
    String homepage_runtime_domain_monitor_server_step2();

    String homepage_access_control_sub_header();
    String homepage_access_control_section();
    String homepage_access_control_step_intro();
    String homepage_access_control_step1();
    String homepage_access_control_step2();

    String homepage_patching_section();
    String homepage_patching_step1();
    String homepage_patching_domain_step2();
    String homepage_patching_step_apply();

    String homepage_help_general_resources();
    String homepage_help_eap_documentation_text();
    String homepage_help_eap_documentation_link();
    String homepage_help_learn_more_eap_text();
    String homepage_help_learn_more_eap_link();
    String homepage_help_trouble_ticket_text();
    String homepage_help_trouble_ticket_link();
    String homepage_help_training_text();
    String homepage_help_training_link();
    String homepage_help_tutorials_link();
    String homepage_help_tutorials_text();
    String homepage_help_eap_community_link();
    String homepage_help_eap_community_text();
    String homepage_help_eap_configurations_link();
    String homepage_help_eap_configurations_text();
    String homepage_help_knowledgebase_link();
    String homepage_help_knowledgebase_text();
    String homepage_help_consulting_link();
    String homepage_help_consulting_text();
    String homepage_help_wilfdfly_documentation_text();
    String homepage_help_admin_guide_text();
    String homepage_help_wildfly_issues_text();
    String homepage_help_get_help();
    String homepage_help_irc_text();
    String homepage_help_user_forums_text();
    String homepage_help_developers_mailing_list_text();
    String homepage_help_wilfdfly_home_text();
    String homepage_help_model_reference_text();
    String homepage_help_latest_news();

    String patch_manager_servers_still_running_warning();

    String common_label_restart();

    String patch_manager_restart_verify();

    String patch_manager_restart_required();

    String common_label_stats();

    String tx_jacorb_state_dmr_error();

    String forbidden_desc();

    String common_label_search();

    String search_placeholder();

    String search_popup_title();

    String search_tooltip_osx();

    String search_tooltip_other();

    String search_no_results();

    String section_deployment_intro();

    String search_index_reset();

    String search_index_reset_finished();

    String patch_manager_patch_details();

    String subsys_jca_dataSource_choose_template();

    String subsys_jca_dataSource_custom_template();

    String missing_driver();

    String missing_driver_module();

    String connecto_to();

    String connecto_to_desc_standalone();

    String connecto_to_desc_domain();

    String bs_configure_interface_header();

    String bs_configure_interface_desc();

    String bs_ping();

    String bs_interface_success();

    String bs_configure_interface_name_placeholder();

    String bs_configure_interface_invalid_scheme();

    String bs_configure_interface_invalid_host();

    String bs_configure_interface_invalid_port();

    String bs_configure_interface_duplicate();

    String bs_connect_interface_header();

    String bs_connect_interface_desc();

    String bs_connect_interface_connect();

    String bs_connect_interface_no_selection();

    String subsys_iiop_openjdk_desc();

    String wizard_back();

    String homepage_help_need_help();
}
