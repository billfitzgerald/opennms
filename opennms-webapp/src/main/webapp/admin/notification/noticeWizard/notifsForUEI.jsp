<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2022 The OpenNMS Group, Inc.
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

--%>

<%@page language="java"
	contentType="text/html"
	session="true"
	import="java.util.*,
		org.opennms.web.admin.notification.noticeWizard.*,
		org.opennms.netmgt.config.*,
		org.opennms.netmgt.config.notifications.*
	"
%>
<%@ page import="org.opennms.core.utils.WebSecurityUtils" %>

<%!
    public void init() throws ServletException {
        try {
            NotificationFactory.init();
        }
        catch( Exception e ) {
            throw new ServletException( "Cannot load configuration file", e );
        }
    }
%>

<%
	String uei = request.getParameter("uei");
	Map<String, Notification> allNotifications=NotificationFactory.getInstance().getNotifications();
	List<Notification> notifsForUEI=new ArrayList<>();
	for(String key : allNotifications.keySet()) {
	    Notification notif=allNotifications.get(key);
		if(notif.getUei().equals(uei)) {
		    notifsForUEI.add(notif);
		}
	}
%>

<%@ page import="org.opennms.web.utils.Bootstrap" %>
<% Bootstrap.with(pageContext)
          .headTitle("Choose Event")
          .headTitle("Admin")
          .breadcrumb("Admin", "admin/index.jsp")
          .breadcrumb("Configure Notifications", "admin/notification/index.jsp")
          .breadcrumb("Event Notifications", "admin/notification/noticeWizard/eventNotices.htm")
          .breadcrumb("Existing notifications for UEI")
          .build(request);
%>
<jsp:directive.include file="/includes/bootstrap.jsp" />

<script type="text/javascript" >

    function next()
    {
        if (document.events.uei.selectedIndex==-1)
        {
            alert("Please select a uei to associate with this notification.");
        }
        else
        {
            document.events.submit();
        }
    }
	function submitEditForm(noticeName) {
		document.getElementById("notice").value=noticeName;
		document.editForm.submit();
	}

</script>
<!-- Hidden form that will cause the notification to be edited -->
<form action="admin/notification/noticeWizard/notificationWizard"  method="post" name="editForm">
	<input type="hidden" name="sourcePage" value="<%=NotificationWizardServlet.SOURCE_PAGE_NOTIFS_FOR_UEI%>"/>
	<input type="hidden" name="userAction" value="edit"/>
	<input type="hidden" id="notice" name="notice" value=""/>
</form>

<form action="admin/notification/noticeWizard/notificationWizard"  method="post" name="newNotificationForm">
	<input type="hidden" name="sourcePage" value="<%=NotificationWizardServlet.SOURCE_PAGE_NOTIFS_FOR_UEI%>"/>
	<input type="hidden" name="userAction" value="new"/>
	<input type="hidden" name="uei" value="<%=WebSecurityUtils.sanitizeString(uei)%>"/>
</form>

<div class="card">
  <div class="card-header">
    <span>Existing Notifications for UEI <%=WebSecurityUtils.sanitizeString(uei)%></span>
  </div>
      <table class="table table-sm">
      	 <tr><th>Name</th><th>Description</th><th>Rule</th><th>Destination path</th><th>Varbinds</th><th>Actions</th></tr>
      <% for(Notification notif : notifsForUEI) { 
          	String varbindDescription="";
          	Varbind varbind=notif.getVarbind();
          	if(varbind!=null) {
          		varbindDescription=varbind.getVbname()+"="+varbind.getVbvalue();
          	}
      		%>
	        <tr>
	        	<td><%=WebSecurityUtils.sanitizeString(notif.getName())%></td>
	        	<td><%=WebSecurityUtils.sanitizeString(notif.getDescription().orElse(""))%></td>
	        	<td><%=notif.getRule()%></td>
	        	<td><%=WebSecurityUtils.sanitizeString(notif.getDestinationPath())%></td>
	        	<td><%=varbindDescription%></td>
	        	<td><a href="javascript: void submitEditForm('<%=notif.getName()%>');">Edit</a></td>
			</tr>
<% } %>
	  </table>
	<div class="card-footer">
		<a class="btn btn-secondary" href="javascript: document.newNotificationForm.submit()">Create a new notification</a>
	</div>
</div> <!-- panel -->

<jsp:include page="/includes/bootstrap-footer.jsp" flush="false" />
