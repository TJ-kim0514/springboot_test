package com.hexalab.testweb.member.model.dto;

import com.hexalab.testweb.member.jpa.entity.MemberFilesEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data       // getter, setter, ToString, Equals, HashCode 자동생성
@AllArgsConstructor     // 매개변수 있는 생성자 자동생성
@NoArgsConstructor      // 매개변수 없는 생성자 자동생성
@Builder        // 자동 build 어노테이션
public class MemberFiles {

    @NotBlank
    private UUID mfId;
    private String mfMemUUID;
    private String mfOriginalName;
    private String mfRename;


    public MemberFilesEntity toEntity() {
        return MemberFilesEntity.builder()
                .mfId(mfId)
                .mfMemUUID(mfMemUUID)
                .mfOriginalName(mfOriginalName)
                .mfRename(mfRename)
                .build();
    }
}
