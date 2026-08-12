<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- 결제 처리 중에는 뒤쪽 화면을 조작하지 못하도록 전체 화면 팝업을 표시합니다. --%>
<div id="reportStatusOverlay"
     class="status-overlay"
     role="dialog"
     aria-modal="true"
     aria-labelledby="reportStatusTitle"
     aria-describedby="reportStatusMessage"
     hidden>
    <div class="status-box">
        <div id="reportStatusSpinner"
             class="status-spinner"
             aria-hidden="true"></div>

        <div id="reportStatusIcon"
             class="status-icon"
             aria-hidden="true"
             hidden></div>

        <h3 id="reportStatusTitle">신고를 처리하고 있습니다.</h3>
        <p id="reportStatusMessage">
            창을 닫거나 새로고침하지 말아주세요.
        </p>

        <div id="reportStatusActions"
             class="status-actions"
             hidden>
            <button type="button"
                    id="StatusCloseButton"
                    class="btn btn-outline">
                확인
            </button>
            <button type="button"
                    id="StatusActionButton"
                    class="btn btn-primary"
                    hidden>
                이동하기
            </button>
        </div>
    </div>
</div>
