/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2022 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.statsd;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.After;
import org.junit.Test;
import org.opennms.netmgt.config.statsd.model.PackageReport;
import org.opennms.netmgt.config.statsd.model.StatsdPackage;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.api.ResourceDao;
import org.opennms.netmgt.dao.support.BottomNAttributeStatisticVisitor;
import org.opennms.netmgt.filter.api.FilterDao;
import org.opennms.netmgt.measurements.api.FetchResults;
import org.opennms.netmgt.measurements.api.MeasurementFetchStrategy;
import org.opennms.netmgt.measurements.model.Source;
import org.opennms.netmgt.mock.MockResourceType;
import org.opennms.netmgt.model.AttributeStatisticVisitorWithResults;
import org.opennms.netmgt.model.ExternalValueAttribute;
import org.opennms.netmgt.model.OnmsAttribute;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.ResourcePath;
import org.opennms.netmgt.model.RrdGraphAttribute;
import org.opennms.test.ThrowableAnticipator;

/**
 * 
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 */
public class ReportDefinitionTest {
    private NodeDao m_nodeDao = mock(NodeDao.class);
    private ResourceDao m_resourceDao = mock(ResourceDao.class);
    private MeasurementFetchStrategy m_fetchStrategy = mock(MeasurementFetchStrategy.class);
    private FilterDao m_filterDao = mock(FilterDao.class);

    @After
    public void tearDown() {
        verifyNoMoreInteractions(m_nodeDao);
        verifyNoMoreInteractions(m_resourceDao);
        verifyNoMoreInteractions(m_fetchStrategy);
        verifyNoMoreInteractions(m_filterDao);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBogusReportClass() throws Exception {
        ReportDefinition def = new ReportDefinition();
        
        Class<? extends AttributeStatisticVisitorWithResults> clazz = (Class<? extends AttributeStatisticVisitorWithResults>) Class.forName("java.lang.String");
        
        ThrowableAnticipator ta = new ThrowableAnticipator();
        ta.anticipate(new IllegalArgumentException(ThrowableAnticipator.IGNORE_MESSAGE));
        try {
            def.setReportClass(clazz);
        } catch (Throwable t) {
            ta.throwableReceived(t);
        }
        
        ta.verifyAnticipated();
    }

    @Test
    public void testAfterPropertiesSet() {
        createReportDefinition();
    }

    @Test
    public void testReportWalking() throws Exception {
        when(m_resourceDao.findTopLevelResources()).thenReturn(new ArrayList<OnmsResource>(0));
        
        ReportDefinition def = createReportDefinition();
        def.setResourceAttributeKey("ifSpeed");
        def.setResourceAttributeValueMatch("100000000");
        ReportInstance report = def.createReport(m_nodeDao, m_resourceDao, m_fetchStrategy, m_filterDao);

        report.walk();

        assertEquals("results size", 0, report.getResults().size());

        verify(m_resourceDao, times(1)).findTopLevelResources();
    }

    @Test
    public void testUnfilteredResourceAttributeFilteringWithNoMatch() throws Exception {
        MockResourceType resourceType = new MockResourceType();
        resourceType.setName("interfaceSnmp");
        OnmsAttribute attribute = new RrdGraphAttribute("IfInOctets", "something", "something else");
        OnmsResource resource = new OnmsResource("1", "Node One", resourceType, Set.of(attribute), ResourcePath.get("foo"));

        when(m_resourceDao.findTopLevelResources()).thenReturn(List.of(resource));
        
        ReportDefinition def = createReportDefinition();
        def.setResourceAttributeKey("ifSpeed");
        def.setResourceAttributeValueMatch("100000000");
        ReportInstance report = def.createReport(m_nodeDao, m_resourceDao, m_fetchStrategy, m_filterDao);

        report.walk();
        
        assertEquals("results size", 0, report.getResults().size());

        verify(m_resourceDao, times(1)).findTopLevelResources();
    }

    @Test
    public void testUnfilteredResourceAttributeFilteringWithMatch() throws Exception {
        OnmsAttribute rrdAttribute = new RrdGraphAttribute("IfInOctets", "something", "something else");
        ExternalValueAttribute externalValueAttribute = new ExternalValueAttribute("ifSpeed", "100000000");

        Set<OnmsAttribute> attributes = new HashSet<>();
        attributes.add(rrdAttribute);
        attributes.add(externalValueAttribute);

        MockResourceType resourceType = new MockResourceType();
        resourceType.setName("interfaceSnmp");
        OnmsResource resource = new OnmsResource("1", "Node One", resourceType, attributes, ResourcePath.get("foo"));

        when(m_resourceDao.findTopLevelResources()).thenReturn(List.of(resource));
        
        ReportDefinition def = createReportDefinition();
        def.setResourceAttributeKey(externalValueAttribute.getName());
        def.setResourceAttributeValueMatch(externalValueAttribute.getValue());
        ReportInstance report = def.createReport(m_nodeDao, m_resourceDao, m_fetchStrategy, m_filterDao);

        rrdAttribute.setResource(new OnmsResource("1", "Node One", resourceType, Set.of(rrdAttribute), ResourcePath.get("foo")));
        Source source = new Source();
        source.setLabel("result");
        source.setResourceId(rrdAttribute.getResource().getId().toString());
        source.setAttribute(rrdAttribute.getName());
        source.setAggregation("AVERAGE");
        FetchResults results = new FetchResults(new long[] {report.getStartTime()},
                Map.of("result", new double[]{100.0}),
                                                report.getEndTime() - report.getStartTime(),
                                                Collections.emptyMap(),
                                                null);
        when(m_fetchStrategy.fetch(report.getStartTime(),
                                              report.getEndTime(),
                                              1,
                                              0,
                                              null,
                                              null,
                                              List.of(source),
                                              false))
                .thenReturn(results);

        report.walk();

        assertEquals("results size", 1, report.getResults().size());

        verify(m_resourceDao, times(1)).findTopLevelResources();
        verify(m_fetchStrategy, times(1)).fetch(anyLong(), anyLong(), anyLong(), anyInt(), isNull(), isNull(), anyList(), eq(false));
    }

    @Test
    public void testFilteredResourceAttributeFilteringWithNoMatch() throws Exception {
        final OnmsNode node = new OnmsNode();
        node.setId(1);
        node.setLabel("Node One");
        when(m_nodeDao.load(1)).thenReturn(node);
        
        MockResourceType resourceType = new MockResourceType();
        resourceType.setName("interfaceSnmp");
        OnmsAttribute attribute = new RrdGraphAttribute("IfInOctets", "something", "something else");
        OnmsResource resource = new OnmsResource(node.getId().toString(), node.getLabel(), resourceType, Set.of(attribute), ResourcePath.get("foo"));

        ReportDefinition def = createReportDefinition();
        def.getReport().getPackage().setFilter("");
        def.setResourceAttributeKey("ifSpeed");
        def.setResourceAttributeValueMatch("100000000");
        ReportInstance report = def.createReport(m_nodeDao, m_resourceDao, m_fetchStrategy, m_filterDao);

        SortedMap<Integer,String> sortedNodeMap = new TreeMap<Integer, String>();
        sortedNodeMap.put(node.getId(), node.getLabel());
        when(m_filterDao.getNodeMap("")).thenReturn(sortedNodeMap);

        when(m_resourceDao.getResourceForNode(node)).thenReturn(resource);

        report.walk();
        
        assertEquals("results size", 0, report.getResults().size());

        verify(m_nodeDao, times(1)).load(1);
        verify(m_resourceDao, times(1)).getResourceForNode(any(OnmsNode.class));
        verify(m_filterDao, times(1)).getNodeMap("");
    }

    @Test
    public void testFilteredResourceAttributeFilteringWithMatch() throws Exception {
        OnmsAttribute rrdAttribute = new RrdGraphAttribute("IfInOctets", "something", "something else");
        ExternalValueAttribute externalValueAttribute = new ExternalValueAttribute("ifSpeed", "100000000");

        Set<OnmsAttribute> attributes = new HashSet<>();
        attributes.add(rrdAttribute);
        attributes.add(externalValueAttribute);

        final OnmsNode node = new OnmsNode();
        node.setId(1);
        node.setLabel("Node One");
        when(m_nodeDao.load(1)).thenReturn(node);
        
        MockResourceType resourceType = new MockResourceType();
        resourceType.setName("interfaceSnmp");
        OnmsResource resource = new OnmsResource(node.getId().toString(), node.getLabel(), resourceType, attributes, ResourcePath.get("foo"));

        ReportDefinition def = createReportDefinition();
        def.getReport().getPackage().setFilter("");
        def.setResourceAttributeKey(externalValueAttribute.getName());
        def.setResourceAttributeValueMatch(externalValueAttribute.getValue());
        ReportInstance report = def.createReport(m_nodeDao, m_resourceDao, m_fetchStrategy, m_filterDao);

        SortedMap<Integer,String> sortedNodeMap = new TreeMap<Integer, String>();
        sortedNodeMap.put(node.getId(), node.getLabel());
        when(m_filterDao.getNodeMap("")).thenReturn(sortedNodeMap);

        when(m_resourceDao.getResourceForNode(node)).thenReturn(resource);

        Source source = new Source();
        source.setLabel("result");
        source.setResourceId(resource.getId().toString());
        source.setAttribute(rrdAttribute.getName());
        source.setAggregation("AVERAGE");
        FetchResults results = new FetchResults(new long[] {report.getStartTime()},
                Map.of("result", new double[]{100.0}),
                                                report.getEndTime() - report.getStartTime(),
                                                Collections.emptyMap(),
                                                null);
        when(m_fetchStrategy.fetch(report.getStartTime(),
                                              report.getEndTime(),
                                              1,
                                              0,
                                              null,
                                              null,
                                              List.of(source),
                                              false))
                .thenReturn(results);

        report.walk();
        
        assertEquals("results size", 1, report.getResults().size());

        verify(m_nodeDao, times(1)).load(1);
        verify(m_resourceDao, times(1)).getResourceForNode(any(OnmsNode.class));
        verify(m_fetchStrategy, times(1)).fetch(anyLong(), anyLong(), anyLong(), anyInt(), isNull(), isNull(), anyList(), eq(false));
        verify(m_filterDao, times(1)).getNodeMap("");
    }

    private static ReportDefinition createReportDefinition() {
        ReportDefinition def;
        def = new ReportDefinition();
        def.setReport(createPackageReport());
        def.setCount(10);
        def.setConsolidationFunction("AVERAGE");
        def.setRelativeTime(RelativeTime.LASTHOUR);
        def.setResourceTypeMatch("interfaceSnmp");
        def.setAttributeMatch("IfInOctets");
        def.setReportClass(BottomNAttributeStatisticVisitor.class);
        def.afterPropertiesSet();
        return def;
    }

    private static PackageReport createPackageReport() {
        PackageReport packageReport;
        packageReport = new PackageReport();
        packageReport.setDescription("a package!");
        packageReport.setEnabled(true);
        packageReport.setPackage(new StatsdPackage());
        packageReport.setReport(null);
        packageReport.setRetainInterval(Long.valueOf(86400 * 1000));
        packageReport.setSchedule("hmm");
        return packageReport;
    }
}
