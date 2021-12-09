/*******************************************************************************
 * This file is part of OpenNMS(R).
 * 
 * Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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
 *     http://www.gnu.org/licenses/
 * 
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.config.notifd;

import org.opennms.netmgt.config.utils.ConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HandlerClass implements java.io.Serializable {
    private static final long serialVersionUID = 2L;

    private String name;

    private List<InitParams> initParams = new ArrayList<>();

    public HandlerClass() { }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = ConfigUtils.assertNotEmpty(name, "name");
    }

    public List<InitParams> getInitParams() {
        return initParams;
    }

    public void setInitParams(final List<InitParams> params) {
        if (params == this.initParams) return;
        this.initParams.clear();
        if (params != null) this.initParams.addAll(params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, initParams);
    }

    @Override
    public boolean equals(final Object obj) {
        if ( this == obj ) {
            return true;
        }

        if (obj instanceof HandlerClass) {
            final HandlerClass that = (HandlerClass)obj;
            return Objects.equals(this.name, that.name)
                    && Objects.equals(this.initParams, that.initParams);
        }
        return false;
    }
}