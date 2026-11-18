package com.back.domain.post.post.entity;

// Post (글)
// 제목과 본문을 가진 게시물. 댓글(PostComment)을 0개 이상 가진다. (GLOSSARY.md)
// DB에서는 post 테이블의 한 행이다. id·작성일·수정일은 BaseEntity에서 물려받는다.

import com.back.domain.post.postComment.entity.PostComment;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
// JPA는 DB에서 읽은 행으로 객체를 만들 때 "인자 없는 생성자"를 쓴다. 그래서 반드시 필요하다.
@NoArgsConstructor
public class Post extends BaseEntity {

    private String title;

    // 본문은 최대 5000자라 기본 VARCHAR(255)로는 부족하다. TEXT 타입으로 만든다.
    @Column(columnDefinition = "TEXT")
    private String content;

    // ── JPA 연관관계: 글 1 : 댓글 N (일대다) ──
    // DB에서는 post_comment 테이블에 post_id(외래 키) 컬럼 하나로 관계를 표현한다.
    // 그런데 자바에서는 양쪽(Post.comments, PostComment.post)에서 서로를 볼 수 있다 = "양방향" 연관관계.
    //
    // 연관관계의 주인(owner): 외래 키(post_id)를 실제로 관리하는 쪽. 외래 키가 있는 PostComment.post가 주인이다.
    // mappedBy = "post": "나는 주인이 아니고, PostComment의 post 필드에 매핑된 거울일 뿐"이라는 뜻.
    //   그래서 comments 리스트에 무엇을 넣어도 DB의 post_id는 바뀌지 않는다. 주인(comment.post)을 설정해야 바뀐다.
    // 주의: 그래서 addComment()에서 리스트에 넣는 것과 comment.post 설정을 항상 함께 한다(생성자에서).
    //
    // cascade = REMOVE: 글을 삭제하면 그 글의 댓글도 함께 삭제한다.
    // orphanRemoval = true: comments 리스트에서 빠진 댓글(고아)은 DB에서도 삭제한다.
    //   댓글은 소속 글 없이 존재할 수 없다는 규칙(GLOSSARY.md)을 DB 수준에서 지키기 위함이다.
    // cascade = PERSIST: 트랜잭션 안에서 저장된 글의 comments에 새 댓글을 넣으면, 댓글도 함께 INSERT된다.
    //   그래서 댓글 전용 저장소(PostCommentRepository) 없이 post.addComment(...)만으로 댓글이 저장된다.
    // @OneToMany의 기본 fetch는 LAZY: 글을 읽을 때 댓글은 실제로 comments를 쓸 때 따로 조회한다.
    // 주의: LAZY 컬렉션은 트랜잭션(영속성 컨텍스트) 밖에서 읽으면 LazyInitializationException이 난다.
    @OneToMany(mappedBy = "post", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    public Post(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // 글 수정: 필드 값만 바꾼다. save()도 UPDATE SQL도 여기엔 없다.
    // 왜 setter(@Setter) 대신 modify()인가: "제목과 본문은 함께 바뀐다"는 규칙을 메서드 하나로 드러내고,
    //   아무 곳에서나 필드를 하나씩 바꾸지 못하게 하기 위해서다.
    // 실제 DB 반영은 PostService.modify()의 주석(변경 감지, dirty checking)을 보자.
    public void modify(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public PostComment addComment(String content) {
        PostComment comment = new PostComment(this, content);
        comments.add(comment);
        return comment;
    }
}
