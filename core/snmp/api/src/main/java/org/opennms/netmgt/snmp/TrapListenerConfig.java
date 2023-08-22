/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.snmp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@XmlRootElement(name = "trap-listener-config")
@XmlAccessorType(XmlAccessType.NONE)
@JsonPropertyOrder(alphabetic=true)
@JsonInclude(Include.NON_NULL)
public class TrapListenerConfig {

    public static final String TWIN_KEY = "trapd.listener.config";

    @XmlElementWrapper(name = "snmp-v3-users")
    @XmlElement(name = "snmp-v3-user")
    @JsonProperty("snmp-v3-users")
    private List<SnmpV3User> snmpV3Users= new ArrayList<>();

    public List<SnmpV3User> getSnmpV3Users() {
        return this.snmpV3Users;
    }

    public void setSnmpV3Users(final List<SnmpV3User> snmpV3Users) {
        this.snmpV3Users = snmpV3Users;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TrapListenerConfig)) {
            return false;
        }
        final TrapListenerConfig that = (TrapListenerConfig) o;
        return Objects.equals(this.snmpV3Users, that.snmpV3Users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.snmpV3Users);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TrapListenerConfig.class.getSimpleName() + "[", "]")
                .add("snmpV3Users=" + snmpV3Users)
                .toString();
    }
}
