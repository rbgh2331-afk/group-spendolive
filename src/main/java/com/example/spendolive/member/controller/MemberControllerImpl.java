package com.example.spendolive.member.controller;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityReturnValueHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.spendolive.member.domain.MemberAccountVO;
import com.example.spendolive.member.domain.MemberAjaxResponse;
import com.example.spendolive.member.domain.MemberVO;

import com.example.spendolive.member.service.MemberService;
import com.example.spendolive.mypage.service.MyPageService;


@Controller("memberController")
@ControllerAdvice
@RequestMapping(value="/member")
public class MemberControllerImpl implements MemberController{
    @Autowired
    private MemberService memberService;
    @Autowired
    private MyPageService mypageService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Value("${kakao.client.id}")
    private String kakaoclientId;
    @Value("${kakao.redirect.uri}")
    private String kakaoredirectUri;
    @Value("${openbanking.client-id}")
    private String openbankingclientId;
    @Value("${openbanking.redirect-uri}")
    private String openbankingredirectUri;
    @Value("${openbanking.integrated-redirect-uri}")
    private String openbankingIntegratedredirectUri;
    @Value("${openbanking.client-secret}")
    private String openbankingclientSecret;    
    @Override
    
    // 코드리뷰.4
    @RequestMapping(value="/login.do" ,method = RequestMethod.POST )
    public ResponseEntity<MemberAjaxResponse> login(@RequestParam Map<String, String> loginMap, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String id = loginMap.get("id");
        String rawPassword = loginMap.get("password");
        String url ="";
      
        MemberVO memberVO = memberService.getMemberById(id);
        if (memberVO == null){
            return ResponseEntity.ok(new MemberAjaxResponse(
                false,
                "LOGIN_FAILED",
                "아이디가 존재하지 않습니다.",
                "FAIL",
                null,
                url));
        }
        if (passwordEncoder.matches(rawPassword, memberVO.getPassword())) {
            List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + memberVO.getRole()) );
            //스프링 시큐리티용 인증(Authentication) 객체 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                memberVO.getId(), // principal (아이디 또는 MemberVO)
                null,             // credentials (비밀번호는 인증 후 null 처리)
                authorities       // 권한 목록
            );
            //SecurityContext 생성 후 인증 객체 담기
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);      
            HttpSession session = request.getSession();
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
            session.setAttribute("isLogOn", true);  
            session.setAttribute("memberInfo", memberVO);
            List<MemberAccountVO> accountList =memberService.getAccountById(id);
            if(accountList != null){
                for(MemberAccountVO account : accountList){
                    if(account.getOpen_bank_token() != null){
                        memberService.registerOpenBankingIntegratedToken(memberVO,account);
                    }
                }
            }
             
         
                
                String log = (String) session.getAttribute("log");
                if ("mypage".equals(log)) {
                    url = "/spendolive/mypage.do";
                } else if ("expense".equals(log)) {
                    url = "/spendolive/expense.do";
                } else if ("ott".equals(log)) {
                    url = "/spendolive/ott.do";
                } else {
                    url = "/spendolive/main.do";
                }
                if(memberVO.getRole().equals("ADMIN")){url = "/spendolive/admin/main.do";
                return ResponseEntity.ok(new MemberAjaxResponse(
                    true,
                    "ADMINLOGIN_COMPLETED",
                    "로그인에 성공하였습니다.",
                    "SUCCESS",
                    null,
                    url));
                }
    
            return ResponseEntity.ok(new MemberAjaxResponse(
                true,
                "LOGIN_COMPLETED",
                "로그인에 성공하였습니다.",
                "SUCCESS",
                null,
                url));
              
        }else {
            url = "/spendolive/member/loginForm.do";
            return ResponseEntity.ok(new MemberAjaxResponse(
                false,
                "LOGIN_FAILED",
                "비밀번호가 틀립니다",
                "FAIL",
                null,
                url));
        }
    }
       
       
    
    // 코드리뷰.3 ->loginform.jsp
    @Override
    @RequestMapping(value="/loginForm.do" , method = {RequestMethod.POST, RequestMethod.GET})
    public ModelAndView loginForm(@RequestParam(value = "log", required = false) String log, HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        HttpSession session = request.getSession();
        session.setAttribute("log", log); // log = 원래 이동하려던 페이지 정보 
        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize"
                            + "?client_id=" + kakaoclientId 
                            + "&redirect_uri=" +kakaoredirectUri
                            + "&response_type=code";
        ModelAndView mav = new ModelAndView();
        mav.addObject("kakaoAuthUrl", kakaoAuthUrl);
        mav.setViewName("member/loginForm");
        return mav;
    }
    
    @Override
    @RequestMapping(value="/logout.do" ,method = RequestMethod.GET)
    public ModelAndView logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView();
        HttpSession session=request.getSession();
        session.setAttribute("isLogOn", false);
        session.removeAttribute("SPRING_SECURITY_CONTEXT");
        session.removeAttribute("memberInfo");
        session.removeAttribute("login_type");
        session.removeAttribute("loginId");
        mav.setViewName("redirect:/spendolive/main.do");
        return mav;
    }                   
    
    
    // 코드리뷰.2
    @Override
    @RequestMapping(value="/addmember.do" ,method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<MemberAjaxResponse> addMember(@ModelAttribute("memberVO") MemberVO member, HttpServletRequest request, HttpServletResponse response ,RedirectAttributes redirectAttributes)
            throws Exception {
        request.setCharacterEncoding("utf-8");
        HttpSession session = request.getSession();
        session.removeAttribute("id");
        session.removeAttribute("member_name");
        try {
            memberService.addMember(member);
            return ResponseEntity.ok(new MemberAjaxResponse(
                true,
                "SIGNUP_COMPLETED",
                "회원가입에 성공하였습니다.",
                "SUCCESS",
                null,
                "/member/loginForm.do"));
        }catch(Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MemberAjaxResponse(
                            false,
                            "SIGNUP_FAILED",
                            "회원가입에 실패하였습니다. 정보를 확인 해 주세요.",
                            "FAILED",
                            "",
                            "redirect:/member/signup.do"));
        }
        
    }
    
    
    // 코드리뷰.1 -> signup.jsp -> js
    //회원가입 페이지 이동 메서드
    @Override
    @RequestMapping(value="/signup.do" , method = RequestMethod.GET)
    public ModelAndView memberForm(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView();
        

        mav.setViewName("member/signup");
        
    
        
        return mav;
    }
 
 
    // 코드리뷰.2-1
    @Override
    @ResponseBody
    @PostMapping("/sendEmail.do")
    public ResponseEntity<MemberAjaxResponse> sendEmail(@RequestParam("email") String email, HttpServletRequest request) throws Exception {
        
       
            try{
            if(!memberService.checkEmail(email)){
            return ResponseEntity.ok(new MemberAjaxResponse(
                false,
                "EMAIL_EXIST",
                "EMAIL이 이미 존재 합니다.",
                "EXIST",
                email,
                null));}
            }catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MemberAjaxResponse(
                            false,
                            "CHECK_FAILED",
                            "오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                            "FAILED",
                            email,
                            null));
            }
            try {
            // 메일 발송 후 생성된 6자리 코드 반환받기
            String verificationCode = memberService.sendVerificationEmail(email);
            
            // 사용자가 나중에 입력한 값과 비교할 수 있도록 세션에 인증코드 저장
            HttpSession session = request.getSession();
            session.setAttribute("verificationCode", verificationCode);
            
            return ResponseEntity.ok(new MemberAjaxResponse(
                            true,
                            "SEND_COMPLETED",
                            "인증번호 전송 완료되었습니다.",
                            "SUCCESS",
                            email,
                            null));
            
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MemberAjaxResponse(
                                false,
                                "SEND_FAILED",
                                "인증번호 발생 중 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.",
                                "FAILED",
                                email,
                                null));
            }
            
        }
 
 
    // 코드리뷰.2-2
    @Override
    @PostMapping("/verifyEmail.do")
    @ResponseBody
    public boolean verifyEmail(@RequestParam("inputCode") String inputCode, HttpServletRequest request) {
        HttpSession session = request.getSession();
    
        String originalCode = (String) session.getAttribute("verificationCode");
        
        // 사용자가 화면에 입력한 값과 진짜 값이 일치하는지 판별 (true / false 반환)
        if (originalCode != null && originalCode.equals(inputCode)) {
            session.removeAttribute("verificationCode"); // 인증 성공 시 세션 청소
            return true;
        }
        
        return false;
    }
    
    
    // 코드리뷰.2-3
            // 1. 휴대폰 인증번호 발송 요청 처리
        @Override
        @PostMapping("/sendSms.do")
        @ResponseBody
        public  ResponseEntity<MemberAjaxResponse> sendSms(@RequestParam("phone") String phone, HttpServletRequest request) throws Exception {
           
            try {
                try{
                if(memberService.checkPhone(phone)){
                }else return ResponseEntity.ok(new MemberAjaxResponse(
                    false,
                    "PHONE_EXIST",
                    "번호가 이미 존재 합니다.",
                    "EXIST",
                    phone,
                    null));
                }catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MemberAjaxResponse(
                                false,
                                "CHECK_FAILED",
                                "오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                                "FAILED",
                                phone,
                                null));
                }
                // 메일 발송 후 생성된 6자리 코드 반환받기
                String verificationCode = memberService.sendSmsVerification(phone.replace("-", ""));//인증번호 생성
                
                // 사용자가 나중에 입력한 값과 비교할 수 있도록 세션에 인증코드 저장
                HttpSession session = request.getSession();
                session.removeAttribute("smsCode");
                session.setAttribute("smsCode", verificationCode);
                
                return ResponseEntity.ok(new MemberAjaxResponse(
                                true,
                                "SEND_COMPLETED",
                                "인증번호 전송 완료되었습니다.",
                                "SUCCESS",
                                phone,
                                null));
                
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MemberAjaxResponse(
                                false,
                                "SEND_FAILED",
                                "인증번호 발생 중 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.",
                                "FAILED",
                                phone,
                                null));
            }
            
        }
    
    
        // 코드리뷰.2-4
        // 2. 사용자가 입력한 인증번호 검증 처리
        @Override
        @PostMapping("/verifySms.do")
        @ResponseBody
        public boolean verifySms(@RequestParam("inputCode") String inputCode, HttpServletRequest request) {
            HttpSession session = request.getSession();
            
            // 세션에 저장해 둔 진짜 인증번호 꺼내기
            String originalCode = (String) session.getAttribute("smsCode");
            
            // 사용자가 화면에 입력한 값과 진짜 값이 일치하는지 판별 (true / false 반환)
            if (originalCode != null && originalCode.equals(inputCode)) {
                session.removeAttribute("smsCode"); // 인증 성공 시 세션 청소
                return true;
            }
            
            return false;
        }
        
        //아이디 중복확인
        @Override
        @PostMapping("/checkId.do")
        @ResponseBody
        public ResponseEntity<MemberAjaxResponse> checkId(@RequestParam("id") String id) throws Exception {
            
            try {
                    if(memberService.checkId(id)){
                        return ResponseEntity.ok(new MemberAjaxResponse(
                            true,
                            "CHECK_COMPLETED",
                            "중복화인이 완료되었습니다.",
                            "SUCCESS",
                            id,
                            null));
                    }return ResponseEntity.ok(new MemberAjaxResponse(
                            false,
                            "ID_EXIST",
                            "ID가 이미 존재 합니다.",
                            "EXIST",
                            id,
                            null));
            }catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MemberAjaxResponse(
                                false,
                                "CHECK_FAILED",
                                "중복확인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                                "FAILED",
                                id,
                                null));
            }
        }
        @Override
        @PostMapping("/checkEmail")
        @ResponseBody
        public boolean checkEmail(@RequestParam("email") String email) throws Exception {
            return memberService.checkEmail(email);
        }
        @Override
        @RequestMapping(value="/checkPhone", method = RequestMethod.POST)
        @ResponseBody
        public boolean checkPhone(@RequestParam("phone") String phone) throws Exception {
            return memberService.checkPhone(phone);
        }



// 코드리뷰.4-1 -> signup.jsp
    // 카카오 로그인 콜백 (Redirect URI로 설정된 주소)
    @Override
    @RequestMapping(value="/kakaoCallback.do", method = RequestMethod.GET)
    public ModelAndView kakaoCallback(@RequestParam(value = "code", required = false) String code, 
                                      HttpServletRequest request,RedirectAttributes redirectAttributes) {
        ModelAndView mav = new ModelAndView();
                                    
        // 1. 인가 코드 누락(사용자가 취소 버튼을 누른 경우 등) 처리
        if (code == null || code.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("msg", "카카오 로그인이 취소되었거나 오류가 발생했습니다."); 
            return new ModelAndView("member/loginForm");

        }
       
        try {
            // 2. 통합된 MemberService를 통해 카카오 유저 정보 획득
            Map<String, String> userInfo = memberService.getKakaoUserInfo(code);
            String id = userInfo.get("id");
            // 3. 세션 처리
            HttpSession session = request.getSession();
            
            if(memberService.checkId(id)){
                System.out.println(id);
                session.setAttribute("login_type", "KAKAO");
                session.setAttribute("id", id);
                session.setAttribute("member_name", userInfo.get("nickname")); 
                return new ModelAndView("member/signup");
            } else {
                MemberVO memberVO = memberService.getMemberById(id);
                List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + memberVO.getRole()) );
                    //스프링 시큐리티용 인증(Authentication) 객체 생성
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                        memberVO.getId(), // principal (아이디 또는 MemberVO)
                        null,             // credentials (비밀번호는 인증 후 null 처리)
                        authorities       // 권한 목록
                    );
                    //SecurityContext 생성 후 인증 객체 담기
                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                    securityContext.setAuthentication(authentication);      
                    session.setAttribute("login_type", "KAKAO");
                    session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
                    session.setAttribute("isLogOn", true);  
                    session.setAttribute("memberInfo", memberVO);
                    List<MemberAccountVO> accountList =memberService.getAccountById(id);
                    if(accountList != null){
                        for(MemberAccountVO account : accountList){
                            if(account.getOpen_bank_token() != null){
                                memberService.registerOpenBankingIntegratedToken(memberVO,account);
                            }
                        }
                    }
                    String log = (String) session.getAttribute("log");
                    if ("mypage".equals(log)) {
                        mav.setViewName("redirect:/spendolive/mypage.do");
                    } else if ("expense".equals(log)) {
                        mav.setViewName("redirect:/spendolive/expense.do");
                    } else if ("ott".equals(log)) {
                        mav.setViewName("redirect:/spendolive/ott.do");
                    } else {
                        mav.setViewName("redirect:/spendolive/main.do");
                    }
                    session.removeAttribute("log");
                }      
            } catch (Exception e) {
                e.printStackTrace(); 
                redirectAttributes.addFlashAttribute("msg", "카카오 로그인 연동 중 서버 오류가 발생했습니다."); 
                return new ModelAndView("member/loginForm");
            }
            
        return mav;
    }
   
   
    @Override
    @RequestMapping(value = "/openBankingIntegratedAuth.do", method = RequestMethod.GET)
    public String openBankingIntegratedAuth() throws UnsupportedEncodingException {
    
        String state = UUID.randomUUID().toString().replace("-", "");
        String targetUrl = String.format(
        "https://testapi.openbanking.or.kr/oauth/2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=login+accountinfo&state=%s",
        openbankingclientId, openbankingIntegratedredirectUri, state
        );
        return "redirect:" + targetUrl;
    }




    @Override
    @RequestMapping(value="/openBankingAuth.do", method = RequestMethod.GET)
    public String openBankingAuth() throws UnsupportedEncodingException {


        String state = UUID.randomUUID().toString().replace("-", "");
        String encodedRedirectUri = URLEncoder.encode(openbankingredirectUri, StandardCharsets.UTF_8.toString());
        String targetUrl = String.format(
        "https://testapi.openbanking.or.kr/oauth/2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=login+inquiry+transfer&state=%s&auth_type=0",
        openbankingclientId, encodedRedirectUri, state
        );

        // 금결원 페이지로 리다이렉트
        return "redirect:" + targetUrl;
    }
    @Override
    @RequestMapping(value="/openBankingcallback.do", method = RequestMethod.GET)
    public ModelAndView openBankingCallback(
        @RequestParam("code") String code,
        @RequestParam("state") String state,
        HttpServletRequest request, HttpServletResponse response,
        HttpSession session,RedirectAttributes redirectAttributes) throws UnsupportedEncodingException { // 로그인한 회원의 정보를 알기 위해 세션 사용

    // [보안 체크] 내가 보냈던 state 값이 맞는지 검증하는 로직을 넣으면 더 안전합니다.
    
    // 현재 로그인한 사용자의 ID나 고유 번호 가져오기 (세션 등 활용)
    MemberVO memberVO = (MemberVO) session.getAttribute("memberInfo");
    String userId = memberVO.getId();
    ResponseEntity resEntity = null;
    HttpHeaders responseHeaders = new HttpHeaders();
    try {
        // 비즈니스 로직 처리를 위해 서비스 호출
        memberService.registerOpenBankingToken(code, userId, responseHeaders, resEntity,memberVO);

        session.removeAttribute("memberInfo");
        MemberVO newmemberVO = memberService.getMemberById(userId);
        session.setAttribute("memberInfo", newmemberVO);
        redirectAttributes.addFlashAttribute("msg", "계좌인증을 완료했습니다."); 
        return new ModelAndView("redirect:/spendolive/main.do");
        // 연동 성공 후 완료 페이지나 메인 화면으로 이동
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("msg", "계좌 인증에 실패하였습니다. 다시 시도해 주세요."); 
        return new ModelAndView("redirect:/spendolive/main.do");
    }
}
@Override
@RequestMapping(value="/openBankingIntegratedcallback.do", method = RequestMethod.GET)
public ModelAndView openBankingIntegratedcallback(
    @RequestParam("code") String code,
    @RequestParam("state") String state,
    HttpServletRequest request, HttpServletResponse response,
    HttpSession session,RedirectAttributes redirectAttributes) throws UnsupportedEncodingException { // 로그인한 회원의 정보를 알기 위해 세션 사용

// [보안 체크] 내가 보냈던 state 값이 맞는지 검증하는 로직을 넣으면 더 안전합니다.

// 현재 로그인한 사용자의 ID나 고유 번호 가져오기 (세션 등 활용)
//MemberVO memberVO = (MemberVO) session.getAttribute("memberInfo");
//String userId = memberVO.getId();
//ResponseEntity resEntity = null;
//HttpHeaders responseHeaders = new HttpHeaders();
try {
    // 비즈니스 로직 처리를 위해 서비스 호출
    redirectAttributes.addFlashAttribute("msg", "계좌인증을 완료했습니다. 로그인을 다시 해주세요."); 
    return new ModelAndView("redirect:/member/login.do");
    // 연동 성공 후 완료 페이지나 메인 화면으로 이동

    
} catch (Exception e) {
    redirectAttributes.addFlashAttribute("msg", "계좌 인증에 실패하였습니다. 다시 시도해 주세요."); 
    return new ModelAndView("redirect:/spendolive/main.do");
}
}
//강제탈퇴(관리자) 
@Override
@PostMapping("/whitdraw.do")
@ResponseBody
public ResponseEntity<MemberAjaxResponse> whitdraw(@RequestParam("id") String id,  HttpServletRequest request, HttpServletResponse response) throws Exception {
    request.setCharacterEncoding("utf-8");
    try {
            mypageService.withdrawMember(id);
                return ResponseEntity.ok(new MemberAjaxResponse(
                    true,
                    "WITHDRAW_COMPLETED",
                    "탈퇴가 완료되었습니다.",
                    "SUCCESS",
                    id,
                    "/admin/member/list.do"));
    }catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MemberAjaxResponse(
                        false,
                        "WHITDRAW_FAILED",
                        "탈퇴에 실패 하였습니다.",
                        "FAILED",
                        id,
                        "/admin/member/list.do"));
    }
}
    /* =========================================================
       [추가 기능] 아이디 찾기 - 1단계: 휴대폰 인증번호 발송
       ---------------------------------------------------------
       화면 위치: loginForm.jsp > 아이디 찾기 폼 > "인증번호 받기" 버튼
       호출 JS  : sendFindIdSms()
       URL      : POST /member/findId/sendSms.do
       역할     : 입력한 휴대폰 번호로 가입된 ACTIVE 회원이 있는지 확인한 뒤,
                  인증번호를 발급하고 세션에 임시 저장한다.
       세션 저장: findIdSmsCode = 인증번호, findIdPhone = 숫자만 남긴 휴대폰 번호
       주의     : 현재 sendSmsVerification()은 실제 문자 발송 대신 콘솔 출력 방식일 수 있음.
       ========================================================= */
    @RequestMapping(value = "/findId/sendSms.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> sendFindIdSms(@RequestParam("phone") String phone,
                                             HttpServletRequest request) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            String foundId = memberService.findIdByPhone(phone);
            if (foundId == null || foundId.isBlank()) {
                result.put("success", false);
                result.put("message", "해당 휴대폰 번호로 가입된 계정이 없습니다.");
                return result;
            }

            String normalizedPhone = normalizePhone(phone);
            String verificationCode = memberService.sendSmsVerification(normalizedPhone);

            HttpSession session = request.getSession();
            session.setAttribute("findIdSmsCode", verificationCode);
            session.setAttribute("findIdPhone", normalizedPhone);

            result.put("success", true);
            result.put("message", "인증번호를 발송했습니다. 콘솔에 출력된 인증번호를 입력해주세요.");
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "인증번호 발송 중 오류가 발생했습니다.");
            return result;
        }
    }


    

    /* =========================================================
       [추가 기능] 아이디 찾기 - 2단계: 인증번호 확인 후 아이디 반환
       ---------------------------------------------------------
       화면 위치: loginForm.jsp > 아이디 찾기 폼 > "아이디 찾기" 버튼
       호출 JS  : verifyFindIdSms()
       URL      : POST /member/findId/verify.do
       역할     : 사용자가 입력한 인증번호와 세션의 findIdSmsCode를 비교한다.
                  인증 성공 시 휴대폰 번호로 member_tb.id를 조회해서 화면에 알려준다.
       세션 정리: 성공 시 findIdSmsCode, findIdPhone 제거
       ========================================================= */
    @RequestMapping(value = "/findId/verify.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> verifyFindIdSms(@RequestParam("inputCode") String inputCode,
                                               HttpServletRequest request) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            HttpSession session = request.getSession();
            String originalCode = (String) session.getAttribute("findIdSmsCode");
            String phone = (String) session.getAttribute("findIdPhone");

            if (originalCode == null || phone == null || !originalCode.equals(inputCode)) {
                result.put("success", false);
                result.put("message", "인증번호가 일치하지 않습니다.");
                return result;
            }

            String foundId = memberService.findIdByPhone(phone);
            if (foundId == null || foundId.isBlank()) {
                result.put("success", false);
                result.put("message", "가입된 아이디를 찾을 수 없습니다.");
                return result;
            }

            session.removeAttribute("findIdSmsCode");
            session.removeAttribute("findIdPhone");

            result.put("success", true);
            result.put("id", foundId);
            result.put("message", "가입된 아이디는 [ " + foundId + " ] 입니다.");
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "아이디 찾기 중 오류가 발생했습니다.");
            return result;
        }
    }

    /* =========================================================
       [추가 기능] 비밀번호 찾기 - 1단계: 아이디/휴대폰 일치 확인 후 인증번호 발송
       ---------------------------------------------------------
       화면 위치: loginForm.jsp > 비밀번호 찾기 폼 > "인증번호 받기" 버튼
       호출 JS  : sendFindPwSms()
       URL      : POST /member/findPw/sendSms.do
       역할     : 1) 아이디가 ACTIVE 회원인지 확인
                  2) 아이디와 휴대폰 번호가 같은 회원 정보인지 확인
                  3) 맞으면 인증번호를 발급하고 세션에 저장
       세션 저장: findPwSmsCode, findPwId, findPwPhone
       보안 이유: 아이디만 알면 비밀번호를 바꿀 수 없게 휴대폰 번호까지 검증한다.
       ========================================================= */
    @RequestMapping(value = "/findPw/sendSms.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> sendFindPwSms(@RequestParam("id") String id,
                                             @RequestParam("phone") String phone,
                                             HttpServletRequest request) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            if (id == null || id.isBlank()) {
                result.put("success", false);
                result.put("message", "아이디를 입력해주세요.");
                return result;
            }

            if (!memberService.existsActiveId(id)) {
                result.put("success", false);
                result.put("message", "입력한 아이디가 존재하지 않습니다.");
                return result;
            }

            if (!memberService.existsActiveMemberByIdAndPhone(id, phone)) {
                result.put("success", false);
                result.put("message", "아이디와 휴대폰 번호가 일치하지 않습니다.");
                return result;
            }

            String normalizedPhone = normalizePhone(phone);
            String verificationCode = memberService.sendSmsVerification(normalizedPhone);

            HttpSession session = request.getSession();
            session.setAttribute("findPwSmsCode", verificationCode);
            session.setAttribute("findPwId", id);
            session.setAttribute("findPwPhone", normalizedPhone);
            session.removeAttribute("findPwVerifiedId");

            result.put("success", true);
            result.put("message", "인증번호를 발송했습니다. 콘솔에 출력된 인증번호를 입력해주세요.");
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "인증번호 발송 중 오류가 발생했습니다.");
            return result;
        }
    }

    /* =========================================================
       [추가 기능] 비밀번호 찾기 - 2단계: 휴대폰 인증 완료 처리
       ---------------------------------------------------------
       화면 위치: loginForm.jsp > 비밀번호 찾기 폼 > "인증 확인" 버튼
       호출 JS  : verifyFindPwSms()
       URL      : POST /member/findPw/verify.do
       역할     : 인증번호가 맞으면 findPwVerifiedId를 세션에 저장한다.
                  이 값이 있어야 다음 단계인 비밀번호 변경이 가능하다.
       세션 저장: findPwVerifiedId = 인증 완료된 회원 아이디
       세션 정리: findPwSmsCode 제거
       ========================================================= */
    @RequestMapping(value = "/findPw/verify.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> verifyFindPwSms(@RequestParam("inputCode") String inputCode,
                                               HttpServletRequest request) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            HttpSession session = request.getSession();
            String originalCode = (String) session.getAttribute("findPwSmsCode");
            String id = (String) session.getAttribute("findPwId");

            if (originalCode == null || id == null || !originalCode.equals(inputCode)) {
                result.put("success", false);
                result.put("message", "인증번호가 일치하지 않습니다.");
                return result;
            }

            session.setAttribute("findPwVerifiedId", id);
            session.removeAttribute("findPwSmsCode");

            result.put("success", true);
            result.put("message", "휴대폰 인증이 완료되었습니다. 새 비밀번호를 입력해주세요.");
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "인증 확인 중 오류가 발생했습니다.");
            return result;
        }
    }

    /* =========================================================
       [추가 기능] 비밀번호 찾기 - 3단계: 새 비밀번호 변경
       ---------------------------------------------------------
       화면 위치: loginForm.jsp > 비밀번호 찾기 폼 > 새 비밀번호 입력 영역
       호출 JS  : resetPassword()
       URL      : POST /member/findPw/reset.do
       역할     : 휴대폰 인증이 완료된 회원(findPwVerifiedId)에 한해서
                  새 비밀번호와 비밀번호 확인값을 비교한 뒤 DB 비밀번호를 변경한다.
       세션 조건: findPwVerifiedId가 없으면 "휴대폰 인증 먼저" 메시지 반환
       세션 정리: 성공 시 findPwVerifiedId, findPwId, findPwPhone 제거
       ========================================================= */
    @RequestMapping(value = "/findPw/reset.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> resetPassword(@RequestParam("newPassword") String newPassword,
                                             @RequestParam("newPasswordConfirm") String newPasswordConfirm,
                                             HttpServletRequest request) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            HttpSession session = request.getSession();
            String verifiedId = (String) session.getAttribute("findPwVerifiedId");

            if (verifiedId == null || verifiedId.isBlank()) {
                result.put("success", false);
                result.put("message", "휴대폰 인증을 먼저 완료해주세요.");
                return result;
            }

            if (newPassword == null || newPassword.isBlank() || newPasswordConfirm == null || newPasswordConfirm.isBlank()) {
                result.put("success", false);
                result.put("message", "새 비밀번호와 비밀번호 확인을 모두 입력해주세요.");
                return result;
            }

            if (!newPassword.equals(newPasswordConfirm)) {
                result.put("success", false);
                result.put("message", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
                return result;
            }

            if (newPassword.length() < 4) {
                result.put("success", false);
                result.put("message", "비밀번호는 최소 4자 이상 입력해주세요.");
                return result;
            }

            memberService.updatePasswordById(verifiedId, newPassword);

            session.removeAttribute("findPwVerifiedId");
            session.removeAttribute("findPwId");
            session.removeAttribute("findPwPhone");

            result.put("success", true);
            result.put("message", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.");
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "비밀번호 변경 중 오류가 발생했습니다.");
            return result;
        }
    }

    /* =========================================================
       [추가 유틸] 휴대폰 번호 정규화
       ---------------------------------------------------------
       화면에서는 010-1234-5678 또는 01012345678 둘 다 입력될 수 있으므로
       DB 조회 전 숫자만 남겨 같은 형식으로 비교한다.
       예: 010-1234-5678 -> 01012345678
       ========================================================= */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }
    private ModelAndView layout(String bodyPage) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("common/layout");
        mav.addObject("body_page", bodyPage);
        return mav;
    }

}
