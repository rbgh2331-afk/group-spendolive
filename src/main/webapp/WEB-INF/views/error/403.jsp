<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html>
<link rel="stylesheet" href="${contextPath}/resources/css/error.css">
<head>
<meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>403 - 접근 권한 없음</title>
</head>
<body class="auth-body">
<header class="site-header error-site-header">
    <img src="${contextPath}/resources/images/logo.png" alt="SpendOlive" class="error-site-logo">
</header>
    <div class="error-container">
        <div class="error-code">403</div>
        <h1 class="error-title">접근 권한이 없습니다</h1>
        <p class="error-message">
            해당 페이지에 접근할 수 있는 권한이 없습니다.<br>
            다른 계정으로 로그인하시거나 관리자에게 문의하세요.
        </p>
        <div class="btn-group">
            <a href="javascript:history.back();" class="btn btn-secondary">이전 페이지</a>
            <a href="${contextPath}/spendolive/main.do" class="btn btn-primary">메인으로</a>
        </div>
    </div>
</body>
</html>
</html>