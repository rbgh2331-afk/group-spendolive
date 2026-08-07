<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>
        SpendOlive | 회원가입
    </title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
     <script>
        window.contextPath = "${contextPath}";
    </script>
    <link rel="stylesheet" href="${contextPath}/resources/css/styles.css">
</head>
<body class="auth-body">
    <div class="auth-wrap">
        <aside class="auth-brand-panel">
        <a href="${contextPath}/spendolive/main.do" class="auth-logo" onclick-"login()">
            <img src="${contextPath}/resources/images/logo.png" alt="SpendOlive" style="width:42px;height:42px;border-radius:50%;object-fit:cover;">
            <span>
                SpendOlive
            </span>
        </a>
        <div class="auth-brand-copy">
            <p class="auth-label">
                SIGN UP
            </p>
            <h1>
                안전하게 인증하고
                <br>
                회원가입하세요
            </h1>
            <p class="auth-desc">
                아이디 중복확인, 이메일 인증, 전화번호 인증을 통해 SpendOlive 계정을 생성할 수 있습니다.
            </p>
        </div>
        <div class="auth-brand-stats">
            <div>
                <strong>
                ID
            </strong>
            <span>
                중복확인
            </span>
        </div>
        <div>
            <strong>
            Email
        </strong>
        <span>
            이메일 인증
        </span>
    </div>
    <div>
        <strong>
        Phone
    </strong>
    <span>
        전화번호 인증
    </span>
</div>
</div>
</aside>
<main class="auth-form-panel">
    <section class="auth-card auth-card-wide">
        <div class="auth-card-header">
            <p class="eyebrow">
                CREATE ACCOUNT
            </p>
            <h2>
                회원가입
            </h2>
            <p>
                아이디 또는 이메일 형식으로 가입할 수 있습니다.
            </p>
        </div>

        <c:choose>
            <c:when test="${login_type == 'KAKAO'}"> 
            <form action="${contextPath}/member/addmember.do" method="post" onsubmit="return joinCheckKakao()">
            <div class="auth-grid-2">
                <div class="auth-form-group">
                    <label for="name">
                        이름
                    </label>
                    <input id="name" name="member_name" type="readonly" value="${member_name}" required>
                </div>
                <div class="auth-form-group">
                    <label for="nickname">
                        닉네임
                    </label>
                    <input id="nickname"name="nickname" type="text" placeholder="닉네임을 입력하세요" required>
                </div>
            </div>
               <div class="auth-form-group">
                <label for="email">
                    이메일
                </label>
                <div class="auth-input-row">
                    <input id="email" name="email"type="email" placeholder="example@email.com" required>
                    <button id="emailButton" class="auth-btn auth-btn-light" type="button">인증요청</button>
                </div>
             

            <div class="auth-form-group" id="emailAuthArea" style="display:none;">
                <label for="emailAuthCode">인증번호 입력</label>
                <div class="auth-input-row">
                    <input id="emailAuthCode" type="text" placeholder="6자리 인증번호를 입력하세요" >
                    <button class="auth-btn auth-btn-light" type="button" onclick="verifyEmail()">인증확인</button>
                </div>
               
                <p id="emailAuthResult" class="auth-result-text">이메일로 발송된 인증번호를 입력해 주세요.</p>
            </div>
        
            <div class="auth-form-group">
                <label for="phone">전화번호</label>
                <div class="auth-input-row">
                    <input id="phone" name="phone" type="tel" placeholder="010-0000-0000" required>
                    <button id="phoneButton" class="auth-btn auth-btn-light" type="button">인증요청</button>
                </div>
                

            <div class="auth-form-group" id="phoneAuthArea" style="display:none;">
                <label for="phoneAuthCode">전화번호 인증번호 입력</label>
                <div class="auth-input-row">
                    <input id="phoneAuthCode" type="text" placeholder="6자리 인증번호를 입력하세요">
                    <button class="auth-btn auth-btn-light" type="button" onclick="verifySms()">인증확인</button>
                </div>
                <p id="phoneAuthResult" class="auth-result-text">휴대폰으로 발송된 인증번호를 입력해 주세요.</p>
            </div>
            <input id="userId" type="hidden" name="id" value="${id}">
            <input id="password" type="hidden" name="password" value="KAKAO">
            <input id="login_type" type="hidden" name="login_type" value="${login_type}">
            </c:when>
        <c:otherwise>
         
        
        <form action="${contextPath}/member/addmember.do" method="post" onsubmit="return joinCheck()">
        <input type="hidden" name="login_type" value="LOCAL">
            <div class="auth-grid-2">
                <div class="auth-form-group">
                    <label for="name">
                        이름
                    </label>
                    <input id="name" name="member_name" type="text" placeholder="이름을 입력하세요" required>
                </div>
                <div class="auth-form-group">
                    <label for="nickname">
                        닉네임
                    </label>
                    <input id="nickname"name="nickname" type="text" placeholder="닉네임을 입력하세요" required>
                </div>
            </div>
            <div class="auth-form-group">
                <label for="userId">
                    아이디
                </label>
              <div class="auth-input-row">
                    <input id="userId" type="text" name="id" placeholder="사용할 아이디를 입력하세요" required>
                    <button id="checkIdButton" class="auth-btn auth-btn-light" type="button">중복확인</button>
                </div>
      
            <div class="auth-form-group">
                <label for="email">
                    이메일
                </label>
                <div class="auth-input-row">
                    <input id="email" name="email"type="email" placeholder="example@email.com" required>
                    <button id="emailButton" class="auth-btn auth-btn-light" type="button">인증요청</button>
                </div>
                
            
            <div class="auth-form-group" id="emailAuthArea" style="display:none;">
                <label for="emailAuthCode">인증번호 입력</label>
                <div class="auth-input-row">
                    <input id="emailAuthCode" type="text" placeholder="6자리 인증번호를 입력하세요" >
                    <button class="auth-btn auth-btn-light" type="button" onclick="verifyEmail()">인증확인</button>
                </div>
               
                <p id="emailAuthResult" class="auth-result-text">이메일로 발송된 인증번호를 입력해 주세요.</p>
            </div>
                <div class="auth-form-group">
                <label for="phone">전화번호</label>
                <div class="auth-input-row">
                    <input id="phone" name="phone" type="tel" placeholder="010-0000-0000" required>
                    <button id="phoneButton" class="auth-btn auth-btn-light" type="button">인증요청</button>
                </div>
                
            <div class="auth-form-group" id="phoneAuthArea" style="display:none;">
                <label for="phoneAuthCode">전화번호 인증번호 입력</label>
                <div class="auth-input-row">
                    <input id="phoneAuthCode" type="text" placeholder="6자리 인증번호를 입력하세요">
                    <button class="auth-btn auth-btn-light" type="button" onclick="verifySms()">인증확인</button>
                </div>
                <p id="phoneAuthResult" class="auth-result-text">휴대폰으로 발송된 인증번호를 입력해 주세요.</p>
            </div>
            <div class="auth-grid-2">
                <div class="auth-form-group">
                    <label for="password">
                        비밀번호
                    </label>
                    <input id="password" type="password"name="password" placeholder="비밀번호를 입력하세요" required>
                </div>
                <div class="auth-form-group">  
                    <label for="passwordCheck">
                        비밀번호 확인
                    </label>
                    <input id="passwordCheck" type="password" placeholder="비밀번호를 다시 입력하세요" required>
                
                </div>
                </div>
                <div class="auth-form-group">
                <label class="auth-check-row">
                    <input type="checkbox" required>
                    서비스 이용약관 동의
        <textarea style="margin-top: 12px; width: 100%; height: 400px; resize: none; box-sizing: border-box;"  readonly>제1장  서비스 이용약관
제1조 (목적)
본 약관은 SpendOlive(이하 '서비스')가 제공하는 지출 관리 및 OTT 구독 관리 서비스의 이용과 관련하여 서비스와 이용자 간의 권리, 의무 및 책임 사항, 기타 필요한 사항을 규정함을 목적으로 합니다.
제2조 (용어의 정의)
본 약관에서 사용하는 용어의 정의는 다음과 같습니다.
•	"서비스"란 SpendOlive가 제공하는 지출 관리, OTT 구독 관리 및 이와 관련된 제반 서비스를 의미합니다.
•	"이용자"란 본 약관에 동의하고 서비스에 가입하여 서비스를 이용하는 자를 말합니다.
•	"회원"이란 서비스에 가입하여 아이디(ID)와 비밀번호를 부여받은 이용자를 의미합니다.
•	"아이디(ID)"란 회원의 식별 및 서비스 이용을 위하여 회원이 설정하고 서비스가 승인한 이메일 주소를 말합니다.
•	"비밀번호"란 회원의 본인 확인 및 정보 보호를 위해 회원이 설정한 문자, 숫자 등의 조합을 말합니다.
•	"구독방장"이란 OTT 공동 구독 그룹을 개설하고 실제 요금을 납부하는 대표 회원을 의미합니다.
•	"구독참여자"란 구독방장이 개설한 공동 구독 그룹에 참여하여 서비스를 이용하는 회원을 의미합니다.
제3조 (약관의 효력 및 변경)
① 본 약관은 서비스를 이용하고자 하는 모든 이용자에게 적용됩니다.
② 서비스는 필요한 경우 관련 법령을 위반하지 않는 범위 내에서 본 약관을 변경할 수 있습니다.
③ 약관이 변경될 경우 서비스는 변경 사유 및 변경 내용을 시행일로부터 최소 7일 전에 서비스 공지사항을 통해 공지합니다. 다만, 이용자에게 불리한 변경의 경우 30일 전에 공지합니다.
④ 이용자가 변경된 약관에 동의하지 않을 경우 서비스 이용을 중단하고 탈퇴할 수 있습니다. 변경 약관 시행일 이후에도 계속 서비스를 이용하면 변경 약관에 동의한 것으로 간주합니다.
제4조 (서비스의 제공 및 변경)
① 서비스는 다음 각 호의 서비스를 제공합니다.
•	개인 지출 내역 입력, 조회, 분석 및 통계 서비스
•	OTT 구독 정보 등록 및 구독 현황 관리 서비스
•	OTT 공동 구독 그룹 개설 및 참여 서비스
•	구독료 정산 및 수수료 처리 서비스
•	공지사항, 알림 및 캘린더 서비스
•	기타 서비스가 자체적으로 개발하거나 다른 회사와의 협력을 통해 제공하는 서비스
② 서비스는 서비스 품질 개선, 기술적 필요 또는 운영상의 사유로 제공하는 서비스의 내용을 변경할 수 있으며, 이 경우 변경 내용과 적용 일자를 공지합니다.
③ 서비스는 무료로 제공되는 서비스의 일부 또는 전부를 변경하거나 유료로 전환할 수 있으며, 이 경우 최소 30일 전에 사전 공지합니다.
제5조 (서비스 이용계약의 성립)
① 이용계약은 이용자가 약관에 동의한 후 가입 신청을 하고, 서비스가 이를 승낙함으로써 성립합니다.
② 서비스는 다음 각 호에 해당하는 경우 가입 신청을 거절하거나 추후 이용계약을 해지할 수 있습니다.
•	타인의 정보를 도용하거나 허위 정보를 기재하여 신청한 경우
•	만 14세 미만인 자가 신청한 경우
•	이전에 서비스 이용약관 위반으로 이용이 제한된 이력이 있는 경우
•	기타 서비스의 기술상 또는 운영상 지장이 있다고 판단되는 경우
제6조 (회원의 아이디 및 비밀번호 관리)
① 회원은 자신의 아이디 및 비밀번호를 타인에게 공개하거나 양도, 대여할 수 없으며, 이에 대한 관리 책임은 전적으로 회원 본인에게 있습니다.
② 회원은 자신의 아이디 및 비밀번호가 도용되거나 제3자가 사용하고 있음을 인지한 경우 즉시 서비스에 통보하고 서비스의 안내에 따라야 합니다.
③ 서비스는 회원이 본 조 제2항에 따라 통보하지 않거나, 서비스의 안내에 따르지 않아 발생한 불이익에 대하여 책임을 지지 않습니다.
제7조 (이용자의 의무)
이용자는 다음 각 호의 행위를 하여서는 안 됩니다.
•	가입 신청 또는 회원정보 변경 시 허위 내용을 기재하는 행위
•	서비스에 게시된 정보를 허가 없이 변경하거나 삭제하는 행위
•	서비스가 제공하지 않는 방법으로 서비스를 이용하거나 서비스의 운영을 방해하는 행위
•	다른 회원의 개인정보를 허가 없이 수집, 저장, 공개하는 행위
•	서비스를 이용하여 법령 또는 공서양속에 반하는 행위
•	기타 서비스의 정상적인 운영을 방해하는 일체의 행위
제8조 (OTT 공동 구독 수수료)
① 서비스는 OTT 공동 구독 기능을 통해 구독참여자가 구독방장에게 구독료를 정산할 때 중개 서비스를 제공합니다.
② 서비스는 구독참여자가 납부하는 구독 분담금에 대해 아래와 같이 서비스 이용 수수료를 부과합니다.
수수료율: 구독참여자 납부 금액의 3% (부가세 별도)
③ 수수료는 정산 시점에 자동으로 공제되며, 구독방장에게는 수수료를 제외한 금액이 전달됩니다.
④ 수수료율은 서비스의 정책에 따라 변경될 수 있으며, 변경 시 최소 30일 전에 공지합니다.
⑤ 구독방장은 수수료 부과 대상이 아니며, 구독참여자에게만 적용됩니다.
⑥ 구독참여자는 서비스 가입 전 수수료 정책을 충분히 확인하고 동의한 것으로 간주합니다.
제9조 (서비스의 중단)
① 서비스는 다음 각 호에 해당하는 경우 서비스 제공을 일시적으로 중단할 수 있습니다.
•	컴퓨터 등 정보통신 설비의 보수, 점검, 교체 및 고장, 통신 두절 등의 경우
•	서비스의 유지보수를 위한 정기 점검 또는 긴급 점검이 필요한 경우
•	천재지변, 국가 비상사태, 정전 등 불가항력적인 사유가 발생한 경우
② 서비스는 제1항에 의한 중단의 경우 공지사항을 통해 사전 공지합니다. 다만, 불가항력적인 사유로 사전 공지가 불가능한 경우 사후 공지할 수 있습니다.
제10조 (서비스 이용계약의 해지)
① 회원은 언제든지 서비스 내 '회원 탈퇴' 기능을 통하여 이용계약 해지(탈퇴)를 신청할 수 있습니다.
② 서비스는 회원이 다음 각 호에 해당하는 경우 사전 통보 후 이용계약을 해지할 수 있습니다.
•	타인의 정보를 도용하여 가입한 사실이 확인된 경우
•	서비스의 운영을 고의로 방해하거나 타 회원에게 피해를 준 경우
•	본 약관 및 관련 법령을 위반한 경우
③ 탈퇴 또는 해지 시 회원의 지출 내역, OTT 구독 정보 등 개인 데이터는 즉시 삭제됩니다. 단, 관련 법령에 따라 보존이 필요한 정보는 해당 기간 동안 보관합니다.
④ OTT 공동 구독의 구독방장이 탈퇴할 경우 해당 공동 구독 그룹이 자동 해체되며, 참여 중인 구독참여자에게 즉시 알림이 발송됩니다.
제11조 (면책조항)
① 서비스는 이용자가 직접 입력한 지출 내역 및 OTT 구독 정보의 정확성에 대해 책임을 지지 않습니다.
② 서비스에서 제공하는 OTT 구독 요금 정보는 참고용이며, 실제 OTT 사업자의 청구 금액과 다를 수 있습니다. 이로 인해 발생하는 손해에 대해 서비스는 책임을 지지 않습니다.
③ 서비스는 이용자 간의 분쟁에 대해 개입하지 않으며, 이로 인한 손해에 대해 책임을 지지 않습니다.
④ 서비스는 무료로 제공되는 기능에 대하여 법령에서 달리 규정하지 않는 한 손해배상 책임을 지지 않습니다.
제12조 (지식재산권)
① 서비스가 제공하는 모든 콘텐츠(로고, UI 디자인, 텍스트, 이미지, 아이콘 등)에 대한 저작권 및 지식재산권은 서비스에 귀속됩니다.
② 이용자는 서비스를 이용함으로써 얻은 정보를 서비스의 사전 승낙 없이 복제, 전송, 출판, 배포, 방송 기타 방법에 의하여 영리 목적으로 이용하거나 제3자에게 이용하게 하여서는 안 됩니다.
③ 이용자가 서비스 내에 게시한 데이터에 대한 저작권은 해당 이용자에게 있으며, 서비스는 서비스 운영 목적 범위 내에서만 이를 활용합니다.
제13조 (게시물 및 데이터 관리)
① 이용자가 서비스에 입력한 지출 내역, OTT 구독 정보 등 모든 데이터의 권리는 해당 이용자에게 귀속됩니다.
② 서비스는 이용자의 데이터를 서비스 제공 목적 이외의 용도로 사용하지 않습니다.
③ 서비스는 다음 각 호에 해당하는 게시물 또는 데이터를 사전 통보 없이 삭제하거나 이동할 수 있습니다.
•	다른 이용자 또는 제3자를 비방하거나 명예를 훼손하는 내용
•	음란물, 폭력적 내용 등 공서양속에 반하는 내용
•	서비스의 운영 정책에 위반되는 내용
•	기타 관련 법령에 위반되는 내용
제14조 (이용 제한)
① 서비스는 이용자가 본 약관의 의무를 위반하거나 서비스의 정상적인 운영을 방해한 경우, 다음 각 호와 같이 단계적으로 이용을 제한할 수 있습니다.
•	1단계 경고: 위반 사실을 통보하고 시정을 요청합니다.
•	2단계 일시정지: 일정 기간(7일 이내) 서비스 이용을 제한합니다.
•	3단계 영구정지: 반복적인 위반 또는 중대한 위반의 경우 이용계약을 해지합니다. 영구정지 처리된 이용자는 동일한 이메일 주소 또는 휴대폰 번호로 재가입할 수 없으며, 서비스가 동일인임을 확인한 경우 재가입을 영구적으로 제한합니다.
② 서비스는 이용 제한 시 이메일 또는 서비스 내 알림을 통해 해당 이용자에게 사전 통보합니다. 단, 긴급한 경우 사후 통보할 수 있습니다.
③ 이용자는 이용 제한에 대해 이의가 있는 경우 서비스에 이의 신청을 할 수 있으며, 서비스는 이를 검토하여 결과를 통보합니다.
제15조 (광고 및 제휴 서비스)
① 서비스는 서비스 운영과 관련하여 서비스 화면에 광고를 게재할 수 있습니다.
② 서비스는 제3자가 제공하는 외부 링크 또는 제휴 서비스로 연결되는 링크를 제공할 수 있습니다. 이 경우 해당 외부 사이트의 내용 및 신뢰성에 대해 서비스는 책임을 지지 않습니다.
③ 이용자와 광고주 또는 제휴 서비스 사업자 간에 발생한 거래나 분쟁은 이용자와 해당 사업자 간에 해결하여야 하며, 서비스는 이에 대해 책임을 지지 않습니다.
제16조 (준거법 및 관할법원)
① 본 약관의 해석 및 서비스와 이용자 간의 분쟁에 대해서는 대한민국 법률을 적용합니다.
② 서비스 이용과 관련하여 발생한 분쟁에 대한 소송은 민사소송법상의 관할법원에 제소합니다.

부칙: 본 약관은 2025년 6월 1일부터 시행합니다.
</textarea>
</label>
<label class="auth-check-row">
<input type="checkbox" required>
개인정보 처리방침 동의
</label>
<textarea style="margin-top: 12px; width: 100%; height: 400px; resize: none; box-sizing: border-box;"  readonly>제2장  개인정보처리방침
SpendOlive(이하 '서비스')는 개인정보 보호법, 정보통신망 이용촉진 및 정보보호 등에 관한 법률 등 관련 법령에 따라 이용자의 개인정보를 처리하며, 이를 아래와 같이 공개합니다.
제1조 (개인정보의 수집 항목 및 수집 방법)
① 서비스는 다음과 같은 개인정보를 수집합니다.
•	필수 항목: 이메일 주소, 비밀번호(암호화 저장), 닉네임, 휴대폰 번호
•	선택 항목: 프로필 이미지, 계좌번호(OTT 공동 구독 정산 기능 이용 시)
•	서비스 이용 과정에서 자동 수집: 접속 IP 주소, 브라우저 종류 및 버전, 서비스 이용 기록, 접속 일시, 불량 이용 기록
② 개인정보는 다음과 같은 방법으로 수집합니다.
•	회원 가입 및 서비스 이용 과정에서 이용자의 직접 입력을 통한 수집
•	서비스 이용 과정에서 생성 정보 수집 툴을 통한 자동 수집
제1-1조 (금융정보 수집 및 이용 동의)
① 서비스는 OTT 공동 구독 정산 기능 제공을 위해 이용자의 금융정보를 별도로 수집합니다.
•	수집 항목: 계좌번호, 은행명
•	수집 목적: OTT 공동 구독 정산 처리
•	보유 기간: 회원 탈퇴 시 또는 금융정보 삭제 요청 시까지
② 금융정보 수집은 선택 사항이며, 동의하지 않아도 정산 기능을 제외한 서비스 이용이 가능합니다.
③ 이용자는 언제든지 서비스 내 설정 메뉴에서 금융정보 삭제를 요청할 수 있습니다.
금융정보는 개인정보 중 민감도가 높은 항목으로, 별도 동의를 통해 수집되며 정산 목적 외 일체의 용도로 사용되지 않습니다.
제2조 (개인정보의 처리 목적)
서비스는 수집한 개인정보를 다음 목적으로 처리합니다.
•	회원 가입 및 관리: 회원 식별, 본인 확인, 회원 자격 유지 및 관리, 서비스 부정 이용 방지
•	서비스 제공: 지출 내역 저장 및 분석, OTT 구독 현황 관리, 공동 구독 그룹 운영, 수수료 정산
•	고객 지원: 민원 및 문의 접수, 처리 및 답변
•	공지 및 알림: 서비스 이용에 필요한 공지사항, 이벤트 정보 및 각종 알림 발송
•	서비스 개선: 접속 빈도 파악, 서비스 이용 통계 분석 및 서비스 품질 개선
제3조 (개인정보의 보유 및 이용 기간)
① 서비스는 이용자의 개인정보를 회원 탈퇴 시 또는 처리 목적이 달성된 때까지 보유 및 이용합니다.
② 다음 각 호의 경우에는 해당 기간 동안 개인정보를 보관합니다.
•	전자상거래 등에서의 소비자 보호에 관한 법률에 따른 보관
-	계약 또는 청약 철회에 관한 기록: 5년
-	대금 결제 및 재화 등의 공급에 관한 기록: 5년
-	소비자 불만 또는 분쟁 처리에 관한 기록: 3년
•	통신비밀보호법에 따른 통신 사실 확인 자료: 3개월
•	서비스 부정 이용 방지를 위한 불량 이용 기록: 1년
제4조 (개인정보의 제3자 제공)
① 서비스는 이용자의 개인정보를 원칙적으로 외부에 제공하지 않습니다.
② 다음 각 호의 경우에는 예외로 합니다.
•	이용자가 사전에 동의한 경우
•	법령의 규정에 의거하거나, 수사 목적으로 법령에 정해진 절차와 방법에 따라 수사기관의 요청이 있는 경우
제5조 (개인정보의 파기)
① 서비스는 개인정보 보유 기간의 경과, 처리 목적 달성 등 개인정보가 불필요하게 된 경우 지체 없이 해당 개인정보를 파기합니다.
② 전자적 파일 형태의 정보는 복구 및 재생이 되지 않도록 기술적 방법을 사용하여 완전히 삭제합니다.
③ 종이에 출력된 개인정보는 분쇄기로 분쇄하거나 소각하는 방식으로 파기합니다.
제6조 (이용자 및 법정대리인의 권리)
① 이용자는 서비스에 대해 언제든지 다음 각 호의 개인정보 보호 관련 권리를 행사할 수 있습니다.
•	개인정보 열람 요구
•	오류 등이 있을 경우 정정 요구
•	삭제 요구
•	처리 정지 요구
② 제1항에 따른 권리 행사는 서비스에 대해 서면, 전화, 이메일 등을 통하여 할 수 있으며 서비스는 이에 대해 지체 없이 조치하겠습니다.
③ 이용자가 개인정보의 오류에 대한 정정을 요청한 경우, 서비스는 정정을 완료하기 전까지 해당 개인정보를 이용하지 않습니다.
제7조 (개인정보의 안전성 확보 조치)
서비스는 개인정보의 안전성 확보를 위해 다음과 같은 조치를 취하고 있습니다.
•	비밀번호 암호화: 이용자의 비밀번호는 단방향 암호화(해시)하여 저장 및 관리됩니다.
•	해킹 등에 대비한 기술적 대책: 해킹이나 컴퓨터 바이러스 등에 의한 개인정보 유출 및 훼손을 막기 위하여 보안 프로그램을 설치하고 갱신합니다.
•	개인정보에 대한 접근 제한: 개인정보를 처리하는 데이터베이스 시스템에 대한 접근 권한을 최소화하여 관리합니다.
제8조 (개인정보 보호 책임자)
서비스는 개인정보 처리에 관한 업무를 총괄하고, 개인정보 처리와 관련한 이용자의 불만 처리 및 피해 구제를 위하여 아래와 같이 개인정보 보호 책임자를 지정하고 있습니다.
개인정보 보호 책임자: SpendOlive 운영팀 이메일: privacy@spendolive.com
이용자는 서비스를 이용하면서 발생하는 모든 개인정보 보호 관련 민원을 개인정보 보호 책임자에게 신고할 수 있습니다. 서비스는 이용자의 신고 사항에 대해 신속하게 답변 및 처리해드릴 것입니다.
제9조 (쿠키의 사용)
① 서비스는 이용자에게 개인화된 서비스를 제공하기 위해 쿠키(Cookie)를 사용합니다. 쿠키란 웹사이트 서버가 이용자의 브라우저에 전송하는 소량의 텍스트 파일로, 이용자의 기기에 저장됩니다.
② 쿠키의 사용 목적은 다음과 같습니다.
•	로그인 상태 유지 및 세션 관리
•	이용자의 서비스 이용 환경 설정 저장
•	보안 접속 및 부정 이용 방지
③ 이용자는 웹 브라우저의 설정을 통해 쿠키 저장을 거부할 수 있습니다. 단, 쿠키 저장을 거부할 경우 로그인이 필요한 서비스 이용에 어려움이 발생할 수 있습니다.
④ 브라우저별 쿠키 설정 방법은 다음과 같습니다.
•	Chrome: 설정 → 개인정보 및 보안 → 쿠키 및 기타 사이트 데이터
•	Edge: 설정 → 쿠키 및 사이트 권한
•	Firefox: 설정 → 개인 정보 및 보안 → 쿠키와 사이트 데이터
제10조 (개인정보 처리 위탁)
① 서비스는 원활한 서비스 제공을 위해 개인정보 처리 업무의 일부를 외부 업체에 위탁할 수 있습니다.
② 서비스는 위탁 계약 시 다음 사항을 준수합니다.
•	개인정보 보호 관련 법령에 따른 사항을 위탁 계약서에 명시
•	수탁업체의 개인정보 처리 현황에 대한 정기적 감독 실시
•	수탁업체가 개인정보를 안전하게 처리하도록 필요한 사항 지도 및 관리
제11조 (개인정보처리방침의 변경)
① 본 개인정보처리방침은 시행일로부터 적용됩니다.
② 법령이나 서비스 정책에 따른 변경 내용의 추가, 삭제 및 정정이 있는 경우에는 변경 사항의 시행 최소 7일 전부터 공지사항을 통해 고지할 것입니다.

부칙: 본 개인정보처리방침은 2025년 6월 1일부터 시행합니다.

                      </textarea>
            </div>
          
            
           </c:otherwise>
        </c:choose>
            <br>
            <button id="signupButton"class="auth-btn auth-btn-primary" type="submit">
                회원가입
            </button>
        </form>
        <div class="auth-link-row">
            <span>
                이미 계정이 있으신가요?
            </span>
            <a href="${contextPath}/member/loginForm.do">
                로그인
            </a>
        </div>
             <div id="signupStatusOverlay"
                    class="status-overlay"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="emailStatusTitle"
                    aria-describedby="emailStatusMessage"
                    hidden>
                    <div class="status-box">
                    
                    <div id="signupStatusSpinner"
                            class="status-spinner"
                            aria-hidden="true"></div>

                        <div id="signupStatusIcon"
                            class="status-icon"
                            aria-hidden="true"
                            hidden></div>
                            <h3 id="signupStatusTitle">회원가입중 입니다.</h3>
                        <p id="signupStatusMessage">
                            창을 닫거나 새로고침하지 말아주세요.
                        </p>
                    <div id="signupStatusActions"
                            class="status-actions"
                            hidden>
                            <button type="button"
                                    id="signupStatusCloseButton"
                                    class="btn btn-outline">
                                확인
                            </button>
                            <button type="button"
                                    id="signupStatusActionButton"
                                    class="btn btn-primary"
                                    hidden>
                                이동하기
                            </button>
                        </div>
                </div>
            </div>
    </section>

</main>
</div>
<script src="${contextPath}/resources/js/app.js"></script>
<script src="${contextPath}/resources/js/signup.js"></script>
</body>
<jsp:include page="/WEB-INF/views/member/popup.jsp" />
</html>
<script>
var msg = "${msg}";
    var message = "${message}";
    if (msg && msg !== "") {
        alert(msg);
    }
    if (message && message !== "") {
        alert(message);
    }
document.addEventListener('keydown', function(event) {
    if (event.key === 'Enter') {
        // 현재 엔터를 친 요소가 input 태그인지 확인 
        // (textarea나 button에서 엔터를 칠 때는 정상 작동하게 두기 위함)
        if (event.target.tagName === 'INPUT') {
            event.preventDefault(); // 기본 동작(submit) 막기
        }
    }
});
</script>