package com.back.domain.post.postComment.dto;

// PostCommentDto
// 댓글(PostComment)을 API 응답으로 내보낼 때 쓰는 모양. (PostDto와 같은 이유로 엔티티 대신 record DTO를 쓴다)
// 요청 흐름: 서비스가 댓글 엔티티를 넘기면 → 컨트롤러가 이 DTO로 바꿔 → JSON 응답.
//
// 소속 글은 Post 객체가 아니라 postId 숫자 하나로만 내보낸다:
//   1. 순환 참조 방지: postComment.post → post.comments → postComment.post → … 를 아예 끊는다.
//   2. 클라이언트가 필요한 건 "어느 글의 댓글인가"뿐이다. 글 내용이 필요하면 /api/v1/posts/{postId}로 따로 조회한다.

import com.back.domain.post.postComment.entity.PostComment;

import java.time.LocalDateTime;

public record PostCommentDto(
        Long id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        Long postId,
        String content
) {
    public PostCommentDto(PostComment postComment) {
        this(
                postComment.getId(),
                postComment.getCreateDate(),
                postComment.getModifyDate(),
                // 주의: postComment.post는 LAZY라 실제 Post 대신 "프록시"(빈 껍데기 대리 객체)가 들어 있다.
                //   Hibernate 프록시는 id를 이미 알고 있어서, getId()만 부르면 글 SELECT 없이 바로 돌려준다.
                //   getTitle() 같은 다른 필드를 부르면 그때 SELECT가 나간다(트랜잭션 밖이면 LazyInitializationException).
                postComment.getPost().getId(),
                postComment.getContent()
        );
    }
}
