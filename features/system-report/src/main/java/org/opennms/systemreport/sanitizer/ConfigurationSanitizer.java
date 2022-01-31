/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.systemreport.sanitizer;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationSanitizer {

    private final Map<String, ConfigFileSanitizer> sanitizers = new HashMap<>();

    public ConfigurationSanitizer(XmlFileSanitizer xmlFileSanitizer, PropertiesFileSanitizer propertiesFileSanitizer) {
        // TODO Refactor to depend on a list of ConfigFileSanitizer instead of individual beans
        sanitizers.put(xmlFileSanitizer.getFileType(), xmlFileSanitizer);
        sanitizers.put(propertiesFileSanitizer.getFileType(), propertiesFileSanitizer);
    }

    public Resource getSanitizedResource(final File file) {
        String fileName = file.getName();

        if (fileName.contains(".")) {
            String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
            if (sanitizers.containsKey(fileExtension)) {
                try {
                    return sanitizers.get(fileExtension).getSanitizedResource(file);
                } catch (FileSanitizationException e) {
                    e.getCause().printStackTrace();

                    return new SanitizedResource(e.getMessage().getBytes());
                }
            }
        }

        return new FileSystemResource(file);
    }
}
