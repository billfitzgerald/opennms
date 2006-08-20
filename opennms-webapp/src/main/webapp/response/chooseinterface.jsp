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
// 2002 Nov 12: Added response time reports to webUI. Based on original
//              performance reports.
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
		org.opennms.web.response.*,
		java.util.Calendar
	"
%>

<%!
    public ResponseTimeModel model = null;
    
    public void init() throws ServletException {
        try {
            this.model = new ResponseTimeModel(ServletInitializer.getHomeDir());
        }
        catch (Throwable t) {
            throw new ServletException("Could not initialize the ResponseTimeModel", t);
        }
    } 
%>

<%
    String[] requiredParameters = { "node", "endUrl" };

    String nodeId = request.getParameter("node");
    String endUrl = request.getParameter("endUrl");
    
    if (nodeId == null) {
        throw new MissingParameterException("node", requiredParameters);
    }
    
    if (endUrl == null) {
        throw new MissingParameterException("endUrl", requiredParameters);
    }
    
    ArrayList intfs = this.model.getQueryableInterfacesForNode(nodeId);
%>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Choose Interface" />
  <jsp:param name="headTitle" value="Choose Interface" />
  <jsp:param name="headTitle" value="Response Time" />
  <jsp:param name="headTitle" value="Reports" />
  <jsp:param name="breadcrumb" value="<a href='report/index.jsp'>Reports</a>" />
  <jsp:param name="breadcrumb" value="<a href='response/index.jsp'>Response Time</a>" />
  <jsp:param name="breadcrumb" value="Choose Interface"  />
</jsp:include>

  <script language="Javascript" type="text/javascript" >
      function validateRRD()
      {
          var isChecked = false
          for( i = 0; i < document.report.resource.length; i++ )
          {
              //make sure something is checked before proceeding
              if (document.report.resource[i].selected)
              {
                  isChecked=true;
              }
          }
  
          if (!isChecked)
          {
              alert("Please check the interfaces that you would like to report on.");
          }
          return isChecked;
      }
  
      function submitForm()
      {
          if(validateRRD())
          {
              document.report.submit();
          }
      }
  </script>

<form method="get" name="report" action="<%=endUrl%>">
  <%=Util.makeHiddenTags(request, new String[] {"endUrl"})%>

  <h3>Choose an Interface to Query</h3>

  <p>
    The node that you have chosen has response data for multiple interfaces.
    Please choose the interface that you wish to query.
  </p>

  <select name="resource" size="10">
    <% for(int i=0; i < intfs.size(); i++) { %>
      <option value="<%=intfs.get(i)%>" <%=(i==0) ? "selected" : ""%>><%=intfs.get(i)%></option>
    <% } %>
  </select>

  <br/>
  <br/>

  <input type="button" value="Submit" onclick="submitForm()" />
</form>

<jsp:include page="/includes/footer.jsp" flush="false" />
