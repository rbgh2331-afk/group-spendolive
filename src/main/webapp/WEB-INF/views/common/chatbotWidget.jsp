<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>

<%-- 공통 챗봇 위젯 --%>
<button id="chatbotToggle" class="chatbot-toggle">🙋🏻‍♀️</button>

<div id="chatbotPanel" class="chatbot-panel">

  <div class="chatbot-header">
    <div>
        <p class="eyebrow">SPENDOLIVE BOT</p>
        <strong>SpendOlive 챗봇</strong>
        <span>FAQ 기반으로 답변해드려요</span>
    </div>
    <button id="chatbotClose" class="chatbot-close" aria-label="챗봇 닫기">×</button>
</div>
  <div id="chatbotBody" class="chatbot-body">
    <div class="chatbot-msg bot">안녕하세요! 궁금한 점을 물어보세요 🙂</div>
  </div>
  <div class="chatbot-footer">
    <input id="chatbotInput" type="text" placeholder="질문을 입력하세요">
    <button id="chatbotSend">전송</button>
  </div>
</div>
