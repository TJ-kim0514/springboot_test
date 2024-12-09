package com.hexalab.testweb.security.config;

import com.hexalab.testweb.member.model.service.MemberService;
import com.hexalab.testweb.security.handler.CustomLogoutHandler;
import com.hexalab.testweb.security.jwt.filter.JWTFilter;
import com.hexalab.testweb.security.jwt.filter.LoginFilter;
import com.hexalab.testweb.security.jwt.model.service.RefreshService;
import com.hexalab.testweb.security.jwt.util.JWTUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final RefreshService refreshService;
    private final MemberService memberService;
    private final JWTUtil jwtUtil;
    private final CorsConfigurationSource corsConfigurationSource;

    // 직접 생성자를 작성해서 초기화 선언함 (@RequiredArgsConstructor 를 사용하지 않을 경우)
    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, RefreshService refreshService, MemberService memberService, JWTUtil jwtUtil, CorsConfigurationSource corsConfigurationSource) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.refreshService = refreshService;
        this.memberService = memberService;
        this.jwtUtil = jwtUtil;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    // 인증 (Authentication) 관리자를 스프링 부트 컨테이너에 Bean 으로 등록해야 함
    // 인증 과정에서 중요한 클래스임
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();  // 예외처리가 필요하므로 throws Exception 추가
    }


    // HTTP 관련 보안 설정을 정의함
    // SecurityFilterChain 을 Bean 으로 등록하고, http 서비스 요청에 대한 보안 설정을 구성함
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)      // import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
                .formLogin(AbstractHttpConfigurer::disable)     // 시큐리티가 제공하는 로그인 폼 사용 못하게 함
//                .httpBasic(AbstractHttpConfigurer::disable)     // form 태그로 submit 해서 오는 요청은 사용 못하게 함
                // 인증과 인가를 설정하는 부분
                .authorizeHttpRequests(auth -> auth
                        // 현재 프로젝트 안에 뷰 페이지를 작업할 때 설정하는 방식임 (리액트 작업시 제외)
                        .requestMatchers("/public/**", "/auth/**", "/css/**", "/js/**", "/error", "/**").permitAll() // 공개 경로 설정 및 인증 경로 허용
                        // JWT 사용시 추가되는 설정임
                        .requestMatchers(HttpMethod.POST, "/notice").hasRole("ADMIN")   // POST 요청은 ADMIN 롤 필요
                        .requestMatchers(HttpMethod.PUT, "/notice").hasRole("ADMIN")    // PUT 요청은 ADMIN 롤 필요
                        .requestMatchers(HttpMethod.DELETE, "/notice").hasRole("ADMIN") // DELETE 요청은 ADMIN 롤 필요
                        //게시글 서비스
                        .requestMatchers(HttpMethod.POST, "/board").hasRole("USER") // POST 요청은 USER 롤 필요
                        .requestMatchers(HttpMethod.PUT, "/board").hasRole("USER") // PUT 요청은 USER 롤 필요
                        .requestMatchers(HttpMethod.DELETE, "/board").hasRole("USER") // DELETE 요청은 USER 롤 필요
                        //회원 서비스
                        .requestMatchers(HttpMethod.GET, "/member/myinfo").hasRole("USER") // GET 요청은 USER 롤 필요
                        .requestMatchers(HttpMethod.PUT, "/member").hasRole("USER") // PUT 요청은 USER 롤 필요
                        .requestMatchers(HttpMethod.DELETE, "/member/{userId}").hasRole("USER") // DELETE 요청은 USER 롤 필요
                        //회원관리 서비스
                        .requestMatchers(HttpMethod.PUT, "/member/loginok/{userId}/{loginOk}").hasRole("ADMIN") // PUT 요청은 ADMIN 롤 필요
                        .requestMatchers(HttpMethod.GET, "/member/search").hasRole("ADMIN") // PUT 요청은 ADMIN 롤 필요
                        //검색과 조회 서비스들
                        .requestMatchers("/login",  "/notice/**", "/board/**", "/logout").permitAll()

                        // 위의 인가 설정을 제외한 나머지 요청들은 인증 거치도록 설정함
                        .anyRequest().authenticated()
                )
                // JWTFilter 와 LoginFilter 를 시큐리티 필터 체인에 추가 등록함
                .addFilterBefore(new JWTFilter(jwtUtil), LoginFilter.class)
                // UsernamePasswordAuthenticationFilter.class : LoginFilter 를 UsernamePasswordAuthenticationFilter 로 형변환 함
                .addFilterAt(new LoginFilter(memberService, refreshService, authenticationManager(authenticationConfiguration), jwtUtil), UsernamePasswordAuthenticationFilter.class)   // UsernamePasswordAuthenticationFilter : 스프링 부트에서 제공함
                // 로그아웃 처리는 커스터마이징함
                .logout(logout -> logout
                        .addLogoutHandler(new CustomLogoutHandler(jwtUtil, memberService, refreshService))
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                        }))
                // 세션 정책을 STATELESS 로 설정하고, 세션을 사용하지 않는 것을 명시함
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }   // securityFilterChain

    //시큐리티 작동시 SecurityChainFilter 들이 순서대로 자동 작동되는지 확인
    //디버그용 필터
    static class DebugFilter implements Filter {
        private final String filterName;

        public DebugFilter(String filterName) {
            this.filterName = filterName;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            log.info("[DEBUG] Entering Filter : " + filterName);
            //현재 SecurityContext 상태 출력
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(authentication != null) {
                log.info("[DEBUG] Authentication : " + authentication.getName()
                        + ", Authorization : " + authentication.getAuthorities());
            }else{
                log.info("[DEBUG] Authentication is null");
            }

            chain.doFilter(request, response);
            log.info("[DEBUG] Exiting Filter : " + filterName);
        }   // doFilter

    }   // DebugFilter
}
