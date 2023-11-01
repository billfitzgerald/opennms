/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
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
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.flows.classification.internal.matcher;

import java.util.Objects;
import java.util.function.Function;

import org.opennms.netmgt.flows.classification.ClassificationRequest;
import org.opennms.netmgt.flows.classification.IpAddr;
import org.opennms.netmgt.flows.classification.internal.value.IpValue;

class IpMatcher implements Matcher {

    // Extracts the value from the ClassificationRequest. Allows to easily distinguish between srcAddress and dstAddress
    private final Function<ClassificationRequest, IpAddr> valueExtractor;
    private final IpValue value;

    protected IpMatcher(IpValue input, Function<ClassificationRequest, IpAddr> valueExtractor) {
        this.value = input;
        this.valueExtractor = Objects.requireNonNull(valueExtractor);
    }

    @Override
    public boolean matches(ClassificationRequest request) {
        var addr = valueExtractor.apply(request);
        final boolean matches = value.isInRange(addr);
        return matches;
    }
}
