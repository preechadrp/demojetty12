<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
<link rel="stylesheet" type="text/css" href="./css/my.css">
</head>
<body>
	<h2 class="test1">Hello from JSP running on Jetty 12 Embedded!</h2>
	<div class="test2">
		<%
		out.println("Current time: " + new java.util.Date());
		%>
	</div>
</body>
</html>