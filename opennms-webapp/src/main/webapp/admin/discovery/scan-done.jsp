<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
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

--%>

<%@page language="java"
	contentType="text/html"
	session="true"
%>

<%@ page import="org.opennms.web.utils.Bootstrap" %>
<% Bootstrap.with(pageContext)
          .headTitle("Discovery")
          .headTitle("Admin")
          .breadcrumb("Admin", "admin/index.jsp")
          .breadcrumb("Discovery", "admin/discovery/index.jsp")
          .breadcrumb("Configuration Updated")
          .build(request);
%>
<jsp:directive.include file="/includes/bootstrap.jsp" />

<div class="card">
  <div class="card-header">
    <span>Discovery Scan Started</span>
  </div>
  <div class="card-body">
    <p>
      The discovery scan has been submitted. It will execute immediately.
    </p>

    <p><a href='admin/index.jsp'>Return to the admin page</a></p>
    <p><a href='admin/discovery/index.jsp'>Return to the discovery configuration page</a></p>

  </div> <!-- card-body -->
</div> <!-- panel -->

<jsp:include page="/includes/bootstrap-footer.jsp" flush="false" />

