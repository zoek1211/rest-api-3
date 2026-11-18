package com.back.domain.post.postComment.dto;

// PostCommentWriteReqBody
// "댓글 작성" 요청 본문(JSON)을 담는 요청 DTO. 요청 흐름: 클라이언트 JSON → (Jackson이 이 record로 변환)
//   → (@Valid가 아래 규칙 검사) → ApiV1PostCommentController.write(...) 인자.
//
// 글 작성 요청 DTO(PostWriteReqBody)와 같은 원리다. 클라이언트가 보낼 수 있는 값은 content 하나뿐이다.
//   어느 글의 댓글인지(postId)는 본문이 아니라 URL 경로(/posts/{postId}/comments)로 받는다.
//   주의: 본문에 postId를 또 받으면 경로의 postId와 다를 때 무엇을 믿을지 애매해진다. 소속은 경로 하나로만 정한다.

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 댓글 본문 규칙: 2–100자, 공백만은 안 된다. (글 본문 5000자보다 훨씬 짧다)
// 주의: @Size는 null을 통과시키므로 @NotBlank와 함께 쓴다. 빈 문자열 ""은 두 규칙을 모두 어겨 msg가 두 줄이 된다.
public record PostCommentWriteReqBody(
        @NotBlank
        @Size(min = 2, max = 100)
        String content
) {
}
