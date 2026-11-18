package com.back.domain.post.postComment.entity;

// PostComment (댓글)
// 특정 글 하나에 속한 짧은 본문. 소속 글 없이는 존재할 수 없다. (GLOSSARY.md)
// DB에서는 post_comment 테이블의 한 행이고, post_id 외래 키(FK)로 소속 글을 가리킨다.

import com.back.domain.post.post.entity.Post;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class PostComment extends BaseEntity {

    // ── JPA 연관관계: 댓글 N : 글 1 (다대일) ──
    // 외래 키(post_id)를 가진 이쪽이 "연관관계의 주인"이다. 이 필드 값이 곧 DB의 post_id가 된다.
    // optional = false + nullable = false: 소속 글이 없는 댓글은 만들 수 없다 (NOT NULL 제약).
    // fetch = LAZY: @ManyToOne의 기본값은 EAGER(댓글을 읽을 때마다 글도 즉시 조회)라 불필요한 쿼리가 늘기 쉽다.
    //   필요할 때만 글을 조회하도록 LAZY로 바꾼다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Post post;

    private String content;

    // 댓글은 Post.addPostComment()를 통해 만든다. 그래야 양방향 관계(post.comments와 postComment.post)가 함께 맞춰진다.
    public PostComment(Post post, String content) {
        this.post = post;
        this.content = content;
    }

    // 댓글 본문 수정. setter 대신 "수정"이라는 의도가 드러나는 메서드를 둔다(Post.modify와 같은 이유).
    // 소속 글(post)은 바꿀 수 없다: 댓글을 다른 글로 옮기는 기능은 없다.
    // 주의: 값만 바꾸고 저장은 하지 않는다. 트랜잭션 안의 변경 감지가 UPDATE를 만든다(PostService.modifyPostComment 주석).
    public void modify(String content) {
        this.content = content;
    }
}
