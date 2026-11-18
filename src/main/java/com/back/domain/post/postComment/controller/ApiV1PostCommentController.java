package com.back.domain.post.postComment.controller;

// ApiV1PostCommentController
// 댓글(PostComment) API의 입구. 요청 흐름: HTTP 요청 → DispatcherServlet → "컨트롤러" → PostService → 저장소 → DB.
//
// URL이 /api/v1/posts/{postId}/comments인 이유: 댓글은 글 없이 존재할 수 없는 "글의 하위 자원"이다.
//   REST에서는 이런 소속 관계를 경로의 중첩으로 표현한다("1번 글의 댓글들").
//   /api/v1/comments?postId=1 같은 평평한 URL도 가능하지만, 소속이 필수인 자원이라 중첩이 더 자연스럽다.
// 글 컨트롤러와 나눈 이유: 자원(글/댓글)마다 컨트롤러 하나. 댓글 엔드포인트 5개가 모두 이 파일에 모인다.

import com.back.domain.post.post.service.PostService;
import com.back.domain.post.postComment.dto.PostCommentDto;
import com.back.domain.post.postComment.dto.PostCommentWriteReqBody;
import com.back.domain.post.postComment.entity.PostComment;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
// 클래스 수준 경로에도 {postId} 같은 경로 변수를 쓸 수 있다. 각 메서드에서 @PathVariable로 받는다.
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class ApiV1PostCommentController {

    private final PostService postService;

    // GET /api/v1/posts/{postId}/comments: 댓글 다건 조회(최신순).
    // ADR 0001: 조회는 RsData로 감싸지 않고 DTO 배열 그대로. 댓글이 없으면 빈 배열 [] (200).
    // 없는 글이면 서비스가 NoSuchElementException → GlobalExceptionHandler가 404("404-1").
    @GetMapping
    public List<PostCommentDto> getItems(@PathVariable long postId) {
        return postService.findCommentsByPostIdOrderByIdDesc(postId)
                .stream()
                .map(PostCommentDto::new) // 엔티티 → DTO
                .toList();
    }

    // GET /api/v1/posts/{postId}/comments/{id}: 댓글 단건 조회.
    // 경로 변수 두 개를 모두 받는다. 클래스 수준의 {postId}와 메서드 수준의 {id}가 합쳐진 경로다.
    // 없는 글, 없는 댓글, 다른 글의 댓글은 모두 같은 404("404-1"). 클라이언트 입장에서는 셋 다 "이 주소에 댓글이 없다"이다.
    @GetMapping("/{id}")
    public PostCommentDto getItem(@PathVariable long postId, @PathVariable long id) {
        return new PostCommentDto(postService.findCommentById(postId, id));
    }

    // POST /api/v1/posts/{postId}/comments: 댓글 작성.
    // 글 작성(ApiV1PostController.write)과 같은 틀이다:
    //   - @RequestBody + @Valid: 본문 검증 실패는 메서드 실행 전에 400("400-1")으로 끝난다.
    //   - 없는 글은 서비스가 NoSuchElementException → 404("404-1").
    //   - 반환은 RsData(ADR 0001). 201 상태 코드는 ResponseAspect가 "201-1"을 보고 정한다.
    @PostMapping
    public RsData<PostCommentDto> write(
            @PathVariable long postId,
            @RequestBody @Valid PostCommentWriteReqBody reqBody
    ) {
        // 서비스가 flush()까지 해 두었으므로 여기서 comment.getId()는 이미 채워져 있다(PostService.writeComment 주석).
        PostComment comment = postService.writeComment(postId, reqBody.content());

        return new RsData<>(
                "201-1",
                "%d번 댓글이 작성되었습니다.".formatted(comment.getId()),
                new PostCommentDto(comment)
        );
    }
}
