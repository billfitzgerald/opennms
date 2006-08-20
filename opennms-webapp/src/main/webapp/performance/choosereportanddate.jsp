<%--

//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2003 Feb 07: Fixed URLEncoder issues.
// 2002 Nov 26: Fixed breadcrumbs issue.
// 2002 Oct 24: Corrected AM/PM ordering.
// 
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//

--%>

<%@page language="java"
	contentType="text/html"
	session="true"
	import="java.util.*,
		org.opennms.web.*,
		org.opennms.web.graph.*,
		org.opennms.web.performance.*,
		java.util.Calendar
	"
%>

<%!
    public PerformanceModel model = null;

    public void init() throws ServletException {
        try {
            this.model = new PerformanceModel(ServletInitializer.getHomeDir());
        } catch (Throwable t) {
            throw new ServletException("Could not initialize the PerformanceModel", t);
        }
    }
%>

<%
    // optional parameter node
    String nodeIdString = request.getParameter("node");

    //optional parameter domain
    String domain = request.getParameter("domain");
    if(( nodeIdString == null ) && ( domain == null )) {
        throw new MissingParameterException("node or domain");
    }

    // optional parameter resource
    String resource = request.getParameter("resource");    
    
    PrefabGraph[] graphs = null;

    // FIXME: getQueries throws an exception if nodeId doesn't have snmp data!
    // This doesn't seem to be an issue any more, at least from performance reports
    if((resource == null || resource.length() == 0) && (nodeIdString != null )) {
        graphs = this.model.getQueries(Integer.parseInt(nodeIdString));
    } else if (nodeIdString != null ) {
        boolean includeNodeQueries = false;
		graphs = this.model.getQueries(Integer.parseInt(nodeIdString), resource, includeNodeQueries);
    } else {
        graphs = this.model.getQueriesForDomain(domain, resource);
    }

    /*
     * Order the graphs by their order in the properties file.
     * Note: PrefabGraph implements the Comparable interface.
     */
    Arrays.sort(graphs);
    
    Calendar now = Calendar.getInstance();
    Calendar yesterday = Calendar.getInstance();
    yesterday.add( Calendar.DATE, -1 );
%>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Choose Report and Date" />
  <jsp:param name="headTitle" value="Choose Report and Date" />
  <jsp:param name="headTitle" value="Performance" />
  <jsp:param name="headTitle" value="Reports" />
  <jsp:param name="breadcrumb" value="<a href='report/index.jsp'>Reports</a>" />
  <jsp:param name="breadcrumb" value="<a href='performance/index.jsp'>Performance</a>" />
  <jsp:param name="breadcrumb" value="Choose Report and Date" />
</jsp:include>

<script language="Javascript" type="text/javascript" >
  function validateReport()
  {
      var isChecked = false
      for( i = 0; i < document.report.reports.length; i++ ) 
      {
         //make sure something is checked before proceeding
         if (document.report.reports[i].selected)
         {
            isChecked=true;
         }
      }
      
      if (!isChecked)
      {
          alert("Please check the reports that you would like to see.");
      }
      return isChecked;
  }
  
  function submitForm()
  {
      if (validateReport())
      {
          document.report.submit();
      }
  }
</script>

<h3>Network Performance Data</h3>

<% if (nodeIdString != null ) { %>
  <form method="get" name="report" action="graph/results">
<% } else { %>
  <form method="get" name="report" action="graph/domainResults">
<% } %>
  <input type="hidden" name="type" value="performance"/>
  <%=Util.makeHiddenTags(request)%>
  <% if(resource == null) { %>
    <input type="hidden" name="resource" value="" />
  <% } %>

  <div style="width: 40%; float: left;">
    <p>
      <% if (graphs.length > 0) { %>
        Please choose one or more of the following queries to perform on
        the node or interface.
      <% } else { %>
        No standard reports being collected for this node or interface.
      <% } %>
    </p>

    <p>
      <select name="reports" multiple="multiple" size="10">
        <% for( int i = 0; i < graphs.length; i++ ) { %>
          <option VALUE=<%=graphs[i].getName()%>><%=graphs[i].getTitle()%></option>
        <% } %>
      </select>
    </p>
  </div>

  <div style="width: 60%; float: left;">
    Query Start Time<br/>

    <select name="startMonth" size="1">
      <% for( int i = 0; i < 12; i++ ) { %>
        <option value="<%=i%>" <% if( yesterday.get( Calendar.MONTH ) == i ) out.print("selected ");%>><%=months[i]%></option>
      <% } %>
    </select>

    <input type="text" name="startDate" size="4" maxlength="2" value="<%=yesterday.get( Calendar.DATE )%>" />
    <input type="text" name="startYear" size="6" maxlength="4" value="<%=yesterday.get( Calendar.YEAR )%>" />

    <select name="startHour" size="1">
      <% int yesterdayHour = yesterday.get( Calendar.HOUR_OF_DAY ); %>
      <% for( int i = 1; i < 25; i++ ) { %>
        <option value="<%=i%>" <% if( yesterdayHour == i ) out.print( "selected " ); %>>
          <%=(i<13) ? i : i-12%>&nbsp;<%=(i<12 | i>23) ? "AM" : "PM"%>
        </option>
      <% } %>
    </select>

    <br/>
    <br/>

    Query End Time<br/>

    <select name="endMonth" size="1">
      <% for( int i = 0; i < 12; i++ ) { %>
        <option value="<%=i%>" <% if( now.get( Calendar.MONTH ) == i ) out.print("selected ");%>><%=months[i]%></option>
      <% } %>
    </select>

    <input type="text" name="endDate" size="4" maxlength="2" value="<%=now.get( Calendar.DATE )%>" />
    <input type="text" name="endYear" size="6" maxlength="4" value="<%=now.get( Calendar.YEAR )%>" />

    <select name="endHour" size="1">
      <% int nowHour = now.get( Calendar.HOUR_OF_DAY ); %>
      <% for( int i = 1; i < 25; i++ ) { %>
        <option value="<%=i%>" <% if( nowHour == i ) out.print( "selected " ); %>>
          <%=(i<13) ? i : i-12%>&nbsp;<%=(i<12 | i>23) ? "AM" : "PM"%>
        </option>
      <% } %>
    </select>
  </div>

  <div class="spacer"><!-- --></div>
  <br/>

   <input type="button" value="Submit" onclick="submitForm()"/>
   <input type="reset" />
</form>

<jsp:include page="/includes/footer.jsp" flush="false" />


<%!
    //note these run from 0-11, this is because of java.util.Calendar!
    public static String[] months = new String[] {
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    };
%>
