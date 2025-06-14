<%--
  ~ Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
  ~
  ~ This software is the property of WSO2 Inc. and its suppliers, if any.
  ~ Dissemination of any information or reproduction of any material contained
  ~ herein is strictly forbidden, unless permitted by WSO2 in accordance with
  ~ the WSO2 Commercial License available at http://wso2.com/licenses. For specific
  ~ language governing the permissions and limitations under this license,
  ~ please see the license as well as any agreement you’ve entered into with
  ~ WSO2 governing the purchase of this software and any associated services.
  --%>

<!doctype html>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%
    String stat = request.getParameter("status");
    String statusMessage = request.getParameter("statusMsg");
    if (stat == null || statusMessage == null) {
        stat = "Authentication Error !";
        statusMessage = "Something went wrong during the authentication process. Please try signing in again.";
    }
    session.invalidate();
%>

<html>
<head>
    <jsp:include page="includes/head.jsp"/>
</head>

<body>
<div class="page-content-wrapper">
    <div class="container-fluid ">
        <div class="container">
            <div class="login-form-wrapper">
                <div class="row">
                    <div class="row">
                        <div class="brand-container add-margin-bottom-5x">
                            <div class="row">
                                <div class="col-md-2"></div>
                                <div class="col-xs-6 col-sm-3 col-md-8 col-lg-8">
                                    <img src="images/logo-dark.svg" class="img-responsive brand-spacer login-logo"
                                         alt="WSO2 Open Banking"/>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-2"></div>
                    <div class="col-xs-12 col-sm-12 col-md-8 col-lg-8 login">
                        <div class="boarder-all data-container error">
                            <div class="clearfix"></div>
                            <form class="form-horizontal">
                                <div class="login-form" style="padding:20px">
                                    <h3 class="ui header"><%=Encode.forHtml(stat)%>
                                    </h3>
                                    <div class="ui body">
                                        <p class="padding-bottom-double">
                                            <%=Encode.forHtmlContent(statusMessage)%>
                                        </p>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="includes/footer.jsp"/>

</body>
</html>