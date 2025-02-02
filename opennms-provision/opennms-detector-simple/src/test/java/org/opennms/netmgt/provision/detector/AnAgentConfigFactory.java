/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2014 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.provision.detector;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import org.opennms.netmgt.config.SnmpEventInfo;
import org.opennms.netmgt.config.api.SnmpAgentConfigFactory;
import org.opennms.netmgt.config.snmp.Definition;
import org.opennms.netmgt.config.snmp.SnmpConfig;
import org.opennms.netmgt.config.snmp.SnmpProfile;
import org.opennms.netmgt.snmp.SnmpAgentConfig;

/**
 * @author Donald Desloge
 *
 */
public class AnAgentConfigFactory implements SnmpAgentConfigFactory {

    public void define(final SnmpEventInfo info) {
    }

    @Override
    public void saveCurrent() throws IOException {

    }

    @Override
    public SnmpAgentConfig getAgentConfig(InetAddress address, String location) {
        final SnmpAgentConfig agentConfig = new SnmpAgentConfig(address);
        agentConfig.setVersion(SnmpAgentConfig.DEFAULT_VERSION);
        return agentConfig;
    }

    @Override
    public SnmpAgentConfig getAgentConfigFromProfile(SnmpProfile snmpProfile, InetAddress address, boolean metaDataInterpolation) {
        return null;
    }

    @Override
    public void saveDefinition(Definition definition) {

    }

    @Override
    public boolean removeFromDefinition(InetAddress ipAddress, String location, String module) {
       return true;
    }

    @Override
    public void saveAgentConfigAsDefinition(SnmpAgentConfig snmpAgentConfig, String location, String module) {

    }

    @Override
    public List<SnmpProfile> getProfiles() {
        return null;
    }

    @Override
    public SnmpConfig getSnmpConfig() {
        return null;
    }

}
