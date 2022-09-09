/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
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

package org.opennms.core.ipc.twin.memory;

import java.io.Closeable;
import java.util.Objects;
import java.util.function.Consumer;

import org.opennms.core.ipc.twin.api.TwinSubscriber;

import com.google.common.reflect.TypeToken;

public class MemoryTwinSubscriber implements TwinSubscriber {

    private final MemoryTwinPublisher publisher;

    private final String location;

    public MemoryTwinSubscriber(final MemoryTwinPublisher publisher,
                                final String location) {
        this.publisher = Objects.requireNonNull(publisher);
        this.location = Objects.requireNonNull(location);
    }

    @Override
    public <T> Closeable subscribe(final String key, final TypeToken<T> type, final Consumer<T> consumer) {
        return this.publisher.subscribe(key, this.location, type, consumer);
    }

    @Override
    public void close() {
    }
}
