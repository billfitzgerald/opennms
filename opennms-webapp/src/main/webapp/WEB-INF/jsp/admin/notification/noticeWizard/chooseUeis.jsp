<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2022 The OpenNMS Group, Inc.
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
	import="org.opennms.web.admin.notification.noticeWizard.*"
%>

<%@ taglib uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" prefix="e"%>

<%@ page import="org.opennms.web.utils.Bootstrap" %>
<% Bootstrap.with(pageContext)
          .headTitle("Choose Event")
          .headTitle("Admin")
          .breadcrumb("Admin", "admin/index.jsp")
          .breadcrumb("Configure Notifications", "admin/notification/index.jsp")
          .breadcrumb("Event Notifications", "admin/notification/noticeWizard/eventNotices.htm")
          .breadcrumb("Choose Event")
          .build(request);
%>
<jsp:directive.include file="/includes/bootstrap.jsp" />

<script type="text/javascript" >
    function doReset()
    {
      $('#uei-list-filter').val('');
      $('#filteringModal').modal('show');
    }

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

$(document).ready(function() {

  function toggleRegexpField() {
    var ueiLabel = $("#uei option:selected").text();
    $('#regexp').prop('disabled', ueiLabel !== 'REGEX_FIELD');
  }

  var allUeiOptions = $("#uei > option").clone();
  var unavailableUeiOptGroup = $("#uei > optgroup").clone();

  function filterUeiList() {
    var filterText = $("#uei-list-filter").val().toLowerCase();
    $("#uei").empty();
    if (filterText.length == 0) {
      $("#uei").append(allUeiOptions);
      $("#uei").append(unavailableUeiOptGroup);
    } else {
      allUeiOptions.each(function ( index, element ) {
        if ( $(this).text().toLowerCase().indexOf(filterText) != -1) {
          $("#uei").append( $(this) );
        }
      });
    }
    $('#filteringModal').modal('hide');
    $('#uei-list-filter').focus();
    toggleRegexpField();
  }

  $("#uei-list-filter").keydown(function(event) {
    if (event.which == 27) {
      event.preventDefault();
      if ($("#uei-list-filter").val().length > 0) {
        doReset();
      }
    }
  });

  $("#uei").keydown(function(event) {
    if (event.which == 27) {
      event.preventDefault();
      if ($("#uei-list-filter").val().length > 0) {
        $('#uei-list-filter').val("");
        $('#filteringModal').modal('show');
      }
    }
  });

  $("#uei-list-filter").keypress(function(event) {
    if (event.which == 13) {
      event.preventDefault();
      $('#filteringModal').modal('show');
    }
  });

  $("#filteringModal").on( "shown.bs.modal", function() { filterUeiList(); } );

  $("select#uei").change(function(e) {
    toggleRegexpField();
  });

  $('#regexp').change(function(e) {
    var value = e.target.value;
    if (!value.match("^~")) {
      alert("Invalid regex field: [" + value + "], value does not start with ~\n" +
        "Resetting value back to default.\n"
      );
      e.target.value = e.target.defaultValue;
      return false;
    }
    var uei = $('#uei')[0];
    $(uei.options[uei.selectedIndex]).val(value);
    uei.selectmenu('refresh');
  });

  toggleRegexpField();

  if ($('#regexp').val() === '~^$') {
    $('#regexp').prop('disabled', true);
  }

});

</script>

<div id="filteringModal" class="modal fade" tabindex="-1">
  <div class="modal-dialog modal-sm">
    <div class="modal-content">
      <div class="modal-body">
        <i class="fa fa-spinner fa-spin"></i> Filtering
      </div>
    </div>
  </div>
</div>

<h2>${e:forHtml(model.title)}</h2>

<form method="post" name="events"
      action="admin/notification/noticeWizard/notificationWizard" >
<input type="hidden" name="sourcePage" value="<%=NotificationWizardServlet.SOURCE_PAGE_UEIS%>"/>

<div class="row">
  <div class="col-md-12 col-lg-8">
    <div class="card">
      <div class="card-header">
        <span>Choose the event UEI that will trigger this notification.</span>
      </div>
      <table class="table table-sm">
        <tr>
          <td valign="top" align="left">
            <div class="form-group">
              <label for="uei" class="col-form-label">Events</label>
              <input id="uei-list-filter" name="uei-list-filter" type="text" class="form-control" size="96" value="" placeholder="Filter displayed events..." />
              <select id="uei" name="uei" class="form-control" size="20" >
              ${model.eventSelect}
              </select>
            </div>
            <div class="form-group">
              <label for="regexp" class="col-form-label">Regular Expression Field</label>
              <input id="regexp" name="regexp" type="text" class="form-control" size="96" value="${e:forHtmlAttribute(model.noticeUei)}" />
            </div>
          </td>
        </tr>
        <tr>
          <td colspan="2">
            <input type="reset" class="btn btn-secondary" onclick="doReset()" />
          </td>
        </tr>
        <tr>
          <td colspan="2">
           <a HREF="javascript:next()">Next &#155;&#155;&#155;</a>
          </td>
        </tr>
      </table>
    </div> <!-- panel -->
  </div> <!-- column -->
</div> <!-- row -->

</form>

<jsp:include page="/includes/bootstrap-footer.jsp" flush="false" />
