<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- 500 오류 안내 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html>
<link rel="stylesheet" href="${contextPath}/resources/css/error.css">
<head>
<meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>500 - 서버 오류</title>
</head>
<body class="auth-body">
<header class="site-header error-site-header">
    <img src="${contextPath}/resources/images/logo.png" alt="SpendOlive" class="error-site-logo">
</header>
    <div class="error-container">
        <div class="error-code">500</div>
        <h1 class="error-title">서버 오류가 발생했습니다</h1>
        <p class="error-message">
            요청을 처리하는 중에 일시적인 문제가 발생했습니다.<br>
            잠시 후 다시 시도해 주시기 바랍니다.
        </p>
        <a href="${contextPath}/spendolive/main.do" class="btn btn-primary">메인으로 돌아가기</a>
    </div>
</body>
</html>
