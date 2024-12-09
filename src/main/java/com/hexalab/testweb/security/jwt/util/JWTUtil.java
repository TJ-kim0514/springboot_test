package com.hexalab.testweb.security.jwt.util;

import com.hexalab.testweb.member.model.dto.Member;
import com.hexalab.testweb.member.model.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.Date;

@Component  // 스프링 부트 컨테이너에 의해 관리되는 컴포넌트로 선언
//@PropertySource("classpath:application.properties")      // application.properties 가 연결되지 않을 때 사용하는 어노테이션
public class JWTUtil {
    // JWT 생성과 검증에 사용될 비밀키(secret key) 만료시간을 필드로 선언함
    private final SecretKey secretKey;
    private final MemberService memberService;
    // @Value 가 안먹힐 때 직접변수 작성해서 사용
//    private static final String secretKey = "${jwt.secret}";

    // 생성자를 통한 의존성 주입
    // application.properties 에 정의한 jwt 비밀키와 만료시간 값을 읽어와서 지정함
    public JWTUtil(@Value("${jwt.secret}") String secret_key, MemberService memberService) {   // application.properties 에 정의한 명칭을 그대로 가져와야 함
        // 비밀키 초기화함, 이 비밀키는 토큰의 서명(Signature)으로 사용됨
        this.secretKey = new SecretKeySpec(secret_key.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
        this.memberService = memberService;
    }

    // @Value 가 안먹힐 때의 생성자
//    public JWTUtil(MemberService memberService) {
//        this.secretKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
//        this.memberService = memberService;
//    }

    // JWT 토큰 생성 : 인증(authentication)시 받은 사용자 아이디를 전달받아서 확인하고 토큰 생성함
    // userId : 로그인 요청시 넘어온 회원 아이디 받음. category : 토큰의 종류(access token, refresh token)
    // expiredMs : 만료기한에 대한 밀리초
    public String generateToken(String memId, String category, Long expiredMs) {
        // Member Service 사용해서 db 에서 로그인한 사용자 정보를 조회해 옴
        Member member = memberService.selectMember(memId);

        // 사용자 정보가 없는 경우, UsernameNotFoundException (스프링 제공됨)을 발생시킴
        if (member == null) {
            throw new UsernameNotFoundException("userId : " + memId + "not found");
        }

        // 사용자의 관리자 여부 확인
        String adminYN = member.getMemType();

        // JWT 토큰 생성 : 사용자 아이디(subject)에 넣고, 관리자여부는 클레임으로 추가함 (임의대로 지정함)
        return Jwts.builder()
                .setSubject(memId) // 사용자 ID 설정 (로그인 시 이메일 사용시에는 이메일 등록)
                .claim("category", category)    // 카테고리 정보 추가 ("access", "refresh")
                .claim("name", member.getMemName())    // 사용자 이름 또는 닉네임 정보 추가
                .claim("role", (adminYN.equals("Y")? "ADMIN": "USER"))      // ROLE 정보 추가   (SecurityConfig 에서 지정한 명칭과 일치해야 함 - 대소문자 일치)
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))        // 토큰만료시간 설정        // import java.sql.Date
                .signWith(secretKey, SignatureAlgorithm.HS256)      // 비밀키와 알고리즘으로 서명
                .compact();     // JWT 생성 : JWT 를 압축 문자열로 만듦
    }

    // JWT 에서 사용자 아이디 추출하는 메소드
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    // JWT 의 만료 여부 확인용 메소드
    public Boolean isTokenExpired(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
        return claims.getExpiration().before(new java.util.Date());     // true : 유효기간 지남 - 만료, false : 유효기간 남음 - 만료 안됨
    }

    // JWT 에서 관리자 여부 추출하는 메소드
    public String getAdminFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
        return claims.get("role", String.class);       // get() 으로 꺼낼경우 기본 Object 형 이므로 형변환 필요
    }

    // JWT 에서 등록된 토큰 종류 추출하는 메소드
    public String getCategoryFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
        return claims.get("category", String.class);
    }
}
