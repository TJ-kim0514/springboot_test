package com.hexalab.testweb.security.handler;

import com.hexalab.testweb.member.model.dto.Member;
import com.hexalab.testweb.member.model.service.MemberService;
import com.hexalab.testweb.security.jwt.jpa.entity.RefreshToken;
import com.hexalab.testweb.security.jwt.model.service.RefreshService;
import com.hexalab.testweb.security.jwt.util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import java.util.Optional;

@RequiredArgsConstructor        // 자동 의존성 주입
public class CustomLogoutHandler implements LogoutHandler {
    private final JWTUtil jwtUtil;
    private final MemberService memberService;
    private final RefreshService refreshService;

    // 의존성 주입을 위한 매개변수 있는 생성자 직접 작성
//    public CustomLogoutHandler(JWTUtil jwtUtil, RefreshService refreshService, MemberService memberService) {
//        this.jwtUtil = jwtUtil;
//        this.memberService = memberService;
//        this.refreshService = refreshService;
//    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // request header 에서 'Authorization: Bearer <token 문자열>' 토큰 문자열 추출
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            // 'Bearer ' 다음부터 시작하는 실제 토큰 문자열 추출
            String token = authorization.substring("Bearer ".length());
            // 토큰 문자열에서 사용자 아이디 추출
            String memId = jwtUtil.getUserIdFromToken(token);
            // 사용자 아이디를 이용해서 사용자 정보 조회
            Member member = memberService.selectMember(memId);
            if(member != null) {
                // 해당 사용자의 Refresh-Token 을 db 에서 조회해 옴
                Optional<RefreshToken> refresh = refreshService.findByTokenValue(token);
                if(refresh.isPresent()) {
                    RefreshToken refreshToken = refresh.get();
                    // 해당 리프레시 토큰을 db 에서 삭제
                    refreshService.deleteByRefreshToken(refreshToken.getTokenValue());
                }
            }
        }

        // 클라이언트에게 로그아웃 성공 응답을 보냄
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
