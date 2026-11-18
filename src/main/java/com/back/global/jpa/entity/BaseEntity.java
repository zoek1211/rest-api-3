package com.back.global.jpa.entity;

// BaseEntity
// 모든 엔티티(Post, PostComment)가 공통으로 가지는 id, 작성일, 수정일을 한곳에 모은 부모 클래스.
// 요청 흐름에서 보면: 컨트롤러 → 서비스 → 저장소(Repository) → "엔티티" → DB 테이블.
// 엔티티(Entity)란 DB 테이블의 한 행(row)에 대응하는 자바 객체다. JPA가 둘 사이를 자동으로 변환해 준다(ORM).
//
// 왜 부모 클래스로 뺐나: 엔티티마다 id·날짜 필드와 Auditing 설정을 반복해서 쓰지 않기 위해서다.

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// @MappedSuperclass: 이 클래스 자체는 테이블이 되지 않고, 필드만 자식 엔티티의 테이블 컬럼으로 물려준다.
//   그래서 post 테이블과 post_comment 테이블에 각각 id, create_date, modify_date 컬럼이 생긴다.
@MappedSuperclass
// JPA Auditing: "누가/언제 만들고 고쳤는지"를 자동으로 기록하는 기능.
//   AuditingEntityListener가 저장(INSERT) 직전에 @CreatedDate·@LastModifiedDate를,
//   수정(UPDATE) 직전에 @LastModifiedDate를 현재 시각으로 채운다.
// 주의: 이 리스너만 붙이면 동작하지 않는다. BackApplication의 @EnableJpaAuditing이 함께 있어야 한다.
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    // @Id: 기본 키(PK). @GeneratedValue(IDENTITY): 값을 DB의 auto_increment에 맡긴다.
    //   그래서 저장하기 전엔 null이고, 저장(save)한 뒤에 1, 2, 3… 순서로 채워진다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime modifyDate;
}
