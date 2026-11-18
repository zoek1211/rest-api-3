package com.back.domain.post.post.dto;

// PostDto
// 글(Post)을 API 응답으로 내보낼 때 쓰는 모양. 요청 흐름: 서비스가 엔티티를 넘기면 → 컨트롤러가 "DTO"로 바꿔 → JSON 응답.
//
// DTO(Data Transfer Object): 계층 사이·시스템 사이에 데이터를 "옮기기만" 하는 객체. 로직이 없다.
//
// 왜 엔티티(Post)를 그대로 JSON으로 내보내지 않나:
//   1. 순환 참조: Post.comments → PostComment.post → Post.comments → … Jackson이 끝없이 따라가다
//      StackOverflowError(또는 무한 JSON)가 난다.
//   2. 내부 구조 노출: 엔티티는 DB 구조를 그대로 닮았다. 그대로 내보내면 필드 하나 추가·이름 변경만으로
//      API 응답이 바뀌어 클라이언트가 깨진다. DTO를 두면 "API 계약"과 "DB 구조"를 따로 바꿀 수 있다.
//   3. 지연 로딩(LAZY): comments를 직렬화하려고 건드리는 순간 트랜잭션 밖이면 LazyInitializationException이 난다.
//
// 왜 record인가: 생성자·getter(id() 형태)·equals·hashCode·toString을 자바가 자동으로 만들어 주고,
//   필드가 final이라 만들어진 뒤 바뀌지 않는다(불변). "옮기기만 하는" DTO에 딱 맞는다.
//   Jackson은 record의 컴포넌트 이름(id, createDate, …)을 그대로 JSON 키로 쓴다.

import com.back.domain.post.post.entity.Post;

import java.time.LocalDateTime;

public record PostDto(
        Long id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String title,
        String content
) {
    // 엔티티 → DTO 변환을 DTO 쪽에 둔다. 엔티티가 API 응답 모양을 몰라도 되게 하기 위해서다.
    public PostDto(Post post) {
        this(
                post.getId(),
                post.getCreateDate(),
                post.getModifyDate(),
                post.getTitle(),
                post.getContent()
        );
    }
}
