/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.enlinkd.service.api;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class DiscoveryBridgeTopology {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryBridgeTopology.class);

    private final Map<Integer,BridgeForwardingTable> m_bridgeFtMapUpdate = new HashMap<>();
    private final BroadcastDomain m_domain;
    private Set<Integer> m_failed;
    private Set<Integer> m_parsed;

    public static Set<String> getMacs(BridgeForwardingTable xBridge,
                                      BridgeForwardingTable yBridge, BridgeSimpleConnection simple)
            throws BridgeTopologyException {

        if ( simple.getFirstPort() == null) {
            throw new BridgeTopologyException("getMacs: not found simple connection ["
                    + xBridge.getNodeId() + "]", simple);
        }

        if ( simple.getSecondPort() == null) {
            throw new BridgeTopologyException("getMacs: not found simple connection ["
                    + yBridge.getNodeId() + "]", simple);
        }

        if (xBridge.getNodeId().intValue() != simple.getFirstPort().getNodeId().intValue()) {
            throw new BridgeTopologyException("getMacs: node mismatch ["
                    + xBridge.getNodeId() + "] found " , simple.getFirstPort());
        }

        if (yBridge.getNodeId().intValue() != simple.getSecondPort().getNodeId().intValue()) {
            throw new BridgeTopologyException("getMacs: node mismatch ["
                    + yBridge.getNodeId() + "]", simple.getSecondPort());
        }

        Set<String> macsOnSegment = xBridge.getBridgePortWithMacs(simple.getFirstPort()).getMacs();
        macsOnSegment.retainAll(yBridge.getBridgePortWithMacs(simple.getSecondPort()).getMacs());

        return macsOnSegment;
    }

    public BroadcastDomain getDomain() {
        return m_domain;
    }
    
    public Set<Integer> getFailed() {
        return m_failed;
    }

    public Set<Integer> getParsed() {
        return m_parsed;
    }

    public void addUpdatedBFT(Integer bridgeid, Set<BridgeForwardingTableEntry> notYetParsedBFT) {
        if (m_domain.getBridge(bridgeid) == null) {
            Bridge.create(m_domain, bridgeid);
        }
        try {
            m_bridgeFtMapUpdate.put(bridgeid, BridgeForwardingTable.create(m_domain.getBridge(bridgeid), notYetParsedBFT));
        } catch (BridgeTopologyException e) {
            LOG.warn("calculate:  node[{}], {}, topology:\n{}", 
                      bridgeid,
                      e.getMessage(),
                      e.printTopology(),
                      e);
        }
    }

    public DiscoveryBridgeTopology(BroadcastDomain domain) {
        Assert.notNull(domain);
        m_domain=domain;
    }
        
    public String getInfo() {
        StringBuilder info = new StringBuilder();
        info.append(getName());
        if (m_domain != null) {
            info.append(" domain nodes: ");
            info.append(m_domain.getBridgeNodesOnDomain());
        }
        info.append(", updated bft nodes: ");
        info.append(m_bridgeFtMapUpdate.keySet());
        if (m_parsed != null) {
            info.append(", parsed bft nodes: ");
            info.append(m_parsed);
        }
        if (m_failed != null) {
            info.append(", failed bft nodes: ");
            info.append(m_failed);
        }
        return  info.toString();
    }
            
    public String getName() {
        return "DiscoveryBridgeTopology";
    }

    private Bridge calcRootBridge() {
        // no spanning tree root?
        // why I'm here?
        // not root bridge defined (this mean no calculation yet done...
        // so checking the best into not parsed
        int size = 0;
        Bridge elected = null;
        for (Integer bridgeid : m_bridgeFtMapUpdate.keySet()) {
            Bridge bridge = m_domain.getBridge(bridgeid);
            LOG.debug("calculate: bridge:[{}] bft size \"{}\" in topology",
                      bridge.getNodeId(),
                      m_bridgeFtMapUpdate.get(bridgeid).getBftSize());
            if (size < m_bridgeFtMapUpdate.get(bridgeid).getBftSize()) {
                elected = bridge;
                size = m_bridgeFtMapUpdate.get(bridgeid).getBftSize();
            }
        }
        if (elected != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("calculate: bridge:[{}] 'elected' root with max bft size \"{}\" in topology",
                          elected.getNodeId(), size);
            }
        } else {
            elected = m_domain.getBridges().iterator().next();
            if (LOG.isDebugEnabled()) {
        	LOG.debug("calculate: bridge:[{}] 'elected' first bridge in topology", 
                       elected.getNodeId());
            }
        }

        return elected;
        
    }

    private Bridge electRootBridge() throws BridgeTopologyException {
        Bridge electedRoot = BroadcastDomain.electRootBridge(m_domain);
        Bridge rootBridge = m_domain.getRootBridge();
        
        if (electedRoot == null) {
            if (rootBridge != null) {
                electedRoot = rootBridge;
            } else {
                electedRoot = calcRootBridge();
            }
        }

        if (electedRoot.getNodeId() == null) {
            throw new BridgeTopologyException("elected Root bridge id cannot be null", electedRoot);
        }

        return electedRoot;
    }
    
    private void root(BridgeForwardingTable rootBft,Map<Integer, BridgeForwardingTable> bridgeFtMapCalcul) throws BridgeTopologyException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("calculate: bridge:[{}] elected has updated bft",
                      rootBft.getNodeId());
        }
        if (m_domain.getSharedSegments().isEmpty()) {
            rootBft.getBridge().setRootBridge();
            rootBft.getPorttomac().
                        forEach(ts -> 
                            SharedSegment.createAndAddToBroadcastDomain(m_domain,ts));
            LOG.debug("calculate: bridge:[{}] elected [root] is first:{}", 
                      rootBft.getNodeId(),
                 m_domain.getBridgeNodesOnDomain());
            return;
        } 
        BridgeForwardingTable oldRootBft = bridgeFtMapCalcul.get(m_domain.getRootBridge().getNodeId());
        BridgeSimpleConnection sp = BridgeSimpleConnection.create(oldRootBft, rootBft);
        sp.findSimpleConnection();
        rootBft.setRootPort(sp.getSecondBridgePort());
        down(oldRootBft,rootBft,sp,bridgeFtMapCalcul,0);
    }

    public  void calculate() {
        Assert.notNull(m_bridgeFtMapUpdate);
        if (LOG.isDebugEnabled()) {
            LOG.debug("calculate: domain\n{}", 
                      m_domain.printTopology());
        }
        m_parsed = new HashSet<>();
        m_failed = new HashSet<>();
        Bridge electedRoot;
        try {
            electedRoot = electRootBridge();
        } catch (BridgeTopologyException e) {
            LOG.error("calculate: {}, topology:\n{}",
                      e.getMessage(),
                      e.printTopology(),
                      e);
            m_failed.addAll(m_bridgeFtMapUpdate.keySet());
            return;
        }
        
        Map<Integer, BridgeForwardingTable> bridgeFtMapCalcul = new HashMap<>();
        
        if (m_bridgeFtMapUpdate.keySet().equals(m_domain.getBridgeNodesOnDomain())) {
            m_domain.clearTopology();
            if (LOG.isDebugEnabled()) {
                LOG.debug("calculate: domain cleaned ->\n{}", 
                          m_domain.printTopology());
            }
        } else {
            for (Integer bridgeId: m_bridgeFtMapUpdate.keySet()) {
                if (m_domain.getBridge(bridgeId).isNewTopology()) {
                    LOG.debug("calculate: bridge:[{}] is 'new'. skip clean topology   ", 
                                bridgeId);
                    continue;
                }
                
                try {
                    BroadcastDomain.clearTopologyForBridge(m_domain,bridgeId);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("calculate: bridge:[{}] cleaned ->\n{}", 
                                  bridgeId,
                                  m_domain.printTopology());
                    }
                } catch (BridgeTopologyException e) {
                    LOG.warn("calculate: bridge:[{}], {}, \n{}", bridgeId, e.getMessage(),e.printTopology());
                    m_failed.add(bridgeId);
                }
            }
            
            for (Bridge bridge: m_domain.getBridges()) {
                if (m_bridgeFtMapUpdate.containsKey(bridge.getNodeId())) {
                    continue;
                }
                if (bridge.isNewTopology()) {
                    LOG.warn("calculate: bridge:[{}] is new without update bft",
                              bridge.getNodeId());
                    continue;
                }
                try {
                    bridgeFtMapCalcul.put(bridge.getNodeId(),
                                          BridgeForwardingTable.create(bridge,
                                                                       BroadcastDomain.calculateBFT(m_domain,
                                                                                                    bridge)));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("calculate: bft from domain\n{}", 
                                  bridgeFtMapCalcul.get(bridge.getNodeId()).printTopology());
                    }
                } catch (BridgeTopologyException e) {
                    LOG.warn("calculate: bridge:[{}] clear topology. no calculated bft: {} ->\n{}",
                             bridge.getNodeId(), e.getMessage(),
                             e.printTopology());
                    m_domain.clearTopology();
                    calculate();
                }
            }
        }
        
        BridgeForwardingTable rootBft = 
                m_bridgeFtMapUpdate.get(electedRoot.getNodeId());
        
        if ( rootBft != null ) {
            try {
                root(rootBft, bridgeFtMapCalcul);
                m_parsed.add(rootBft.getNodeId());
            } catch (BridgeTopologyException e) {
                LOG.error("calculate: bridge:[{}], {}, \n{}", rootBft.getNodeId(), e.getMessage(),e.printTopology());
                m_failed.addAll(m_bridgeFtMapUpdate.keySet());
                return;
            }
        } 
        
        if (rootBft == null) {
            rootBft = bridgeFtMapCalcul.get(electedRoot.getNodeId());
        }
        
        if (m_domain.getRootBridge() != null && !Objects.equals(m_domain.getRootBridge().getNodeId(), electedRoot.getNodeId())) {
            try {
                BroadcastDomain.hierarchySetUp(m_domain,electedRoot);
                LOG.debug("calculate: bridge:[{}] elected is new [root] ->\n{}",
                          electedRoot.getNodeId(), 
                          m_domain.printTopology());
            } catch (BridgeTopologyException e) {
                LOG.error("calculate: bridge:[{}], {}, \n{}", electedRoot.getNodeId(), e.getMessage(),e.printTopology());
                m_failed.addAll(m_bridgeFtMapUpdate.keySet());
                m_failed.add(electedRoot.getNodeId());
                return;
            }
        }
        
        Set<Integer> postprocessing = new HashSet<>();
        for (Integer bridgeid: m_bridgeFtMapUpdate.keySet()) {
            if (m_parsed.contains(bridgeid) || m_failed.contains(bridgeid)) {
                continue;
            }
            BridgeForwardingTable bridgeFT = m_bridgeFtMapUpdate.get(bridgeid);
            if (bridgeFT.getPorttomac().size() == 1) {
                Integer bridgeFTrootPort = bridgeFT.getPorttomac().iterator().next().getPort().getBridgePort();
                bridgeFT.setRootPort(bridgeFTrootPort);
                postprocessing.add(bridgeid);
                LOG.debug("calculate: bridge:[{}] only one port:[{}] set to root. Postprocessing",
                          bridgeid, bridgeFTrootPort);
                continue;
            }
            BridgeSimpleConnection upsimpleconn = BridgeSimpleConnection.create(rootBft, bridgeFT);
            try {
                  upsimpleconn.findSimpleConnection();
                  if (LOG.isDebugEnabled()) {
                           LOG.debug("calculate: level: 1, bridge:[{}] -> {}", 
                                    bridgeFT.getNodeId(),
                                    upsimpleconn.printTopology());
                   }
                   bridgeFT.setRootPort(upsimpleconn.getSecondBridgePort());
                   LOG.debug("calculate: level: 1, bridge:[{}]. set root port:[{}]", 
                                bridgeFT.getNodeId(),
                                upsimpleconn.getSecondBridgePort());
            } catch (BridgeTopologyException e) {
                LOG.warn("calculate: bridge:[{}], no root port found. {}, \n{}", bridgeid, e.getMessage(),e.printTopology());
                m_failed.add(bridgeid);
                continue;
            }                
            
            try {
                down(rootBft, m_bridgeFtMapUpdate.get(bridgeid),upsimpleconn,bridgeFtMapCalcul,0);
                m_parsed.add(bridgeid);
            } catch (BridgeTopologyException e) {
                LOG.warn("calculate: bridge:[{}], no topology found. {}, \n{}", bridgeid, e.getMessage(),e.printTopology());
                m_failed.add(bridgeid);
            }                
        }  

        for (Integer failedbridgeid: new HashSet<>(m_failed)) {
            if (failedbridgeid == null) {
                LOG.error("calculate: bridge:[null], first iteration on failed");
                continue;
            }
            BridgeForwardingTable failedBridgeFT = m_bridgeFtMapUpdate.get(failedbridgeid);
            if (failedBridgeFT == null) {
                LOG.error("calculate: bridge:[{}], first iteration on failed. FT is null",failedbridgeid);
                continue;
            }
            try {
                postprocess(failedBridgeFT, rootBft,bridgeFtMapCalcul, new HashSet<>(m_parsed));
            } catch (BridgeTopologyException e) {
                LOG.warn("calculate: bridge:[{}], first iteration on failed. no topology found. {}, \n{}", failedbridgeid, e.getMessage(),e.printTopology());
                continue;
            }
            m_parsed.add(failedbridgeid);
            m_failed.remove(failedbridgeid);
        }        

        for (Integer failedbridgeid: new HashSet<>(m_failed)) {
            if (failedbridgeid == null) {
                LOG.error("calculate: bridge:[null], second iteration on failed");
                continue;
            }
            BridgeForwardingTable failedBridgeFT = m_bridgeFtMapUpdate.get(failedbridgeid);
            if (failedBridgeFT == null) {
                LOG.error("calculate: bridge:[{}], second iteration on failed. FT is null",failedbridgeid);
                continue;
            }
             try {
                postprocess(failedBridgeFT,rootBft, bridgeFtMapCalcul, new HashSet<>(m_parsed));
            } catch (BridgeTopologyException e) {
                LOG.warn("calculate: bridge:[{}], second iteration on failed. no topology found. {}, \n{}", failedbridgeid, e.getMessage(),e.printTopology());
                continue;
            }
            m_parsed.add(failedbridgeid);
            m_failed.remove(failedbridgeid);
        }        

        for (Integer postprocessbridgeid: new HashSet<>(postprocessing)) {
            if (postprocessbridgeid == null) {
                LOG.error("calculate: bridge:[null], postprocessbridge");
                continue;
            }            
            BridgeForwardingTable postprocessBridgeFT = m_bridgeFtMapUpdate.get(postprocessbridgeid);
            if (postprocessBridgeFT == null) {
                LOG.error("calculate: bridge:[{}],postprocessbridge. FT is null",postprocessbridgeid);
                continue;
            }
            BridgeSimpleConnection simpleConnection = BridgeSimpleConnection.create(rootBft, postprocessBridgeFT);
            try {
                simpleConnection.findSimpleConnection();
                down(rootBft, postprocessBridgeFT, simpleConnection, bridgeFtMapCalcul,
                     0);
            } catch (BridgeTopologyException e) {
                LOG.warn("calculate: bridge:[{}], postprocessbridge. No topology found for single port node. {}, \n{}", postprocessbridgeid, e.getMessage(),e.printTopology());
                m_failed.add(postprocessbridgeid);
                continue;
            }
            m_parsed.add(postprocessbridgeid);
        }        

        for (Integer failedbridgeid: new HashSet<>(m_failed)) {
            if (failedbridgeid == null) {
                LOG.error("calculate: bridge:[null], third iteration on failed");
                continue;
            }
            BridgeForwardingTable failedBridgeFT = m_bridgeFtMapUpdate.get(failedbridgeid);
            if (failedBridgeFT == null) {
                LOG.error("calculate: bridge:[{}], third iteration on failed. FT is null",failedbridgeid);
                continue;
            }
            try {
                postprocess(failedBridgeFT,rootBft, bridgeFtMapCalcul, new HashSet<>(m_parsed));
            } catch (BridgeTopologyException e) {
                LOG.warn("calculate: bridge:[{}], third iteration on failed. no topology found. {}, \n{}", failedbridgeid, e.getMessage(),e.printTopology());
                continue;
            }
            m_parsed.add(failedbridgeid);
            m_failed.remove(failedbridgeid);
        }        

        m_bridgeFtMapUpdate.values().stream().
            filter(ft -> m_parsed.contains(ft.getNodeId())).
                forEach(ft -> BroadcastDomain.addforwarders(m_domain, ft));
        
        bridgeFtMapCalcul.values().
            forEach(ft -> BroadcastDomain.addforwarders(m_domain, ft));

        if (LOG.isDebugEnabled()) {
            LOG.debug("calculate: domain\n{}", 
                      m_domain.printTopology());
        }
    }
    
    private void postprocess(BridgeForwardingTable postBridgeFT, BridgeForwardingTable rootBridgeFT,Map<Integer,BridgeForwardingTable> bridgeFtMapCalcul, Set<Integer> parsed) throws BridgeTopologyException {
        Integer postbridgeid = postBridgeFT.getBridge().getNodeId();
        for (Integer parsedbridgeid : parsed) {
            if (parsedbridgeid.intValue() == rootBridgeFT.getNodeId().intValue()) {
                continue;
            }
            BridgeForwardingTable parsedBridgeFT = m_bridgeFtMapUpdate.get(parsedbridgeid);
            if (parsedBridgeFT == null) {
                parsedBridgeFT = bridgeFtMapCalcul.get(parsedbridgeid);
            }
            
            BridgeSimpleConnection sp = BridgeSimpleConnection.create(parsedBridgeFT,
                    postBridgeFT);

            try {
                sp.findSimpleConnection();
            } catch (BridgeTopologyException e) {
                LOG.warn("postprocess: bridge:[{}] <--> bridge:[{}] no topology found. {}, \n{}",
                         postbridgeid, parsedbridgeid, e.getMessage(),
                         e.printTopology());
                continue;
            }
            
            if (!parsedBridgeFT.getBridge().isRootBridge()
                    && !parsedBridgeFT.getRootPort().equals(sp.getFirstPort())) {
                if (postBridgeFT.getBridge().isNewTopology()) {
                    postBridgeFT.setRootPort(sp.getSecondBridgePort());
                }
                try {
                    down(parsedBridgeFT, postBridgeFT, sp, bridgeFtMapCalcul,
                         0);
                } catch (BridgeTopologyException e) {
                    LOG.warn("postprocess: bridge:[{}] <--> bridge:[{}] no topology found. {}, \n{}",
                             postbridgeid, parsedbridgeid, e.getMessage(),
                             e.printTopology());
                    continue;
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("postprocess: bridge:[{}] <--> bridge:[{}] topology found.",
                              postbridgeid, parsedbridgeid);
                }
                return;
            }
        }
        BridgeSimpleConnection simpleConnection = BridgeSimpleConnection.create(rootBridgeFT, postBridgeFT);
        try {
            simpleConnection.findSimpleConnection();
            down(rootBridgeFT, postBridgeFT, simpleConnection, bridgeFtMapCalcul,
                 0);
            return;
        } catch (BridgeTopologyException e) {
            LOG.warn("postprocess: bridge:[{}] <--> bridge:[{}] no topology found. {}, \n{}",
                     postbridgeid,rootBridgeFT.getNodeId(), e.getMessage(),
                     e.printTopology());
        }
        throw new BridgeTopologyException("postprocess: no connection found", postBridgeFT);
    }
    
    private void down(BridgeForwardingTable bridgeUpFT,  
            BridgeForwardingTable bridgeFT, BridgeSimpleConnection upsimpleconn, Map<Integer,BridgeForwardingTable> bridgeFtMapCalcul, Integer level) throws BridgeTopologyException {

        if (++level == BroadcastDomain.maxlevel) {
            throw new BridgeTopologyException(
                          "down: level: " + level +", bridge:["+bridgeFT.getNodeId()+"], too many iteration");
        }

        SharedSegment upSegment = m_domain.getSharedSegment(upsimpleconn.getFirstPort());
        if (upSegment == null) {
            throw new BridgeTopologyException(
                          "down: level: " + level +", bridge:["+bridgeFT.getNodeId()+"], up segment not found");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("down: level: {}, bridge:[{}]. up segment -> \n{} ",
                        level,
                        bridgeFT.getNodeId(),
                        upSegment.printTopology());
        }

        Set<BridgePort> parsed = new HashSet<>();
        parsed.add(bridgeFT.getRootPort());

        Set<BridgeForwardingTable> checkforwarders = new HashSet<>();
        checkforwarders.add(bridgeUpFT);
        checkforwarders.add(bridgeFT);

        Map<BridgePortWithMacs, Set<BridgePortWithMacs>> splitted 
            = new HashMap<>();
        
        BridgeForwardingTable nextDownBridge = null;
        BridgeSimpleConnection nextDownSP = null;
        boolean levelfound = false;
        
        Set<String> maconupsegment = getMacs(bridgeUpFT, bridgeFT, upsimpleconn);
        
        for (Bridge curbridge : m_domain.getBridgeOnSharedSegment(upSegment)) {
            
            if (curbridge.getNodeId().intValue() == upSegment.getDesignatedBridge().intValue()) {
                continue;
            }
            
            BridgeForwardingTable curBridgeFT = m_bridgeFtMapUpdate.get(curbridge.getNodeId());
            if (curBridgeFT == null) {
                curBridgeFT = bridgeFtMapCalcul.get(curbridge.getNodeId());
            }
            if (curBridgeFT == null) {
                throw new BridgeTopologyException(
                      "down: level: " + level +", bridge:["+bridgeFT.getNodeId()+"], no bft for: " + curbridge.printTopology());
            }
            checkforwarders.add(curBridgeFT);
            
            BridgeSimpleConnection simpleconn = 
                    BridgeSimpleConnection.create(curBridgeFT,
                                       bridgeFT);
            simpleconn.findSimpleConnection();
            if (LOG.isDebugEnabled()) {
                LOG.debug("down: level: {}, bridge:[{}]. {}", 
                         level,
                         bridgeFT.getNodeId(),
                         simpleconn.printTopology());
            }
            if (!Objects.equals(simpleconn.getSecondBridgePort(), bridgeFT.getBridge().getRootPort())
                && !Objects.equals(simpleconn.getFirstBridgePort(), curbridge.getRootPort())) {
                throw new BridgeTopologyException(
                              "down: level: " + level +", bridge:["+bridgeFT.getNodeId()+"], Topology mismatch. NO ROOTS");
            }

            // bridge is a leaf of curbridge
            if (Objects.equals(simpleconn.getSecondBridgePort(), bridgeFT.getRootBridgePort())
                    &&
                    !Objects.equals(simpleconn.getFirstBridgePort(), curbridge.getRootPort())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("down: level: {}, bridge: [{}], is 'down' of -> {}",
                              level,
                              bridgeFT.getNodeId(),
                              simpleconn.getFirstPort().printTopology()
                              );
                }
                if (nextDownBridge != null) {
                    throw new BridgeTopologyException(
                              "down: level: " + level +", bridge:["+bridgeFT.getNodeId()+"], Topology mismatch. LEAF OF TWO");
                }
                if (levelfound) {
                    throw new BridgeTopologyException(
                              "down: level: " + level +", bridge:["+bridgeFT.getNodeId()+"], Topology mismatch. LEAF AND LEVEL FOUND");
                }
                nextDownBridge = curBridgeFT;
                nextDownSP = simpleconn;
                continue;
            }
            
            // bridge is up curbridge
            if (Objects.equals(simpleconn.getFirstBridgePort(), curBridgeFT.getRootBridgePort())
                    && !Objects.equals(simpleconn.getSecondBridgePort(), bridgeFT.getRootBridgePort())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("down: level: {}, bridge: [{}], {} is 'up' of -> [{}]",
                              level,
                              bridgeFT.getNodeId(),
                              simpleconn.getSecondPort().printTopology(),
                              curbridge.getNodeId()
                              );
                }
                if (nextDownBridge != null) {
                    throw new BridgeTopologyException(
                      "down: level: " + level +", bridge:["+bridgeFT.getNodeId()+"], Topology mismatch. LEAF AND LEVEL FOUND");
                }
                levelfound = true;
                if (!splitted.containsKey(bridgeFT.getBridgePortWithMacs(simpleconn.getSecondPort()))) {
                    splitted.put(bridgeFT.getBridgePortWithMacs(simpleconn.getSecondPort()),
                            new HashSet<>());
                }
                splitted.get(bridgeFT.getBridgePortWithMacs(simpleconn.getSecondPort())).
                    add(curBridgeFT.getBridgePortWithMacs(simpleconn.getFirstPort()));
                parsed.add(simpleconn.getSecondPort());
                continue;
            }
            //here are all the simple connection in which the connection is the root port
            maconupsegment.retainAll(getMacs(curBridgeFT, bridgeFT, simpleconn));
        } // end of loop on up segment bridges
        
        if (nextDownBridge != null) {
            down(nextDownBridge, bridgeFT, nextDownSP,bridgeFtMapCalcul,level);
            return;
        }
        
        SharedSegment.merge(m_domain, 
                            upSegment, 
                            splitted,
                            maconupsegment,
                            bridgeFT.getRootPort(),
                            BridgeForwardingTable.getThroughSet(bridgeFT, parsed));
        checkforwarders.forEach(ft -> BroadcastDomain.addforwarders(m_domain, ft));
    }
    
}
