<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html>
        <link rel="stylesheet" href="${contextPath}/resources/css/error.css">
<head>
<meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>400 - 잘못된 요청</title>
</head>
<body class="auth-body">
<header class="site-header error-site-header">
    <img src="${contextPath}/resources/images/logo.png" alt="SpendOlive" class="error-site-logo">
</header>
    <div class="error-container">
        <div class="error-code">400</div>
        <h1 class="error-title">잘못된 요청입니다</h1>
        <p class="error-message">
            요청하신 주소나 입력값이 올바르지 않습니다.<br>
            입력하신 내용을 다시 한번 확인해 주세요.
        </p>
        <a href="${contextPath}/spendolive/main.do" class="btn btn-primary">메인으로 돌아가기</a>
    </div>
</body>
</html>