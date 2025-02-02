/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

/**
 * 
 */
package org.opennms.netmgt.provision.service;

import static org.opennms.netmgt.provision.service.ProvisionService.IP_ADDRESS;
import static org.opennms.netmgt.provision.service.ProvisionService.LOCATION;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opennms.core.tasks.BatchTask;
import org.opennms.core.tasks.RunInBatch;
import org.opennms.netmgt.config.api.SnmpAgentConfigFactory;
import org.opennms.netmgt.config.snmp.SnmpProfile;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.monitoringLocations.OnmsMonitoringLocation;
import org.opennms.netmgt.provision.NodePolicy;
import org.opennms.netmgt.provision.service.snmp.SystemGroup;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpAgentTimeoutException;
import org.opennms.netmgt.snmp.SnmpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.common.base.Strings;

import io.opentracing.Span;

final class NodeInfoScan implements RunInBatch {
    private static final Logger LOG = LoggerFactory.getLogger(NodeInfoScan.class);

    private final SnmpAgentConfigFactory m_agentConfigFactory;
    private final InetAddress m_agentAddress;
    private final String m_foreignSource;
    private OnmsNode m_node;
    private Integer m_nodeId;
    private final OnmsMonitoringLocation m_location;
    private boolean restoreCategories = false;
    private final ProvisionService m_provisionService;
    private final ScanProgress m_scanProgress;
    private Span m_parentSpan;
    private Span m_span;

    NodeInfoScan(OnmsNode node, InetAddress agentAddress, String foreignSource, final OnmsMonitoringLocation location, ScanProgress scanProgress, SnmpAgentConfigFactory agentConfigFactory, ProvisionService provisionService, Integer nodeId, Span span){
        m_node = node;
        m_agentAddress = agentAddress;
        m_foreignSource = foreignSource;
        m_location = location;
        m_scanProgress = scanProgress;
        m_agentConfigFactory = agentConfigFactory;
        m_provisionService = provisionService;
        m_nodeId = nodeId;
        m_parentSpan = span;
    }

    /** {@inheritDoc} */
    @Override
    public void run(BatchTask phase) {
        if (m_parentSpan != null) {
            m_span = m_provisionService.buildAndStartSpan("NodeInfoScan", m_parentSpan.context());
        } else {
            m_span = m_provisionService.buildAndStartSpan("NodeInfoScan", null);
        }
        m_span.setTag(IP_ADDRESS, m_agentAddress.getHostAddress());
        m_span.setTag(LOCATION, getLocationName());
        phase.getBuilder().addSequence(
                new RunInBatch() {
                    @Override
                    public void run(BatchTask batch) {
                        Span span = m_provisionService.buildAndStartSpan("CollectNodeInfo", m_span.context());
                        collectNodeInfo();
                        span.finish();
                    }
                },
                new RunInBatch() {
                    @Override
                    public void run(BatchTask phase) {
                        Span span = m_provisionService.buildAndStartSpan("PersistNodeInfo", m_span.context());
                        doPersistNodeInfo();
                        span.finish();
                        m_span.finish();
                    }
                });

    }

    private InetAddress getAgentAddress() {
        return m_agentAddress;
    }

    private SnmpAgentConfig getAgentConfig(InetAddress primaryAddress) {
        return getAgentConfigFactory().getAgentConfig(primaryAddress,
                (m_location == null) ? null : m_location.getLocationName());
    }

    private SnmpAgentConfigFactory getAgentConfigFactory() {
        return m_agentConfigFactory;
    }

    private String getForeignSource() {
        return m_foreignSource;
    }

    public OnmsMonitoringLocation getLocation() {
        return m_location;
    }

    private String getLocationName() {
        return m_location == null ? null : m_location.getLocationName();
    }

    private ProvisionService getProvisionService() {
        return m_provisionService;
    }

    private void abort(String reason) {
        m_scanProgress.abort(reason);
    }

    private OnmsNode getNode() {
        return m_node;
    }
    
    private Integer getNodeId() {
        return m_nodeId;
    }

    private void setNode(OnmsNode node) {
        m_node = node;
    }

    private void collectNodeInfo() {
        Assert.notNull(getAgentConfigFactory(), "agentConfigFactory was not injected");
        InetAddress primaryAddress = getAgentAddress();
        SnmpAgentConfig agentConfig = getAgentConfig(primaryAddress);
        
        SystemGroup systemGroup = new SystemGroup(primaryAddress);

        try {
            try {
                m_provisionService.getLocationAwareSnmpClient().walk(agentConfig, systemGroup)
                    .withDescription("systemGroup")
                    .withLocation(getLocationName())
                    .execute()
                    .get();
                systemGroup.updateSnmpDataForNode(getNode());
            } catch (ExecutionException e) {
                boolean succeeded = false;
                if (isSnmpRelatedException(e) && !isAgentConfigValid(agentConfig, primaryAddress)) {
                    succeeded = peformScanWithMatchingProfile(agentConfig, primaryAddress);
                }
                if(!succeeded) {
                    abort("Aborting node scan : Agent failed while scanning the system table: " + e.getMessage());
                }
            }

            List<NodePolicy> nodePolicies = getProvisionService().getNodePoliciesForForeignSource(getEffectiveForeignSource());

            OnmsNode node = null;
            if (isAborted()) {
                if (getNodeId() != null && nodePolicies.size() > 0) {
                    restoreCategories = true;
                    node = m_provisionService.getDbNodeInitCat(getNodeId());
                    LOG.debug("collectNodeInfo: checking {} node policies for restoration of categories", nodePolicies.size());
                }
            } else {
                node = getNode();
            }
            for(NodePolicy policy : nodePolicies) {
                if (node != null) {
                    LOG.info("Applying NodePolicy {}({}) to {}", policy.getClass(), policy, node.getLabel());
                    node = policy.apply(node, Collections.emptyMap());
                }
            }
        
            if (node == null) {
                restoreCategories = false;
                if (!isAborted()) {
                    String reason = "Aborted scan of node due to configured policy";
                    abort(reason);
                }
            } else {
                setNode(node);
            }
        
        } catch (final InterruptedException e) {
            abort("Aborting node scan : Scan thread interrupted!");
            Thread.currentThread().interrupt();
        }
    }

    static boolean isSnmpRelatedException(Exception e) {
        // All the exceptions are converted to messages with
        // RemoteExecutionException.toErrorMessage(Throwable e)
        return e.getCause() instanceof SnmpException ||
                e.getCause() instanceof SnmpAgentTimeoutException ||
                e.getMessage().contains(SnmpException.class.getSimpleName()) ||
                e.getMessage().contains(SnmpAgentTimeoutException.class.getSimpleName());
    }


    /**
     * Validates if agent config is still valid.
     * Agent config is valid if it is derived from definitions and matches with config from profile.
     * Also valid if it is derived definitions but there is no associated profile.
     */
    private boolean isAgentConfigValid(SnmpAgentConfig currentConfig, InetAddress address) {
        String profileLabel = currentConfig.getProfileLabel();
        // If this config is default, it may not be valid config.
        if(currentConfig.isDefault()) {
            return  false;
        }
        // Not a default config, but is this a definition without profile.
        if (Strings.isNullOrEmpty(profileLabel)) {
            return true;
        } else {
            // Is this definition with profile still valid
            Optional<SnmpProfile> matchingProfile = getAgentConfigFactory().getProfiles().stream()
                    .filter(profile -> profile.getLabel().equals(profileLabel))
                    .findFirst();
            if (matchingProfile.isPresent()) {
                SnmpAgentConfig configFromProfile = getAgentConfigFactory().getAgentConfigFromProfile(matchingProfile.get(), address);
                if (configFromProfile.equals(currentConfig)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean peformScanWithMatchingProfile(SnmpAgentConfig currentConfig, InetAddress primaryAddress) throws InterruptedException {

        try {
            Optional<SnmpAgentConfig> validConfig = m_provisionService.getSnmpProfileMapper()
                                                        .getAgentConfigFromProfiles(primaryAddress, getLocationName(), false)
                                                        .get();

            if (validConfig.isPresent()) {
                SnmpAgentConfig agentConfig = validConfig.get();
                getAgentConfigFactory().saveAgentConfigAsDefinition(agentConfig, getLocationName(), "Provisiond");
                LOG.info("IP address {} is fitted with profile {}", primaryAddress.getHostAddress(), agentConfig.getProfileLabel());
                SystemGroup systemGroup = new SystemGroup(primaryAddress);

                final SnmpAgentConfig interpolatedAgentConfig = m_provisionService.getSnmpProfileMapper()
                        .getAgentConfigFromProfiles(primaryAddress, getLocationName())
                        .get().orElse(agentConfig);
                try {
                    m_provisionService.getLocationAwareSnmpClient().walk(interpolatedAgentConfig, systemGroup)
                            .withDescription("systemGroup")
                            .withLocation(getLocationName())
                            .execute()
                            .get();
                    systemGroup.updateSnmpDataForNode(getNode());
                    return true;
                } catch (ExecutionException e) {
                    LOG.error("Exception while doing SNMP walk with config from SNMP profile {}.", agentConfig.getProfileLabel(), e);
                }
            }
        } catch (ExecutionException e) {
            LOG.error("Exception while trying to get SNMP profiles.", e);
        }

        return false;
    }


    private String getEffectiveForeignSource() {
        return getForeignSource()  == null ? "default" : getForeignSource();
    }

    private void doPersistNodeInfo() {
        if (restoreCategories) {
            LOG.debug("doPersistNodeInfo: Restoring {} categories to DB", getNode().getCategories().size());
        }
        if (!isAborted() || restoreCategories) {
            getProvisionService().updateNodeAttributes(getNode());
        }
    }

    private boolean isAborted() {
        return m_scanProgress.isAborted();
    }
}
