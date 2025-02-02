/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2018 The OpenNMS Group, Inc.
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
package org.opennms.plugins.elasticsearch.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.opennms.plugins.elasticsearch.rest.EventToIndex.isOID;

import java.util.Collections;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.opennms.features.jest.client.JestClientWithCircuitBreaker;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;

public class EventToIndexTest {

    @Test
    public void verifyIsOID() {
        assertThat(isOID(".3"), is(true));
        assertThat(isOID(".3.1.2"), is(true));
        assertThat(isOID("..3.."), is(false));
        assertThat(isOID("192.168.0.1"), is(false));
        assertThat(isOID("nodeLabel"), is(false));
    }

    @Test
    public void testHandleParameters() {
        @SuppressWarnings("resource")
        final EventToIndex etoi = new EventToIndex(mock(JestClientWithCircuitBreaker.class), 1);
        etoi.setGroupOidParameters(true);

        final Event e = new EventBuilder("opennms/fake-uei", "source").addParam(".3.1.2", "oid-param").addParam("nodeLabel", "fake-node").getEvent();
        e.setParmCollection(Collections.unmodifiableList(e.getParmCollection()));
        final JSONObject json = new JSONObject();
        etoi.handleParameters(e, json);
        assertEquals(2, e.getParmCollection().size());
        assertNotNull(json.get("p_oids"));
        assertEquals(2, json.keySet().size());
    }
}
