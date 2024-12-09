package com.hexalab.testweb.security.jwt.filter;

import com.hexalab.testweb.member.jpa.entity.MemberEntity;
import com.hexalab.testweb.security.jwt.filter.output.CustomUserDetails;
import com.hexalab.testweb.security.jwt.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
@RequiredArgsConstructor
// Spring Security 가 제공하는 OncePerRequestFilter 를 상속받음
// OncePerRequestFilter : 모든 요청에 대해 한번씩 실행되는 필터임
public class JWTFilter extends OncePerRequestFilter {
    // JWT 관련 유틸리티 메소드를 제공하는 JWTUtil 인스턴스를 멤버로 선언
    private final JWTUtil jwtUtil;

    // 생성자를 통한 의존성 주입
//    public JWTFilter(JWTUtil jwtUtil) {
//        this.jwtUtil = jwtUtil;
//    }

    // 필터의 주요 로직을 구현하는 메소드임
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 요청 객체 (request) 에서 "Authorization" 헤더를 추출함
        String authorization = request.getHeader("Authorization");

        String requestURI = request.getRequestURI();
        if (requestURI.equals("/reissue")) {
            filterChain.doFilter(request, response);    // 현재 필터를 빠져나감
            return;
        }

        // 'Authorization' 헤더가 없거나 Bearer 뒤에 토큰이 아니면 요청을 계속 진행함
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 로그인 인증 정보(엑세스토큰)가 있다면 토큰을 추출함
//        String token = authorization.substring("Bearer ".length());
        String token = authorization.split(" ")[1];

        // 토큰 만료 여부 확인하고, 만료시에는 다음 필터로 넘기지 않음
        if (jwtUtil.isTokenExpired(token)){
            // response body
            PrintWriter out = response.getWriter();
            out.println("access token expired");
            // response status output
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;     // 서비스 요청한 클라이언트에게 엑세스 토큰 만료시한이 종료되었다고 메세지 보냄
        }

        // token 에서 category 가져오기
        String category = jwtUtil.getCategoryFromToken(token);
        // 토큰 카테고리가 'access' 가 아니라면 만료된 또는 잘못된 토큰으로 판단함
        if (!category.equals("access")) {
            // response body
            PrintWriter out = response.getWriter();
            out.println("invalid access token");
            // response status code output
            // 응답 상태 코드를 401 (SC_UNAUTHORIZED) 이 아닌 다른 코드로 약속하고 넘기면
            // 리프레시 토큰 발급 체크를 한다고 정하면 좀 더 빠르게 진행시킬 수 있음
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 토큰에서 사용자 이름(아이디, 이메일) 과 관리자 여부를 추출함
        String username = jwtUtil.getUserIdFromToken(token);
        String admin = jwtUtil.getAdminFromToken(token);

        // 인증에 사용할 임시 MemberEntity (또는 Member) 객체를 생성하고 사용자이름과 관리자여부를 저장함
        MemberEntity member = new MemberEntity();
        member.setMemId(username);
        member.setMemType(admin.equals("ADMIN")? "Y" : "N");
        // 실제 인증에서는 사용되지 않는 임시 비밀번호 지정함
        member.setMemPw("tempPassword");

        // CustomUserDetails 객체 생성해서 member 를 전달함
        CustomUserDetails customUserDetails = new CustomUserDetails(member);

        // Spring Security 의 Authentication 객체를 생성하고, SecurityContext 에 저장함
        // 이로써 해당 요청에 대한 사용자 인증이 완료됨
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        // customUserDetails.getAuthorities() 인증토큰 받음
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 필터 체인을 계속 진행함 또는 실제 연결할 컨트롤러 메소드로 연결 처리함 (반드시 마지막에 들어가야 하는 코드구문)
        // 필터 진행이 계속되고 모든 필터가 완료가 되어 컨트롤러로 넘어가야 함
        filterChain.doFilter(request, response);
    }



}

