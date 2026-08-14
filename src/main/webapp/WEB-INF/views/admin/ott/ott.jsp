<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<c:set var="isEdit" value="${not empty editService}" />

<div class="admin-main" data-admin-page="ott" data-admin-title="OTT 관리" data-admin-default-section="${isEdit ? 'edit' : 'manage'}">
    <section class="hero">
        <div>
            <div class="hero-kicker">OTT Management</div>
            <h1>OTT 관리</h1>
            <p>등록된 OTT 목록을 확인하고, 신규 등록과 기존 정보 수정을 각각 분리해 관리합니다.</p>
        </div>
    </section>

    <c:if test="${not empty msg}"><div class="flash-ok"><c:out value="${msg}" /></div></c:if>
    <c:if test="${not empty errorMsg}"><div class="flash-err"><c:out value="${errorMsg}" /></div></c:if>

    <div class="admin-local-tabs" aria-label="OTT 관리 세부 메뉴">
        <button type="button" class="admin-local-tab active" data-admin-section-target="manage">OTT 목록·숨김</button>
        <button type="button" class="admin-local-tab" data-admin-section-target="edit">OTT 수정</button>
        <button type="button" class="admin-local-tab" data-admin-section-target="add">OTT 추가</button>
    </div>

    <section class="admin-page-section is-active" data-admin-section="manage" data-admin-hash="manage">
        <div class="panel">
            <div class="panel-header">
                <div class="panel-title">
                    <div class="section-kicker">OTT Services</div>
                    <h2>OTT 항목 목록 (${empty serviceList ? 0 : serviceList.size()}개)</h2>
                    <p>수정 버튼을 누르면 OTT 수정 탭에서 선택한 서비스의 정보를 변경할 수 있습니다.</p>
                </div>
                <button type="button" class="btn primary" data-admin-section-target="add">+ OTT 항목 추가</button>
            </div>

            <c:choose>
                <c:when test="${empty serviceList}">
                    <div class="admin-empty-filter">등록된 OTT 서비스가 없습니다. OTT 추가 탭에서 먼저 등록해 주세요.</div>
                </c:when>
                <c:otherwise>
                    <div class="card-grid">
                        <c:forEach var="service" items="${serviceList}">
                            <c:set var="serviceNameLower" value="${fn:toLowerCase(service.service_name)}" />
                            <c:set var="logoClass" value="" />
                            <c:choose>
                                <c:when test="${fn:contains(serviceNameLower, 'netflix')}"><c:set var="logoClass" value="netflix" /></c:when>
                                <c:when test="${fn:contains(serviceNameLower, 'disney')}"><c:set var="logoClass" value="disney" /></c:when>
                                <c:when test="${fn:contains(serviceNameLower, 'tving')}"><c:set var="logoClass" value="tving" /></c:when>
                                <c:when test="${fn:contains(serviceNameLower, 'wavve')}"><c:set var="logoClass" value="wavve" /></c:when>
                                <c:when test="${fn:contains(serviceNameLower, 'watcha')}"><c:set var="logoClass" value="watcha" /></c:when>
                                <c:when test="${fn:contains(serviceNameLower, 'laftel')}"><c:set var="logoClass" value="laftel" /></c:when>
                            </c:choose>

                            <article class="manage-card">
                                <div class="manage-card-head">
                                    <div class="ott-cell">
                                        <div class="ott-logo ${logoClass}"><c:out value="${fn:substring(service.service_name, 0, 1)}" /></div>
                                        <div>
                                            <h3><c:out value="${service.service_name}" /></h3>
                                            <p><c:out value="${empty service.fixed_plan_name ? '프리미엄' : service.fixed_plan_name}" /></p>
                                        </div>
                                    </div>
                                    <c:choose>
                                        <c:when test="${service.share_yn eq 'N'}"><span class="badge gray">숨김</span></c:when>
                                        <c:otherwise><span class="badge green">사용중</span></c:otherwise>
                                    </c:choose>
                                </div>

                                <div class="meta-grid">
                                    <div class="meta-box"><small>최고 멤버십</small><strong><fmt:formatNumber value="${service.base_price}" type="number" />원</strong></div>
                                    <div class="meta-box"><small>최종 기준금액</small><strong><fmt:formatNumber value="${service.default_price}" type="number" />원</strong></div>
                                    <div class="meta-box"><small>최대 인원</small><strong>${service.max_member_limit}명</strong></div>
                                    <div class="meta-box"><small>추가 멤버 비용</small><strong><fmt:formatNumber value="${service.extra_member_fee}" type="number" />원 × ${service.extra_member_count}명</strong></div>
                                    <div class="meta-box"><small>수수료</small><strong><fmt:formatNumber value="${service.platform_fee_rate}" pattern="0.##" />%</strong></div>
                                    <div class="meta-box"><small>위험도</small><strong><c:out value="${empty service.risk_level ? '-' : service.risk_level}" /></strong></div>
                                </div>

                                <c:if test="${not empty service.block_reason}">
                                    <p>차단·주의 사유: <c:out value="${service.block_reason}" /></p>
                                </c:if>

                                <div class="table-actions">
                                    <a class="mini-btn" href="${contextPath}/admin/ott/edit.do?ott_service_id=${service.ott_service_id}#edit">수정</a>
                                    <form action="${contextPath}/admin/ott/delete.do" method="post">
                                        <input type="hidden" name="ott_service_id" value="${service.ott_service_id}">
                                        <button type="submit" class="mini-btn danger">숨김</button>
                                    </form>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </section>

    <section class="admin-page-section" data-admin-section="add" data-admin-hash="add" hidden>
        <div class="panel">
            <div class="panel-header">
                <div class="panel-title">
                    <div class="section-kicker">Add OTT</div>
                    <h2>OTT 항목 추가</h2>
                    <p>새로운 OTT 서비스 정보를 입력해 등록합니다.</p>
                </div>
                <button type="button" class="btn ghost" data-admin-section-target="manage">목록으로</button>
            </div>

            <form id="adminOttInsertForm" action="${contextPath}/admin/ott/insert.do" method="post">
                <div class="form-grid">
                    <div class="form-field"><label>OTT 이름</label><input class="form-input" type="text" name="service_name" placeholder="예: Netflix" required></div>
                    <div class="form-field"><label>고정 멤버십명</label><input class="form-input" type="text" name="fixed_plan_name" value="프리미엄" placeholder="예: 프리미엄"></div>
                    <div class="form-field"><label>최고 멤버십 가격</label><input class="form-input" type="number" name="base_price" min="0" placeholder="예: 17000" required></div>
                    <div class="form-field"><label>최종 기준금액</label><input class="form-input" type="number" name="default_price" min="0" placeholder="예: 27000"></div>
                    <div class="form-field"><label>최대 인원</label><input class="form-input" type="number" name="max_member_limit" value="4" min="1" max="6" required></div>
                    <div class="form-field"><label>추가 멤버 비용</label><input class="form-input" type="number" name="extra_member_fee" value="0" min="0"></div>
                    <div class="form-field"><label>추가 멤버 수</label><input class="form-input" type="number" name="extra_member_count" value="0" min="0"></div>
                    <div class="form-field"><label>플랫폼 수수료율(%)</label><input class="form-input" type="number" step="0.01" name="platform_fee_rate" value="3" min="0"></div>
                    <div class="form-field">
                        <label>공유 가능 여부</label>
                        <select class="form-input" name="share_yn"><option value="Y" selected>사용중</option><option value="N">숨김</option></select>
                    </div>
                    <div class="form-field">
                        <label>위험도</label>
                        <select class="form-input" name="risk_level"><option value="" selected>선택 안 함</option><option value="LOW">LOW</option><option value="MEDIUM">MEDIUM</option><option value="HIGH">HIGH</option></select>
                    </div>
                </div>

                <div class="form-field">
                    <label>차단·주의 사유</label>
                    <textarea class="form-textarea" name="block_reason" placeholder="공유 불가 또는 주의 사유가 있다면 입력하세요."></textarea>
                </div>

                <div class="toolbar admin-toolbar-end">
                    <button type="button" class="btn ghost" data-admin-section-target="manage">취소</button>
                    <button type="submit" class="btn primary">등록하기</button>
                </div>
            </form>
        </div>
    </section>

    <section class="admin-page-section" data-admin-section="edit" data-admin-hash="edit" hidden>
        <c:choose>
            <c:when test="${isEdit}">
                <div class="panel">
                    <div class="panel-header">
                        <div class="panel-title">
                            <div class="section-kicker">Edit OTT</div>
                            <h2>OTT 항목 수정</h2>
                            <p><strong><c:out value="${editService.service_name}" /></strong> 정보를 수정하고 있습니다.</p>
                        </div>
                        <button type="button" class="btn ghost" data-admin-section-target="manage">목록으로</button>
                    </div>

                    <form id="adminOttUpdateForm" action="${contextPath}/admin/ott/update.do" method="post">
                        <input type="hidden" name="ott_service_id" value="${editService.ott_service_id}">

                        <div class="form-grid">
                            <div class="form-field"><label>OTT 이름</label><input class="form-input" type="text" name="service_name" value="<c:out value='${editService.service_name}' />" placeholder="예: Netflix" required></div>
                            <div class="form-field"><label>고정 멤버십명</label><input class="form-input" type="text" name="fixed_plan_name" value="<c:out value='${empty editService.fixed_plan_name ? "프리미엄" : editService.fixed_plan_name}' />" placeholder="예: 프리미엄"></div>
                            <div class="form-field"><label>최고 멤버십 가격</label><input class="form-input" type="number" name="base_price" value="${editService.base_price}" min="0" placeholder="예: 17000" required></div>
                            <div class="form-field"><label>최종 기준금액</label><input class="form-input" type="number" name="default_price" value="${editService.default_price}" min="0" placeholder="예: 27000"></div>
                            <div class="form-field"><label>최대 인원</label><input class="form-input" type="number" name="max_member_limit" value="${editService.max_member_limit}" min="1" max="6" required></div>
                            <div class="form-field"><label>추가 멤버 비용</label><input class="form-input" type="number" name="extra_member_fee" value="${editService.extra_member_fee}" min="0"></div>
                            <div class="form-field"><label>추가 멤버 수</label><input class="form-input" type="number" name="extra_member_count" value="${editService.extra_member_count}" min="0"></div>
                            <div class="form-field"><label>플랫폼 수수료율(%)</label><input class="form-input" type="number" step="0.01" name="platform_fee_rate" value="${editService.platform_fee_rate}" min="0"></div>
                            <div class="form-field">
                                <label>공유 가능 여부</label>
                                <select class="form-input" name="share_yn"><option value="Y" ${editService.share_yn eq 'Y' ? 'selected' : ''}>사용중</option><option value="N" ${editService.share_yn eq 'N' ? 'selected' : ''}>숨김</option></select>
                            </div>
                            <div class="form-field">
                                <label>위험도</label>
                                <select class="form-input" name="risk_level"><option value="" ${empty editService.risk_level ? 'selected' : ''}>선택 안 함</option><option value="LOW" ${editService.risk_level eq 'LOW' ? 'selected' : ''}>LOW</option><option value="MEDIUM" ${editService.risk_level eq 'MEDIUM' ? 'selected' : ''}>MEDIUM</option><option value="HIGH" ${editService.risk_level eq 'HIGH' ? 'selected' : ''}>HIGH</option></select>
                            </div>
                        </div>

                        <div class="form-field">
                            <label>차단·주의 사유</label>
                            <textarea class="form-textarea" name="block_reason" placeholder="공유 불가 또는 주의 사유가 있다면 입력하세요."><c:out value="${editService.block_reason}" /></textarea>
                        </div>

                        <div class="toolbar admin-toolbar-end">
                            <button type="button" class="btn ghost" data-admin-section-target="manage">취소</button>
                            <button type="submit" class="btn primary">수정 저장</button>
                        </div>
                    </form>
                </div>
            </c:when>
            <c:otherwise>
                <div class="panel">
                    <div class="panel-header">
                        <div class="panel-title">
                            <div class="section-kicker">Edit OTT</div>
                            <h2>OTT 항목 수정</h2>
                            <p>수정할 OTT 서비스를 먼저 선택해 주세요.</p>
                        </div>
                        <button type="button" class="btn primary" data-admin-section-target="manage">OTT 목록 보기</button>
                    </div>
                    <div class="admin-empty-filter">OTT 목록에서 수정 버튼을 누르면 선택한 서비스의 수정 화면이 이 탭에 표시됩니다.</div>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>
