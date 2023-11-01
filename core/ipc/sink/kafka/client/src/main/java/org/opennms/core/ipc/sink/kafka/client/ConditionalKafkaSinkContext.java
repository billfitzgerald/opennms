/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.core.ipc.sink.kafka.client;

import static org.opennms.core.ipc.sink.common.SinkStrategy.Strategy.KAFKA;

import org.opennms.core.ipc.sink.api.MessageConsumerManager;
import org.opennms.core.ipc.sink.common.SinkStrategy;
import org.opennms.core.logging.Logging;
import org.opennms.core.logging.Logging.MDCCloseable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
@Conditional(ConditionalKafkaSinkContext.Condition.class)
@ImportResource("/META-INF/opennms/applicationContext-ipc-sink-kafka-client.xml")
public class ConditionalKafkaSinkContext {
    private static final Logger LOG = LoggerFactory.getLogger(ConditionalKafkaSinkContext.class);

    static class Condition implements ConfigurationCondition {
        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.PARSE_CONFIGURATION;
        }

        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            final boolean enabled = KAFKA.equals(SinkStrategy.getSinkStrategy());
            try (MDCCloseable mdc = Logging.withPrefixCloseable(MessageConsumerManager.LOG_PREFIX)) {
                LOG.debug("Enable Kafka Sink: {}", enabled);
            }
            return enabled;
        }
   }
}
