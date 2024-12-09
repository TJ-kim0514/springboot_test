package com.hexalab.testweb.member.jpa.repository;

import com.hexalab.testweb.member.jpa.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, String>, MemberRepositoryCustom {
}
