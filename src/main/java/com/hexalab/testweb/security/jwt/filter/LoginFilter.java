package com.hexalab.testweb.security.jwt.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexalab.testweb.member.model.dto.Member;
import com.hexalab.testweb.member.model.service.MemberService;
import com.hexalab.testweb.security.jwt.filter.input.InputMember;
import com.hexalab.testweb.security.jwt.filter.output.CustomUserDetails;
import com.hexalab.testweb.security.jwt.jpa.entity.RefreshToken;
import com.hexalab.testweb.security.jwt.model.service.RefreshService;
import com.hexalab.testweb.security.jwt.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final MemberService memberService;
    private final RefreshService refreshService;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.access-token.expiration}")
    private long access_expiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refresh_expiration;

    // 생성자를 통한 의존성 주입
    public LoginFilter(MemberService memberService, RefreshService refreshService, AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        this.memberService = memberService;
        this.refreshService = refreshService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // 요청 본문을 읽는 메소드
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 요청 본문에서 사용자의 로그인 데이터를 InputUser 객체로 변환함
            InputMember loginData = new ObjectMapper().readValue(request.getInputStream(), InputMember.class);
            // 사용자이름(아이디, 이메일) 과 비밀번호를 기반으로 AuthenticationToken 을 생성함
            // 이 토큰은 사용자가 제공한 이름(아이디, 이메일)과 비밀번호를 담고있음
            // 이후 인증 과정에서 사용됨
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(loginData.getMemId(), loginData.getMemPw());
            Member member = memberService.selectMember(loginData.getMemId());
            if (member == null) {
                log.info("회원 정보가 없습니다.");
                throw new DisabledException("사용할 수 없는 계정입니다.");
            } else {
                // 회원 정보가 확인되면 인증을 진행함
                // AuthenticationManager 를 사용하여 실제 인증을 수행함
                // 이 과정에서 사용자이름(아이디, 이메일)과 비밀번호가 검증됨
                return authenticationManager.authenticate(authToken);
            }
        } catch (AuthenticationException e) {
            // 요청 본문을 읽는 과정에서 오류가 발생한 경우, AuthenticationServiceException 을 발생시킴
            throw new AuthenticationServiceException("인증 처리중 오류발생", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 로그인 성공시 실행되는 메소드
    // 인증된 사용자 정보를 바탕으로 jwt token 을 생성하고, 이를 응답(response) 헤더에 추가함
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
        // 인증 객체에서 CustomUserDetails 를 추출함
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        // CustomUserDetails 에서 사용자 이름(아이디, 이메일)을 추출함
        String username = customUserDetails.getUsername();

        // 로그인이 성공했을 때 이므로, jwt 토큰을 생성함
        String access = jwtUtil.generateToken(username, "access", access_expiration);
        String refresh = jwtUtil.generateToken(username, "refresh", refresh_expiration);
        log.info("access_token : " + access);
        log.info("refresh_token : " + refresh);

        // 리프레시토큰은 데이터베이스에 저장 처리
        RefreshToken refreshToken = new RefreshToken().builder()
                .id(UUID.randomUUID())
                .status("activated")
                .memberAgent(request.getHeader("User-Agent"))
                .tokenValue(refresh)
                .expiresIn(refresh_expiration)
                .userId(username)
                .build();

        refreshService.save(refreshToken);

        // 응답 객체 헤더에 JWT를 "Authorization", "Bearer " 값 뒤에 엑세스 토큰 추가
        response.addHeader("Authorization", "Bearer " + access);

        // 필요하다면 다시 회원정보 조회해 옴
        Member member = memberService.selectMember(username);

        // 추가로 클라이언트로 보낼 정보(데이터) 처리
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("refresh", refresh);
        responseBody.put("username", username);
        responseBody.put("isAdmin", (member.getMemType().equals("Y")? true : false));
        responseBody.put("nickname", member.getMemName());

        log.info("responseBody : " + responseBody.toString());

        // ObjectMapper 를 사용해서 Map 을 JSON 문자열로 변환함
        String jsonStr = new ObjectMapper().writeValueAsString(responseBody);

        // 응답객체에 응답 컨텐츠 타입을 설정함
        response.setContentType("application/json");

        // 응답객체 바디에 JSON 문자열을 추가해서 로그인 요청한 클라이언트에게 출력스트림으로 내보냄
        PrintWriter out = response.getWriter();
        out.write(jsonStr);
        out.flush();    // 출력스트림 청소
        out.close();        // 출력스트림 닫기
    }

    // 로그인 실패시 실행되는 메소드
    // 실패할 경우 HTTP 상태 코드 401 을 반환함
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {        // import org.springframework.security.core.AuthenticationException;
        // 전달받은 exception 객체로 부터 최종 예외 원인을 찾아냄
        Throwable rootCause = exception.getCause();
        while (rootCause != null && rootCause != exception) {
            rootCause = rootCause.getCause();
        }

        // 전달받은 exception 을 기반으로 오류메세지를 설정함
        String message = null;
        if (exception instanceof UsernameNotFoundException || exception instanceof BadCredentialsException) {
            message = "계정이 존재하지 않거나 잘못된 계정입니다.";
        } else if (exception instanceof DisabledException) {
            message = "계정이 비활성화되었습니다. 현재 로그인 할 수 없습니다. 관리자에게 문의하세요.";
        } else if (exception instanceof LockedException) {
            message = "계정이 잠겨 있습니다. 5회 접속 요청 실패이므로 10분 뒤에 다시 접속하세요.";
        } else {
            // 다른 예외들은 모두
            message = "로그인 인증에 실패했습니다. 관리자에게 문의하세요.";
        }

        // 응답 데이터 준비. 클라이언트 뷰 페이지에 사용할 데이터들로 저장 처리
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", message);
        responseBody.put("status", HttpStatus.BAD_REQUEST.value());
        responseBody.put("path", request.getRequestURI());
        responseBody.put("timestamp", LocalDateTime.now().toString());
        responseBody.put("error", "Unauthorized");

        // 응답 처리
        response.setContentType("application/json; charset=utf-8");
        response.setCharacterEncoding("utf-8");     // 응답 데이터에 한글이 존재할 경우

        try {
            String jsonStr = new ObjectMapper().writeValueAsString(responseBody);
            PrintWriter out = response.getWriter();
            out.write(jsonStr);
            out.flush();
            out.close();
        } catch (IOException ignored) {
            // 예외 발생시 출력 또는 처리내용 없음
        }
    }



}