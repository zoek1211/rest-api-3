package com.back.domain.post.post.repository;

// PostRepository
// 글(Post)을 DB에 저장·조회·삭제하는 저장소. 요청 흐름: 서비스 → "저장소" → DB.
//
// 인터페이스만 선언하면 Spring Data JPA가 실행 시점에 구현체를 자동으로 만들어 Bean으로 등록한다.
// JpaRepository<엔티티 타입, ID 타입>을 상속하는 것만으로 save, findById, findAll, count, delete 등이 생긴다.

import com.back.domain.post.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
