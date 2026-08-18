<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>

<%-- 공통 폰트 설정 --%>
<!-- 토글 버튼 -->
<button id="fontToggle" class="font-toggle2" title="화면 및 글자 설정">간편</button>

<!-- 네이버 스타일 설정 패널 -->
<div id="fontPanel" class="font-panel">
  <div class="font-panel-header">
    <h3 class="font-panel-title">화면 설정</h3>
    <button type="button" id="fontClose" class="font-close-btn" aria-label="닫기">×</button>
  </div>

  <div class="font-panel-body">
    <div class="font-section">
      <div class="section-title">글자 크기</div>
      <div class="size-card-group">
        <button type="button" id="btn-font-down" class="size-card" title="글자 크기 축소">
          <span class="check-badge">✓</span>
          <div class="preview-box size-sm">-</div>
          <span class="card-label">축소</span>
        </button>
        <button type="button" id="btn-font-reset" class="size-card" title="글자 크기 기본">
          <span class="check-badge">✓</span>
          <div class="preview-box size-md">글자</div>
          <span class="card-label">크기조절</span>
        </button>
        <button type="button" id="btn-font-up" class="size-card" title="글자 크기 확대">
          <span class="check-badge">✓</span>
          <div class="preview-box size-lg">+</div>
          <span class="card-label">확대</span>
        </button>
      </div>
    </div>

    <div class="font-divider"></div>

   <div class="font-section">
  <div class="section-title">글꼴 선택</div>
  <div class="font-card-group">
    <button type="button" class="font-card" data-font="system">
      <span class="check-badge">✓</span>
      <div class="preview-box font-preview-sys">가<span>Aa</span></div>
      <span class="card-label">기본</span>
    </button>

    <button type="button" class="font-card" data-font="jua">
      <span class="check-badge">✓</span>
      <div class="preview-box font-preview-jua">가<span>Aa</span></div>
      <span class="card-label">주아체</span>
    </button>

    <button type="button" class="font-card" data-font="sans-serif">
      <span class="check-badge">✓</span>
      <div class="preview-box font-preview-sans">가<span>Aa</span></div>
      <span class="card-label">고딕</span>
    </button>

    <button type="button" class="font-card" data-font="serif">
      <span class="check-badge">✓</span>
      <div class="preview-box font-preview-serif">가<span>Aa</span></div>
      <span class="card-label">명조</span>
    </button>

    <button type="button" class="font-card" data-font="monospace">
      <span class="check-badge">✓</span>
      <div class="preview-box font-preview-mono">가<span>Aa</span></div>
      <span class="card-label">고정폭</span>
    </button>
  </div>
</div>
  </div>
</div>
