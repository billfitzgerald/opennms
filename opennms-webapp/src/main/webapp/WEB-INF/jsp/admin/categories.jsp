<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2014 The OpenNMS Group, Inc.
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

<%@ page contentType="text/html;charset=UTF-8" language="java" import="org.opennms.web.api.Util" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" prefix="e"%>

<%@ page import="org.opennms.web.utils.Bootstrap" %>
<% Bootstrap.with(pageContext)
          .headTitle("Categories")
          .breadcrumb("Admin", "admin/index.jsp")
          .breadcrumb("Categories")
          .build(request);
%>
<jsp:directive.include file="/includes/bootstrap.jsp" />

<script type="text/javascript">

   var surveillanceCategories = {
		   <c:set var="first" value="true"/>
		   <c:forEach var="surveillanceCat" items="${surveillanceCategories}" varStatus="count">
		     <c:choose>
		       <c:when test="${first == true}">
		         <c:set var="first" value="false" />
		         "${surveillanceCat}":"${surveillanceCat}"
		       </c:when>
		       <c:otherwise>
		         ,"${surveillanceCat}":"${surveillanceCat}"
		       </c:otherwise>
		     </c:choose>
		   </c:forEach>
   };

   function deleteCategory(categoryName, categoryId){
	   if(surveillanceCategories.hasOwnProperty(categoryName)){
           if(confirm("This Surveillance Category is also in your surveillance-views.xml config.\nPlease edit surveillance-views.xml to reflect changes.")){
               location = "<%= Util.calculateUrlBase(request, "admin/categories.htm") %>?removeCategoryId=" + categoryId;
           }
       }else{
           location = "<%= Util.calculateUrlBase(request, "admin/categories.htm") %>?removeCategoryId=" + categoryId;
       }
   }
</script>

<div class="card">
  <div class="card-header">
    <span>Surveillance Categories</span>
  </div>
  <table class="table table-sm table-responsive">
    <tr>
      <th>Delete</th>
      <th>Edit</th>
      <th>Category</th>
    </tr>
    <c:forEach items="${categories}" var="category">
      <tr>
        <td><a href="javascript:void(0);" onclick="deleteCategory('${e:forJavaScript(category.name)}', ${category.id})" ><i class="fa fa-trash-o fa-2x"></i></a></td>
        <td><a href="admin/categories.htm?categoryid=${category.id}&edit"><i class="fa fa-edit fa-2x"></i></a></td>
        <td><a href="admin/categories.htm?categoryid=${category.id}">${fn:escapeXml(category.name)}</a></td>
      </tr>
    </c:forEach>
    <tr>
      <td colspan="3">
        <form role="form" class="form-inline mt-4" action="admin/categories.htm">
          <input type="textfield" class="form-control" size="40" name="newCategoryName" placeholder="Category name"/>
          <button type="submit" class="btn btn-secondary ml-2"><i class="fa fa-plus"></i> Add New Category</button>
        </form>
    </tr>
  </table>
</div> <!-- panel -->

<jsp:include page="/includes/bootstrap-footer.jsp" flush="false"/>
