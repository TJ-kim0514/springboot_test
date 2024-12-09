package com.hexalab.testweb.security.jwt.jpa.repository;

import com.hexalab.testweb.security.jwt.jpa.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshRepository extends JpaRepository<RefreshToken, UUID> {
    // tokenValue 를 전달받아 db에 조회해서 토큰 객체를 리턴하는 메소드
    Optional<RefreshToken> findByTokenValue(String tokenValue);       // import java.util.Optional;

    // 토큰값이 db에 존재하는지 확인하는 용도의 메소드
    Boolean existsByTokenValue(String refresh);

    // 로그아웃시 토큰 삭제용 메소드
    void deleteByTokenValue(String refresh);

    // 엑세스토큰에 등록된 회원아이디(userId)로 리프레시 토큰 조회용 메소드
    @Query("SELECT r FROM RefreshToken r WHERE r.userId = :userId")     // JPQL 방식 ( 테이블명 : 엔티티클래스명, 컬럼명 : 엔티티클래스 안의 멤버변수 명)
    List<RefreshToken> findByUserId(@Param("userId") String userId);
}
