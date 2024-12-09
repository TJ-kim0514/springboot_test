package com.hexalab.testweb.member.jpa.repository;

import com.hexalab.testweb.member.jpa.entity.MemberFilesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberFilesRepository extends JpaRepository<MemberFilesEntity, String>, MemberFilesRepositoryCustom {
}
